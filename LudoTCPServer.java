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
    private static final int NumeroDeJogadores = 3;
    private static final int PEÇAS = 4;
    private static final int TAMANHO_TABULEIRO = 25;

    private List<Socket> clients = new ArrayList<>();
    private List<String> playerNames = new ArrayList<>();
    private List<Boolean> playersWantReplay = new ArrayList<>();
    private List<Boolean> playersReady = new ArrayList<>();
    private int[][] posicaoJogador = new int[NumeroDeJogadores][PEÇAS];
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
                + "Aguardando " + NumeroDeJogadores + " jogadores...");

        while (clients.size() < NumeroDeJogadores) {
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

            int remainingPlayers = NumeroDeJogadores - clients.size();
            if (remainingPlayers > 0) {
                outToClient.writeBytes("Faltam " + remainingPlayers + " jogadores para comecar." + '\n');
            } else {
                outToClient.writeBytes("Todos os jogadores conectados O jogo comecara em instantes..." + '\n');
            }
        }

        
        boolean allConfirmed = waitForAllPlayersToStart();
        if (!allConfirmed) {
            broadcast("Nem todos os jogadores confirmaram o inicio. Encerrando servidor...");
            
            for (Socket client : clients) {
                try { client.close(); } catch (Exception e) { }
            }
            welcomeSocket.close();
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor encerrado por falta de confirmacao dos jogadores.");
            return;
        }

        
        while (true) {
            
            startNewGame();
            
            
            askForReplay();
            
            boolean allWantReplay = checkAllPlayersWantReplay();
            if (!allWantReplay) {
                broadcast("Alguns jogadores não querem jogar novamente. Encerrando...");
                break;
            } else {
                broadcast("Iniciando nova partida...");
            }
        }

        
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
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
            Arrays.fill(posicaoJogador[i], -1);
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
                    currentPlayer = (currentPlayer + 1) % NumeroDeJogadores;
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
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
            playersWantReplay.set(i, false);
        }
        
        broadcast("Deseja jogar novamente? Digite 'sim' para continuar ou 'nao' para sair:");
        
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
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
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
            Arrays.fill(posicaoJogador[i], -1);
        }
        currentPlayer = 0;
    }

    private void movePiece(int player, int piece, int steps) {
        int currentPos = posicaoJogador[player][piece];
        
        if (currentPos == -1) {  
            if (steps == 5) {
                posicaoJogador[player][piece] = 0;  
                broadcast(playerNames.get(player) + " tirou uma peca da base");
            } else {
                sendToClient(player, "Precisa de 5 para sair da base." + '\n');
                return;
            }
        } 
        else if (currentPos == TAMANHO_TABULEIRO - 1) {  
            sendToClient(player, "Esta peca ja chegou ao centro e não pode mais ser movida." + '\n');
            return;
        }
        else {  
            int newPos = currentPos + steps;
            
            
            if (newPos > TAMANHO_TABULEIRO - 1) {
                
                int stepsToCenter = (TAMANHO_TABULEIRO - 1) - currentPos;
                
                broadcast(playerNames.get(player) + " precisa de " + stepsToCenter + 
                         " para chegar exatamente ao centro! (rolou " + steps + ") " +
                         "A peca permanece na posicao " + currentPos + ".");
                
                sendToClient(player, "Para chegar ao centro precisa do numero exato " + 
                            stepsToCenter + ". Sua peca permanece na posicao " + currentPos + ".");
                return;  
            }
            
            
            if (newPos == TAMANHO_TABULEIRO - 1) {
                posicaoJogador[player][piece] = newPos;
                broadcast(playerNames.get(player) + " levou uma peca ao centro!");
                
                
                if (isPlayerWinner(player)) {
                    broadcast(playerNames.get(player) + " COMPLETOU TODAS AS PECAS!");
                }
                return;
            }
            
            
            for (int p = 0; p < NumeroDeJogadores; p++) {
                if (p != player) {
                    for (int pc = 0; pc < PEÇAS; pc++) {
                        if (posicaoJogador[p][pc] == newPos) {
                            posicaoJogador[p][pc] = -1;  
                            broadcast("Peca de " + playerNames.get(p) + " foi CAPTURADA por " + playerNames.get(player));
                        }
                    }
                }
            }
            
            posicaoJogador[player][piece] = newPos;
            
            
            int remainingToCenter = (TAMANHO_TABULEIRO - 1) - newPos;
            if (remainingToCenter > 0) {
                sendToClient(player, "Voce moveu para a posicao " + newPos + 
                            ". Faltam " + remainingToCenter + " passos para chegar ao centro.");
            }
        }
    }
    
    private boolean isPlayerWinner(int player) {
        int count = 0;
        for (int pos : posicaoJogador[player]) {
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
        for (int p = 0; p < NumeroDeJogadores; p++) {
            sb.append(playerNames.get(p)).append(": ");
            for (int i = 0; i < PEÇAS; i++) {
                int pos = posicaoJogador[p][i];
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
        for (int p = 0; p < NumeroDeJogadores; p++) {
            int count = 0;
            for (int pos : posicaoJogador[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) count++;
            }
            if (count == PEÇAS) return true;
        }
        return false;
    }

    private int getWinner() {
        for (int p = 0; p < NumeroDeJogadores; p++) {
            int count = 0;
            for (int pos : posicaoJogador[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) count++;
            }
            if (count == PEÇAS) return p;
        }
        return -1;
    }

    private boolean waitForAllPlayersToStart() {
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
            playersReady.set(i, false);
        }

        broadcast("Todos os jogadores devem confirmar o inicio da partida.");
        
        for (int i = 0; i < NumeroDeJogadores; i++) {
            sendToClient(i, "Deseja iniciar a partida? Digite 'sim' para confirmar ou 'nao' para sair:");
        }

        
        for (int i = 0; i < NumeroDeJogadores; i++) {
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

        
        for (boolean ready : playersReady) {
            if (!ready) return false;
        }
        broadcast("Todos confirmaram. Iniciando a partida...");
        return true;
    }
}