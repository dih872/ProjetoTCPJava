import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LudoTCPClient {

    public static void main(String argv[]) throws Exception {
        
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Criar leitor do teclado
        BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in));

        // Conectar ao servidor (usando localhost por padrão)
        String serverAddress = "localhost"; // faz conexão automatica  com o  localhost
        int serverPort = 6897;
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conectando ao servidor Ludo em " + serverAddress + ":" + serverPort + "...");
        
        Socket clientSocket = new Socket(serverAddress, serverPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conexão estabelecida com sucesso!");

        // Obter informações da conexão
        String localAddress = clientSocket.getLocalAddress().getHostAddress();
        int localPort = clientSocket.getLocalPort();
        String remoteAddress = clientSocket.getInetAddress().getHostAddress();
        int remotePort = clientSocket.getPort();
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Seu Endereço: " + localAddress + ":" + localPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor: " + remoteAddress + ":" + remotePort);

        // Criar objetos para enviar e receber dados
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Thread para receber mensagens do servidor
        Thread receiverThread = new Thread(() -> {
            try {
                String message;
                while ((message = inFromServer.readLine()) != null) {
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + message);
                    
                    // Se for nossa vez ou precisar de resposta, mostrar prompt especial
                    if (message.contains("Seu turno") || 
                        message.contains("Digite 'roll'") || 
                        message.contains("Digite seu nome") || 
                        message.contains("Escolha uma peça") ||
                        message.contains("Deseja jogar novamente")) {
                        System.out.print(">> ");
                    }
                }
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conexao encerrada.");
            }
        });
        receiverThread.start();

        // Loop principal para enviar comandos
        String userInput;
        while ((userInput = keyboardReader.readLine()) != null) {
            outToServer.writeBytes(userInput + '\n');
        }

        // Fechar conexão
        clientSocket.close();
        keyboardReader.close();
    }
}