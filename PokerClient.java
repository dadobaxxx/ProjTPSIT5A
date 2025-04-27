import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.math.BigInteger;
import java.util.Optional;

public class PokerClient extends Application {
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
                String decrypted = RSACryptography.decriptS(rsa, serverMessage);
                handleServerMessage(decrypted);
            }
        } catch (Exception ex) {
            Platform.runLater(() -> gui.showError("Errore di connessione", "Connessione al server persa"));
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("CARD:")) {
            Platform.runLater(() -> gui.updateCards(message));
        } else if (message.startsWith("POT:")) {
            Platform.runLater(() -> gui.updatePot(message));
        } else {
            Platform.runLater(() -> gui.appendChatMessage(message));
        }
    }

    public void sendMessage(String message) {
        try {
            String encrypted = RSACryptography.criptS(rsa, message);
            out.println(encrypted);
        } catch (Exception e) {
            gui.showError("Errore crittografia", "Impossibile inviare il messaggio");
        }
    }
}
