import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LudoTCPClient {

    public static void main(String argv[]) throws Exception {
        
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        
        BufferedReader leitorTeclado = new BufferedReader(new InputStreamReader(System.in));

        
        String enderecoServidor = "localhost"; 
        int portaServidor = 6897;
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conectando ao servidor Ludo em " + enderecoServidor + ":" + portaServidor + "...");
        
        Socket clienteSocket = new Socket(enderecoServidor, portaServidor);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conexão estabelecida com sucesso!");

        
        String enderecoLocal = clienteSocket.getLocalAddress().getHostAddress();
        int portaLocal = clienteSocket.getLocalPort();
        String enderecoRemoto = clienteSocket.getInetAddress().getHostAddress();
        int portaRemota = clienteSocket.getPort();
        
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Seu Endereço: " + enderecoLocal + ":" + portaLocal);
        System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Servidor: " + enderecoRemoto + ":" + portaRemota);

        
        DataOutputStream saidaParaServidor = new DataOutputStream(clienteSocket.getOutputStream());
        BufferedReader entradaDoServidor = new BufferedReader(new InputStreamReader(clienteSocket.getInputStream()));

        
        Thread threadReceptora = new Thread(() -> {
            try {
                String mensagem;
                while ((mensagem = entradaDoServidor.readLine()) != null) {
                    System.out.println("[" + dtf.format(LocalDateTime.now()) + "] " + mensagem);
                    
                    
                    if (mensagem.contains("Seu turno") || 
                        mensagem.contains("Digite 'girar'") || 
                        mensagem.contains("Digite seu nome") || 
                        mensagem.contains("Escolha uma peça") ||
                        mensagem.contains("Deseja jogar novamente")) {
                        System.out.print(">> ");
                    }
                }
            } catch (Exception e) {
                System.out.println("[" + dtf.format(LocalDateTime.now()) + "] Conexao encerrada.");
            }
        });
        threadReceptora.start();

        
        String entradaUsuario;
        while ((entradaUsuario = leitorTeclado.readLine()) != null) {
            saidaParaServidor.writeBytes(entradaUsuario + '\n');
        }

        clienteSocket.close();
        leitorTeclado.close();
    }
}