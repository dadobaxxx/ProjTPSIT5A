package com.ProjTPSIT5A;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.*;

public class PokerClient extends Application {
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }

    private static final Logger logger = Logger.getLogger(PokerServer.class.getName());
    private PokerGUI gui;
    private PrintWriter out;
    private BufferedReader in;
    private RSACryptography rsa;
    private String playerName;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        gui = new PokerGUI(this);
        gui.initializeUI(primaryStage);
        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 12345);
            socket.setSoTimeout(10000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            BigInteger e = new BigInteger(in.readLine());
            BigInteger n = new BigInteger(in.readLine());
            rsa = new RSACryptography(e, n);

            Optional<String> name = gui.showNameDialog();
            if (!name.isPresent()) {
                Platform.exit();
                return;
            }
            playerName = name.get();
            sendMessage(playerName);

            new Thread(this::handleServerMessages).start();

        } catch (IOException ex) {
            gui.showError("Errore di connessione", "Impossibile connettersi al server");
            Platform.exit();
        }

    }

    private void handleServerMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                final String finalMessage = serverMessage; // Variabile effectively final
                logger.info("Messaggio ricevuto: " + finalMessage);

                Platform.runLater(() -> {
                    try {
                        if (finalMessage.trim().isEmpty()) {
                            logger.warning("Messaggio vuoto ignorato");
                            return;
                        }
                        handleServerMessage(finalMessage); // Ora può essere usata in lambda
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Errore elaborazione messaggio", e);
                    }
                });
            }
        } catch (SocketTimeoutException e) {
            Platform.runLater(() -> gui.showError("Timeout", "Connessione al server persa"));
        } catch (Exception ex) {
            Platform.runLater(() -> gui.showError("Errore", "Connessione interrotta"));
        }
    }

    private void handleServerMessage(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                logger.warning("Ricevuto messaggio vuoto dal server");
                return;
            }

            // Gestione ordinata per tipo di messaggio
            if (message.startsWith("CARD:")) {
                handleCardMessage(message);
            } else if (message.equals("PING")) {
                handlePingMessage();
            } else if (message.startsWith("[ERRORE]")) {
                handleErrorMessage(message);
            } else {
                handleChatMessage(message);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore elaborazione messaggio: " + message, e);
            Platform.runLater(() -> gui.showError("Errore", "Formato messaggio non valido: " + message));
        }
    }

    // Metodi helper separati per ogni tipo di messaggio
    private void handleCardMessage(String cardData) {
        Platform.runLater(() -> gui.updateCards(cardData));
    }

    private void handlePingMessage() {
        // Logica per gestire heartbeat (es: reset timer)
        logger.fine("Ricevuto PING dal server");
    }

    private void handleErrorMessage(String errorMsg) {
        Platform.runLater(() -> gui.showError("Errore Server", errorMsg.substring("[ERRORE]".length())));
    }

    private void handleChatMessage(String chatMsg) {
        Platform.runLater(() -> gui.appendChatMessage(chatMsg));
    }

    public void sendMessage(String message) {
        if (message.equals(playerName)) {
            try {
                String encrypted = RSACryptography.criptS(rsa, message);
                out.println(encrypted);
            } catch (Exception e) {
                gui.showError("Errore crittografia", "Impossibile inviare il nome");
            }
        } else {
            out.println(message);
        }
    }
}
