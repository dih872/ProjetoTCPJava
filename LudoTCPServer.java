import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class LudoTCPServer {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int SERVER_PORT = 6897;
    private static final int NUM_PLAYERS = 3;
    private static final int PEÇAS = 4;
    private static final int TAMANHO_TABULEIRO = 25;

    private List<Socket> clients = new ArrayList<>();
    private List<String> playerNames = new ArrayList<>();
    private List<Boolean> playersWantReplay = new ArrayList<>();
    private List<Boolean> playersReady = new ArrayList<>();
    private int[][] playerPositions = new int[NUM_PLAYERS][PEÇAS];
    private int currentPlayer = 0;
    private Random random = new Random();

    public static void main(String argv[]) throws Exception {
        new LudoTCPServer().start();
    }

    public void start() throws Exception {
        
        ServerSocket welcomeSocket = new ServerSocket(SERVER_PORT);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Servidor Ludo TCP rodando na porta " + SERVER_PORT);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Aguardando " + NUM_PLAYERS + " jogadores...");

        while (clients.size() < NUM_PLAYERS) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Aguardando nova conexao de jogadores...");
            Socket connectionSocket = welcomeSocket.accept();

            String remoteAddress = connectionSocket.getInetAddress().getHostAddress();
            int remotePort = connectionSocket.getPort();
            String localAddress = connectionSocket.getLocalAddress().getHostAddress();
            int localPort = connectionSocket.getLocalPort();
            
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Cliente TCP conectado:");
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "  Endereco do Cliente: " + remoteAddress + ":" + remotePort);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "  Endereco do Servidor: " + localAddress + ":" + localPort);

            clients.add(connectionSocket);
            playersWantReplay.add(false);
            playersReady.add(false);

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            outToClient.writeBytes("Digite seu nome: " + '\n');
            String playerName = inFromClient.readLine();
            playerNames.add(playerName);

            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Jogador registrado: " + playerName + " (" + remoteAddress + ":" + remotePort + ")");
            
            outToClient.writeBytes("Conexao estabelecida com sucesso" + '\n');
            outToClient.writeBytes("Voce esta conectado como: " + playerName + '\n');

            int remainingPlayers = NUM_PLAYERS - clients.size();
            if (remainingPlayers > 0) {
                outToClient.writeBytes("Faltam " + remainingPlayers + " jogadores para comecar." + '\n');
            } else {
                outToClient.writeBytes("Todos os jogadores conectados O jogo comecara em instantes..." + '\n');
            }
        }

        // Antes de iniciar a primeira partida, aguardar confirmação de todos os jogadores
        boolean allConfirmed = waitForAllPlayersToStart();
        if (!allConfirmed) {
            broadcast("Nem todos os jogadores confirmaram o inicio. Encerrando servidor...");
            // Fechar conexões e sair
            for (Socket client : clients) {
                try { client.close(); } catch (Exception e) { }
            }
            welcomeSocket.close();
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor encerrado por falta de confirmacao dos jogadores.");
            return;
        }

        // Loop principal do servidor para múltiplas partidas
        while (true) {
            // Iniciar nova partida
            startNewGame();
            
            // Após o jogo terminar, perguntar se querem jogar novamente
            askForReplay();
            
            boolean allWantReplay = checkAllPlayersWantReplay();
            if (!allWantReplay) {
                broadcast("Alguns jogadores não querem jogar novamente. Encerrando...");
                break;
            } else {
                broadcast("Iniciando nova partida...");
            }
        }

        // Fechar conexões
        for (Socket client : clients) {
            try {
                client.close();
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Erro ao fechar conexão com cliente");
            }
        }
        
        welcomeSocket.close();
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Servidor encerrado.");
    }

    private void startNewGame() {
        // Resetar posições dos jogadores
        for (int i = 0; i < NUM_PLAYERS; i++) {
            Arrays.fill(playerPositions[i], -1);
        }
        currentPlayer = 0;
        
        broadcast("Jogo iniciado! Jogadores: " + String.join(", ", playerNames));
        broadcast(getBoardState());
        playGame();
    }

    private void playGame() {
        while (!isGameOver()) {
            String currentName = playerNames.get(currentPlayer);
            broadcast("Turno de " + currentName + ". Rolar dado...");
            sendToClient(currentPlayer, "Seu turno. Digite 'girar' para rolar o dado.");

            String command = receiveFromClient(currentPlayer);
            
            if ("girar".equalsIgnoreCase(command)) {
                int girar = random.nextInt(6) + 1;
                broadcast(currentName + " rolou " + girar + ".");
                sendToClient(currentPlayer, "Voce rolou " + girar + ". Escolha uma peca para mover (0-3) ou 'skip' se nao puder.");

                String moveCommand = receiveFromClient(currentPlayer);
                
                if (!"skip".equalsIgnoreCase(moveCommand)) {
                    try {
                        int pieceIndex = Integer.parseInt(moveCommand);
                        if (pieceIndex >= 0 && pieceIndex < PEÇAS) {
                            movePiece(currentPlayer, pieceIndex, girar);
                        } else {
                            sendToClient(currentPlayer, "Peca invalida." + '\n');
                        }
                    } catch (NumberFormatException e) {
                        sendToClient(currentPlayer, "Comando invalido." + '\n');
                    }
                }

                if (girar != 6) {
                    currentPlayer = (currentPlayer + 1) % NUM_PLAYERS;
                }
            } else {
                sendToClient(currentPlayer, "Comando invalido. Digite 'girar'." + '\n');
            }

            broadcast(getBoardState());
        }

        int winner = getWinner();
        broadcast("=== JOGO TERMINADO! ===");
        broadcast("VENCEDOR: " + playerNames.get(winner));
    }

    private void askForReplay() {
        // Resetar flags de replay ANTES de perguntar
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playersWantReplay.set(i, false);
        }
        
        broadcast("Deseja jogar novamente? Digite 'sim' para continuar ou 'nao' para sair:");
        
        // Coletar respostas de todos os jogadores
        for (int i = 0; i < NUM_PLAYERS; i++) {
            sendToClient(i, "Deseja jogar novamente? (sim/nao):");
            String response = receiveFromClient(i);
            
            if ("sim".equalsIgnoreCase(response) || "s".equalsIgnoreCase(response)) {
                playersWantReplay.set(i, true);
            } else {
                playersWantReplay.set(i, false);
            }
        }
    }

    private boolean checkAllPlayersWantReplay() {
        for (boolean wantsReplay : playersWantReplay) {
            if (!wantsReplay) {
                return false;
            }
        }
        return true;
    }

    private void resetGame() {
        // Resetar todas as posições
        for (int i = 0; i < NUM_PLAYERS; i++) {
            Arrays.fill(playerPositions[i], -1);
        }
        currentPlayer = 0;
    }

    private void movePiece(int player, int piece, int steps) {
        int currentPos = playerPositions[player][piece];
        
        if (currentPos == -1) {  // Peça na base
            if (steps == 5) {
                playerPositions[player][piece] = 0;  // Sai para posição 0
                broadcast(playerNames.get(player) + " tirou uma peca da base");
            } else {
                sendToClient(player, "Precisa de 5 para sair da base." + '\n');
                return;
            }
        } 
        else if (currentPos == TAMANHO_TABULEIRO - 1) {  // Já está no centro
            sendToClient(player, "Esta peca ja chegou ao centro e não pode mais ser movida." + '\n');
            return;
        }
        else {  // Peça no tabuleiro
            int newPos = currentPos + steps;
            
            // Regra especial para chegar ao centro - precisa ser exato
            if (newPos > TAMANHO_TABULEIRO - 1) {
                // Calcular quantos passos faltam para chegar exatamente ao centro
                int stepsToCenter = (TAMANHO_TABULEIRO - 1) - currentPos;
                
                broadcast(playerNames.get(player) + " precisa de " + stepsToCenter + 
                         " para chegar exatamente ao centro! (rolou " + steps + ") " +
                         "A peca permanece na posicao " + currentPos + ".");
                
                sendToClient(player, "Para chegar ao centro precisa do numero exato " + 
                            stepsToCenter + ". Sua peca permanece na posicao " + currentPos + ".");
                return;  // Não move a peça
            }
            
            // Se chegou exatamente no centro
            if (newPos == TAMANHO_TABULEIRO - 1) {
                playerPositions[player][piece] = newPos;
                broadcast(playerNames.get(player) + " levou uma peca ao centro!");
                
                // Verificar se ganhou
                if (isPlayerWinner(player)) {
                    broadcast(playerNames.get(player) + " COMPLETOU TODAS AS PECAS!");
                }
                return;
            }
            
            // Verificar captura (apenas se não estiver no centro)
            for (int p = 0; p < NUM_PLAYERS; p++) {
                if (p != player) {
                    for (int pc = 0; pc < PEÇAS; pc++) {
                        if (playerPositions[p][pc] == newPos) {
                            playerPositions[p][pc] = -1;  // Volta à base
                            broadcast("Peca de " + playerNames.get(p) + " foi CAPTURADA por " + playerNames.get(player));
                        }
                    }
                }
            }
            
            playerPositions[player][piece] = newPos;
            
            // Informar quantos passos faltam para o centro
            int remainingToCenter = (TAMANHO_TABULEIRO - 1) - newPos;
            if (remainingToCenter > 0) {
                sendToClient(player, "Voce moveu para a posicao " + newPos + 
                            ". Faltam " + remainingToCenter + " passos para chegar ao centro.");
            }
        }
    }
    
    private boolean isPlayerWinner(int player) {
        int count = 0;
        for (int pos : playerPositions[player]) {
            if (pos == TAMANHO_TABULEIRO - 1) count++;
        }
        return count == PEÇAS;
    }

    private void broadcast(String message) {
        for (int i = 0; i < clients.size(); i++) {
            try {
                DataOutputStream outToClient = new DataOutputStream(clients.get(i).getOutputStream());
                outToClient.writeBytes(message + '\n');
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Erro ao enviar para jogador " + playerNames.get(i));
            }
        }
    }

    private void sendToClient(int playerIndex, String message) {
        try {
            DataOutputStream outToClient = new DataOutputStream(clients.get(playerIndex).getOutputStream());
            outToClient.writeBytes(message + '\n');
        } catch (Exception e) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Erro ao enviar para jogador " + playerNames.get(playerIndex));
        }
    }

    private String receiveFromClient(int playerIndex) {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clients.get(playerIndex).getInputStream()));
            return inFromClient.readLine();
        } catch (Exception e) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Erro ao receber do jogador " + playerNames.get(playerIndex));
        }
        return "";
    }

    private String getBoardState() {
        StringBuilder sb = new StringBuilder("\n=== ESTADO DO TABULEIRO ===\n");
        for (int p = 0; p < NUM_PLAYERS; p++) {
            sb.append(playerNames.get(p)).append(": ");
            for (int i = 0; i < PEÇAS; i++) {
                int pos = playerPositions[p][i];
                if (pos == -1) {
                    sb.append("[BASE]");
                } else if (pos == TAMANHO_TABULEIRO - 1) {
                    sb.append("[CENTRO]");
                } else {
                    sb.append("[").append(pos).append("]");
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        sb.append("==========================\n");
        return sb.toString();
    }

    private boolean isGameOver() {
        for (int p = 0; p < NUM_PLAYERS; p++) {
            int count = 0;
            for (int pos : playerPositions[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) count++;
            }
            if (count == PEÇAS) return true;
        }
        return false;
    }

    private int getWinner() {
        for (int p = 0; p < NUM_PLAYERS; p++) {
            int count = 0;
            for (int pos : playerPositions[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) count++;
            }
            if (count == PEÇAS) return p;
        }
        return -1;
    }

    private boolean waitForAllPlayersToStart() {
        // Resetar flags
        for (int i = 0; i < NUM_PLAYERS; i++) {
            playersReady.set(i, false);
        }

        broadcast("Todos os jogadores devem confirmar o inicio da partida.");
        // Enviar pedido de confirmacao a cada cliente
        for (int i = 0; i < NUM_PLAYERS; i++) {
            sendToClient(i, "Deseja iniciar a partida? Digite 'sim' para confirmar ou 'nao' para sair:");
        }

        // Receber respostas
        for (int i = 0; i < NUM_PLAYERS; i++) {
            String response = receiveFromClient(i);
            if (response == null) response = "";
            if ("sim".equalsIgnoreCase(response) || "s".equalsIgnoreCase(response)) {
                playersReady.set(i, true);
                sendToClient(i, "Confirmacao recebida. Aguardando os outros jogadores...");
            } else {
                playersReady.set(i, false);
                sendToClient(i, "Voce optou por nao iniciar. Conexao sera encerrada.");
            }
        }

        // Verificar se todos confirmaram
        for (boolean ready : playersReady) {
            if (!ready) return false;
        }
        broadcast("Todos confirmaram. Iniciando a partida...");
        return true;
    }
}