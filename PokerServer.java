// 1. Server aggiornato con crittografia e timer
import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.*;
import java.util.*;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.*;

public class PokerServer {
    private static final int PORT = 12345;
    private static final int TURN_TIMEOUT = 30;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static GameEngine gameEngine = new GameEngine();
    private static RSACryptography rsa = new RSACryptography(2048);

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server avviato...");
            
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientThread = new ClientHandler(socket, gameEngine, rsa);
                clients.add(clientThread);
                executor.execute(clientThread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcastMessage(String message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude) {
                client.sendMessage(message);
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private GameEngine gameEngine;
        private RSACryptography rsa;
        public String playerName;
        private ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor();

        public ClientHandler(Socket socket, GameEngine gameEngine, RSACryptography rsa) {
            this.socket = socket;
            this.gameEngine = gameEngine;
            this.rsa = rsa;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Invia chiave pubblica al client
                out.println(rsa.getE().toString());
                out.println(rsa.getN().toString());

                String encryptedName = in.readLine();
                playerName = RSACryptography.decriptS(rsa, encryptedName);
                gameEngine.addPlayer(playerName, this);
                
                broadcastMessage(playerName + " si è unito al tavolo!", this);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String decrypted = RSACryptography.decriptS(rsa, inputLine);
                    if (decrypted.startsWith("/chat ")) {
                        String message = StringUtils.substringAfter(decrypted, "/chat ");
                        broadcastMessage("[CHAT] " + playerName + ": " + message, null);
                    } else {
                        try {
                            gameEngine.processCommand(decrypted, this);
                        } catch (PokerException e) {
                            sendMessage("ERRORE: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void startTurnTimer() {
            timerExecutor.schedule(() -> {
                gameEngine.processCommand("fold", this);
                broadcastMessage(playerName + " timeout! Fold automatico", null);
            }, TURN_TIMEOUT, TimeUnit.SECONDS);
        }

        public void stopTurnTimer() {
            timerExecutor.shutdownNow();
        }

        public void sendMessage(String message) {
            String encrypted = RSACryptography.criptS(rsa, message);
            out.println(encrypted);
        }
    }
}
