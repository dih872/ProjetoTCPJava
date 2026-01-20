import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LudoTCPClient {

    public static void main(String argv[]) throws Exception {
        
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        
        BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in));

        
        String serverAddress = "localhost"; 
        int serverPort = 6897;
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conectando ao servidor Ludo em " + serverAddress + ":" + serverPort + "...");
        
        Socket clientSocket = new Socket(serverAddress, serverPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conexão estabelecida com sucesso!");

        
        String localAddress = clientSocket.getLocalAddress().getHostAddress();
        int localPort = clientSocket.getLocalPort();
        String remoteAddress = clientSocket.getInetAddress().getHostAddress();
        int remotePort = clientSocket.getPort();
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Seu Endereço: " + localAddress + ":" + localPort);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor: " + remoteAddress + ":" + remotePort);

        
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        
        Thread receiverThread = new Thread(() -> {
            try {
                String message;
                while ((message = inFromServer.readLine()) != null) {
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + message);
                    
                    
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

        
        String userInput;
        while ((userInput = keyboardReader.readLine()) != null) {
            outToServer.writeBytes(userInput + '\n');
        }

        clientSocket.close();
        keyboardReader.close();
    }
}