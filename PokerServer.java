package com.ProjTPSIT5A;

import org.apache.commons.lang3.StringUtils;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class PokerServer {
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }
    private static final Logger logger = Logger.getLogger(PokerServer.class.getName());
    private static final int PORT = 12345;
    private static final int TURN_TIMEOUT = 30;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static GameEngine gameEngine = new GameEngine();
    private static RSACryptography rsa = new RSACryptography(2048);

    public static class NetworkException extends Exception {
        public NetworkException(String detail) {
            super("[NETWORK] Errore di connessione: " + detail);
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Server avviato sulla porta " + PORT);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientThread = new ClientHandler(socket, gameEngine, rsa);
                    clients.add(clientThread);
                    executor.execute(clientThread);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Errore nell'accettare la connessione", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossibile avviare il server", e);
        } finally {
            executor.shutdown();
        }
    }

    static void broadcastMessage(String message, ClientHandler exclude) {
        for (ClientHandler client : new ArrayList<>(clients)) {
            if (client != exclude) {
                try {
                    client.sendMessage(message);
                } catch (NetworkException e) {
                    logger.log(Level.WARNING, "Invio messaggio fallito a " + client.playerName, e);
                }
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

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Invia chiave pubblica
                sendUnencrypted(rsa.getE().toString());
                sendUnencrypted(rsa.getN().toString());

                String encryptedName = in.readLine();
                playerName = RSACryptography.decriptS(rsa, encryptedName);

                try {
                    gameEngine.addPlayer(playerName, this);
                    broadcastMessage(playerName + " si è unito al tavolo!", this);
                } catch (GameEngine.PokerException e) {
                    sendError("Registrazione fallita", e.getMessage());
                    return;
                }

                processClientCommands();

            } catch (IOException e) {
                logger.log(Level.WARNING, "Errore connessione client", e);
            } finally {
                cleanup();
            }
        }

        private void processClientCommands() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    String decrypted = RSACryptography.decriptS(rsa, inputLine);
                    handleCommand(decrypted);

                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Lettura comandi fallita", e);
            }
        }

        private void handleCommand(String decrypted) {
            if (decrypted.startsWith("/chat ")) {
                handleChatMessage(decrypted);
            } else {
                try {
                    gameEngine.processCommand(decrypted, this);
                } catch (GameEngine.PokerException e) {
                    sendError("Comando non valido", e.getMessage());
                }
            }
        }

        private void handleChatMessage(String decrypted) {
            String message = StringUtils.substringAfter(decrypted, "/chat ");
            broadcastMessage("[CHAT] " + playerName + ": " + message, null);
        }

        private void sendUnencrypted(String message) {
            out.println(message);
        }

        public void sendMessage(String message) throws NetworkException {
            try {
                String encrypted = RSACryptography.criptS(rsa, message);
                out.println(encrypted);
            } catch (RSACryptography.CryptoException e) {
                throw new NetworkException("Crittografia fallita: " + e.getMessage());
            }
        }

        private void sendError(String type, String detail) {
            try {
                sendMessage("[ERRORE] " + type + ": " + detail);
            } catch (NetworkException e) {
                logger.log(Level.WARNING, "Impossibile inviare errore al client", e);
            }
        }

        public void startTurnTimer() {
            try {
                timerExecutor.schedule(() -> {
                    try {
                        gameEngine.processCommand("fold", this);
                        broadcastMessage(playerName + " timeout! Fold automatico", null);
                    } catch (GameEngine.PokerException e) {
                        logger.log(Level.WARNING, "Timeout fallito", e);
                    }
                }, TURN_TIMEOUT, TimeUnit.SECONDS);
            } catch (RejectedExecutionException e) {
                logger.log(Level.WARNING, "Timer interrotto", e);
            }
        }

        private void cleanup() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                timerExecutor.shutdownNow();
                clients.remove(this);
                if (playerName != null) {
                    broadcastMessage(playerName + " ha lasciato il tavolo.", this);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Pulizia risorse fallita", e);
            }
        }
    }
}