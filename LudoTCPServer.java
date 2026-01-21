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
    private static final int PORTA_SERVIDOR = 6897;
    private static final int NUMERO_DE_JOGADORES = 3;
    private static final int NUMERO_PECAS = 4;
    private static final int TAMANHO_TABULEIRO = 25;

    private List<Socket> clientes = new ArrayList<>();
    private List<String> nomesJogadores = new ArrayList<>();
    private List<Boolean> jogadoresQueremReiniciar = new ArrayList<>();
    private List<Boolean> jogadoresProntos = new ArrayList<>();
    private int[][] posicaoJogador = new int[NUMERO_DE_JOGADORES][NUMERO_PECAS];
    private int jogadorAtual = 0;
    private Random aleatorio = new Random();

    public static void main(String argv[]) throws Exception {
        new LudoTCPServer().iniciar();
    }

    public void iniciar() throws Exception {
        
        ServerSocket socketBemVindo = new ServerSocket(PORTA_SERVIDOR);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Servidor Ludo TCP rodando na porta " + PORTA_SERVIDOR);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Aguardando " + NUMERO_DE_JOGADORES + " jogadores...");

        while (clientes.size() < NUMERO_DE_JOGADORES) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Aguardando nova conexao de jogadores...");
            Socket socketConexao = socketBemVindo.accept();

            String enderecoRemoto = socketConexao.getInetAddress().getHostAddress();
            int portaRemota = socketConexao.getPort();
            String enderecoLocal = socketConexao.getLocalAddress().getHostAddress();
            int portaLocal = socketConexao.getLocalPort();
            
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Cliente TCP conectado:");
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "  Endereco do Cliente: " + enderecoRemoto + ":" + portaRemota);
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "  Endereco do Servidor: " + enderecoLocal + ":" + portaLocal);

            clientes.add(socketConexao);
            jogadoresQueremReiniciar.add(false);
            jogadoresProntos.add(false);

            BufferedReader entradaDoCliente = new BufferedReader(new InputStreamReader(socketConexao.getInputStream()));
            DataOutputStream saidaParaCliente = new DataOutputStream(socketConexao.getOutputStream());

            saidaParaCliente.writeBytes("Digite seu nome: " + '\n');
            String nomeJogador = entradaDoCliente.readLine();
            nomesJogadores.add(nomeJogador);

            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Jogador registrado: " + nomeJogador + " (" + enderecoRemoto + ":" + portaRemota + ")");
            
            saidaParaCliente.writeBytes("Conexao estabelecida com sucesso" + '\n');
            saidaParaCliente.writeBytes("Voce esta conectado como: " + nomeJogador + '\n');

            int jogadoresRestantes = NUMERO_DE_JOGADORES - clientes.size();
            if (jogadoresRestantes > 0) {
                saidaParaCliente.writeBytes("Faltam " + jogadoresRestantes + " jogadores para comecar." + '\n');
            } else {
                saidaParaCliente.writeBytes("Todos os jogadores conectados O jogo comecara em instantes..." + '\n');
            }
        }

        
        boolean todosConfirmaram = aguardarTodosJogadoresIniciar();
        if (!todosConfirmaram) {
            transmitirParaTodos("Nem todos os jogadores confirmaram o inicio. Encerrando servidor...");
            
            for (Socket cliente : clientes) {
                try { cliente.close(); } catch (Exception e) { }
            }
            socketBemVindo.close();
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor encerrado por falta de confirmacao dos jogadores.");
            return;
        }

        
        while (true) {
            
            iniciarNovoJogo();
            
            
            perguntarReiniciar();
            
            boolean todosQueremReiniciar = verificarTodosJogadoresQueremReiniciar();
            if (!todosQueremReiniciar) {
                transmitirParaTodos("Alguns jogadores não querem jogar novamente. Encerrando...");
                break;
            } else {
                transmitirParaTodos("Iniciando nova partida...");
            }
        }

        
        for (Socket cliente : clientes) {
            try {
                cliente.close();
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Erro ao fechar conexão com cliente");
            }
        }
        
        socketBemVindo.close();
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                + "Servidor encerrado.");
    }

    private void iniciarNovoJogo() {
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            Arrays.fill(posicaoJogador[i], -1);
        }
        jogadorAtual = 0;
        
        transmitirParaTodos("Jogo iniciado! Jogadores: " + String.join(", ", nomesJogadores));
        transmitirParaTodos(obterEstadoTabuleiro());
        jogar();
    }

    private void jogar() {
        while (!jogoTerminou()) {
            String nomeAtual = nomesJogadores.get(jogadorAtual);
            transmitirParaTodos("Turno de " + nomeAtual + ". Rolar dado...");
            enviarParaCliente(jogadorAtual, "Seu turno. Digite 'girar' para rolar o dado.");

            String comando = receberDoCliente(jogadorAtual);
            
            if ("girar".equalsIgnoreCase(comando)) {
                int girar = aleatorio.nextInt(6) + 1;
                transmitirParaTodos(nomeAtual + " rolou " + girar + ".");
                enviarParaCliente(jogadorAtual, "Voce rolou " + girar + ". Escolha uma peca para mover (0-3) ou 'skip' se nao puder.");

                String comandoMover = receberDoCliente(jogadorAtual);
                
                if (!"skip".equalsIgnoreCase(comandoMover)) {
                    try {
                        int indicePeca = Integer.parseInt(comandoMover);
                        if (indicePeca >= 0 && indicePeca < NUMERO_PECAS) {
                            moverPeca(jogadorAtual, indicePeca, girar);
                        } else {
                            enviarParaCliente(jogadorAtual, "Peca invalida." + '\n');
                        }
                    } catch (NumberFormatException e) {
                        enviarParaCliente(jogadorAtual, "Comando invalido." + '\n');
                    }
                }

                if (girar != 6) {
                    jogadorAtual = (jogadorAtual + 1) % NUMERO_DE_JOGADORES;
                }
            } else {
                enviarParaCliente(jogadorAtual, "Comando invalido. Digite 'girar'." + '\n');
            }

            transmitirParaTodos(obterEstadoTabuleiro());
        }

        int vencedor = obterVencedor();
        transmitirParaTodos("=== JOGO TERMINADO! ===");
        transmitirParaTodos("VENCEDOR: " + nomesJogadores.get(vencedor));
    }

    private void perguntarReiniciar() {
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            jogadoresQueremReiniciar.set(i, false);
        }
        
        transmitirParaTodos("Deseja jogar novamente? Digite 'sim' para continuar ou 'nao' para sair:");
        
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            enviarParaCliente(i, "Deseja jogar novamente? (sim/nao):");
            String resposta = receberDoCliente(i);
            
            if ("sim".equalsIgnoreCase(resposta) || "s".equalsIgnoreCase(resposta)) {
                jogadoresQueremReiniciar.set(i, true);
            } else {
                jogadoresQueremReiniciar.set(i, false);
            }
        }
    }

    private boolean verificarTodosJogadoresQueremReiniciar() {
        for (boolean querReiniciar : jogadoresQueremReiniciar) {
            if (!querReiniciar) {
                return false;
            }
        }
        return true;
    }

    private void reiniciarJogo() {
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            Arrays.fill(posicaoJogador[i], -1);
        }
        jogadorAtual = 0;
    }

    private void moverPeca(int jogador, int peca, int passos) {
        int posicaoAtual = posicaoJogador[jogador][peca];
        
        if (posicaoAtual == -1) {  
            if (passos == 5) {
                posicaoJogador[jogador][peca] = 0;  
                transmitirParaTodos(nomesJogadores.get(jogador) + " tirou uma peca da base");
            } else {
                enviarParaCliente(jogador, "Precisa de 5 para sair da base." + '\n');
                return;
            }
        } 
        else if (posicaoAtual == TAMANHO_TABULEIRO - 1) {  
            enviarParaCliente(jogador, "Esta peca ja chegou ao centro e não pode mais ser movida." + '\n');
            return;
        }
        else {  
            int novaPosicao = posicaoAtual + passos;
            
            
            if (novaPosicao > TAMANHO_TABULEIRO - 1) {
                
                int passosParaCentro = (TAMANHO_TABULEIRO - 1) - posicaoAtual;
                
                transmitirParaTodos(nomesJogadores.get(jogador) + " precisa de " + passosParaCentro + 
                         " para chegar exatamente ao centro! (rolou " + passos + ") " +
                         "A peca permanece na posicao " + posicaoAtual + ".");
                
                enviarParaCliente(jogador, "Para chegar ao centro precisa do numero exato " + 
                            passosParaCentro + ". Sua peca permanece na posicao " + posicaoAtual + ".");
                return;  
            }
            
            
            if (novaPosicao == TAMANHO_TABULEIRO - 1) {
                posicaoJogador[jogador][peca] = novaPosicao;
                transmitirParaTodos(nomesJogadores.get(jogador) + " levou uma peca ao centro!");
                
                
                if (jogadorVenceu(jogador)) {
                    transmitirParaTodos(nomesJogadores.get(jogador) + " COMPLETOU TODAS AS PECAS!");
                }
                return;
            }
            
            
            for (int p = 0; p < NUMERO_DE_JOGADORES; p++) {
                if (p != jogador) {
                    for (int pc = 0; pc < NUMERO_PECAS; pc++) {
                        if (posicaoJogador[p][pc] == novaPosicao) {
                            posicaoJogador[p][pc] = -1;  
                            transmitirParaTodos("Peca de " + nomesJogadores.get(p) + " foi CAPTURADA por " + nomesJogadores.get(jogador));
                        }
                    }
                }
            }
            
            posicaoJogador[jogador][peca] = novaPosicao;
            
            
            int restanteParaCentro = (TAMANHO_TABULEIRO - 1) - novaPosicao;
            if (restanteParaCentro > 0) {
                enviarParaCliente(jogador, "Voce moveu para a posicao " + novaPosicao + 
                            ". Faltam " + restanteParaCentro + " passos para chegar ao centro.");
            }
        }
    }
    
    private boolean jogadorVenceu(int jogador) {
        int contador = 0;
        for (int pos : posicaoJogador[jogador]) {
            if (pos == TAMANHO_TABULEIRO - 1) contador++;
        }
        return contador == NUMERO_PECAS;
    }

    private void transmitirParaTodos(String mensagem) {
        for (int i = 0; i < clientes.size(); i++) {
            try {
                DataOutputStream saidaParaCliente = new DataOutputStream(clientes.get(i).getOutputStream());
                saidaParaCliente.writeBytes(mensagem + '\n');
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                        + "Erro ao enviar para jogador " + nomesJogadores.get(i));
            }
        }
    }

    private void enviarParaCliente(int indiceJogador, String mensagem) {
        try {
            DataOutputStream saidaParaCliente = new DataOutputStream(clientes.get(indiceJogador).getOutputStream());
            saidaParaCliente.writeBytes(mensagem + '\n');
        } catch (Exception e) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Erro ao enviar para jogador " + nomesJogadores.get(indiceJogador));
        }
    }

    private String receberDoCliente(int indiceJogador) {
        try {
            BufferedReader entradaDoCliente = new BufferedReader(new InputStreamReader(clientes.get(indiceJogador).getInputStream()));
            return entradaDoCliente.readLine();
        } catch (Exception e) {
            System.out.println("[" + dtf.format(LocalDateTime.now()) + "] "
                    + "Erro ao receber do jogador " + nomesJogadores.get(indiceJogador));
        }
        return "";
    }

    private String obterEstadoTabuleiro() {
        StringBuilder construtor = new StringBuilder("\n=== ESTADO DO TABULEIRO ===\n");
        for (int p = 0; p < NUMERO_DE_JOGADORES; p++) {
            construtor.append(nomesJogadores.get(p)).append(": ");
            for (int i = 0; i < NUMERO_PECAS; i++) {
                int pos = posicaoJogador[p][i];
                if (pos == -1) {
                    construtor.append("[BASE]");
                } else if (pos == TAMANHO_TABULEIRO - 1) {
                    construtor.append("[CENTRO]");
                } else {
                    construtor.append("[").append(pos).append("]");
                }
                construtor.append(" ");
            }
            construtor.append("\n");
        }
        construtor.append("==========================\n");
        return construtor.toString();
    }

    private boolean jogoTerminou() {
        for (int p = 0; p < NUMERO_DE_JOGADORES; p++) {
            int contador = 0;
            for (int pos : posicaoJogador[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) contador++;
            }
            if (contador == NUMERO_PECAS) return true;
        }
        return false;
    }

    private int obterVencedor() {
        for (int p = 0; p < NUMERO_DE_JOGADORES; p++) {
            int contador = 0;
            for (int pos : posicaoJogador[p]) {
                if (pos == TAMANHO_TABULEIRO - 1) contador++;
            }
            if (contador == NUMERO_PECAS) return p;
        }
        return -1;
    }

    private boolean aguardarTodosJogadoresIniciar() {
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            jogadoresProntos.set(i, false);
        }

        transmitirParaTodos("Todos os jogadores devem confirmar o inicio da partida.");
        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            enviarParaCliente(i, "Deseja iniciar a partida? Digite 'sim' para confirmar ou 'nao' para sair:");
        }

        
        for (int i = 0; i < NUMERO_DE_JOGADORES; i++) {
            String resposta = receberDoCliente(i);
           if (resposta == null) resposta = "";
            if ("sim".equalsIgnoreCase(resposta) || "s".equalsIgnoreCase(resposta)) {
                jogadoresProntos.set(i, true);
                enviarParaCliente(i, "Confirmacao recebida. Aguardando os outros jogadores...");
            } else {
                jogadoresProntos.set(i, false);
                enviarParaCliente(i, "Voce optou por nao iniciar. Conexao sera encerrada.");
            }
        }

        
        for (boolean pronto : jogadoresProntos) {
            if (!pronto) return false;
        }
        transmitirParaTodos("Todos confirmaram. Iniciando a partida...");
        return true;
    }
}