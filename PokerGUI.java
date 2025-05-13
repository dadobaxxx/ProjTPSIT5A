package com.ProjTPSIT5A;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.paint.Paint;

import java.util.Optional;

import javafx.application.Platform;

public class PokerGUI {
    private final PokerClient client;
    private TextArea chatArea = new TextArea();
    private TextField inputField = new TextField();
    private HBox playerCards = new HBox(5);
    private HBox communityCards = new HBox(5);
    private Label potLabel = new Label("Pot: 0");
    private Label statusLabel = new Label("In attesa di giocatori...");
    private Stage primaryStage;
    private int maxRaise = 1000; // Esempio: valore ricevuto dal server

    // Colori e stili
    private static final Paint CARD_COLOR = Color.WHITE;
    private static final Paint CARD_BORDER = Color.BLACK;
    private static final Paint HEARTS_COLOR = Color.RED;
    private static final Paint DIAMONDS_COLOR = Color.RED;
    private static final Paint CLUBS_COLOR = Color.BLACK;
    private static final Paint SPADES_COLOR = Color.BLACK;

    public PokerGUI(PokerClient client) {
        this.client = client;
    }

    public void initializeUI(Stage primaryStage) {
        this.primaryStage = primaryStage;

        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(createPokerTable());
        mainPane.setBottom(createChatSection());
        mainPane.setRight(createPlayerControls());

        Scene scene = new Scene(mainPane);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("Texas Hold'em Poker - Testuale");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private StackPane createPokerTable() {
        StackPane table = new StackPane();
        table.setStyle("-fx-background-color: #2e8b57; -fx-padding: 20;");

        // Tavolo verde
        Rectangle tableSurface = new Rectangle(600, 400);
        tableSurface.widthProperty().bind(table.widthProperty().multiply(0.8));
        tableSurface.heightProperty().bind(table.heightProperty().multiply(0.6));
        tableSurface.setFill(Color.DARKGREEN);
        tableSurface.setStroke(Color.BLACK);
        tableSurface.setArcWidth(30);
        tableSurface.setArcHeight(30);

        VBox cardsLayout = new VBox(20);
        cardsLayout.setAlignment(Pos.CENTER);
        communityCards.setAlignment(Pos.CENTER);
        playerCards.setAlignment(Pos.CENTER);
        cardsLayout.getChildren().addAll(communityCards, playerCards);

        table.getChildren().addAll(tableSurface, cardsLayout);
        return table;
    }

    private VBox createChatSection() {
        VBox chatBox = new VBox(5);
        chatBox.setPadding(new Insets(10));
        chatBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-radius: 5;");

        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 14px;");
        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(150);
        inputField.setPromptText("Scrivi un messaggio...");
        inputField.setStyle("-fx-font-size: 14px;");
        chatArea.prefHeightProperty().bind(chatBox.heightProperty().multiply(0.7));
        inputField.prefWidthProperty().bind(chatBox.widthProperty().subtract(60));

        Button sendButton = new Button("Invia");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        sendButton.setOnAction(e -> sendChatMessage());
        inputField.setOnAction(e -> sendChatMessage());
        HBox inputContainer = new HBox(5, inputField, sendButton);
        inputContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(inputField, Priority.ALWAYS); // Espande il campo input
        chatBox.getChildren().addAll(
                new Label("Chat:"),
                scrollPane,
                inputContainer);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        chatBox.prefHeightProperty().bind(primaryStage.heightProperty().multiply(0.3));

        return chatBox;
    }

    private void sendChatMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            client.sendMessage("/chat " + message);
            inputField.clear();
        }
    }

    private VBox createPlayerControls() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-border-color: gray; -fx-border-width: 1;");

        Button foldButton = new Button("Fold");
        Button checkButton = new Button("Check");
        Button callButton = new Button("Call");
        Button raiseButton = new Button("Raise");
        TextField raiseAmount = new TextField();

        foldButton.setOnAction(e -> client.sendMessage("fold"));
        checkButton.setOnAction(e -> client.sendMessage("check"));
        callButton.setOnAction(e -> client.sendMessage("call"));
        raiseButton.setOnAction(e -> handleRaise(raiseAmount.getText()));
        HBox raiseBox = new HBox(5, raiseAmount, raiseButton);
        controls.getChildren().addAll(
                potLabel,
                new Separator(),
                foldButton,
                checkButton,
                callButton,
                raiseBox,
                new Separator(),
                statusLabel);
        return controls;
    }

    public void updateCards(String cardData) {
        communityCards.getChildren().clear();
        playerCards.getChildren().clear();

        String[] parts = cardData.split(";");
        for (String part : parts) {
            if (part.startsWith("COMMUNITY:")) {
                addCardBoxes(part.substring(10).split(","), communityCards);
            } else if (part.startsWith("PLAYER:")) {
                addCardBoxes(part.substring(7).split(","), playerCards);
            }
        }
    }

    private void addCardBoxes(String[] cards, HBox container) {
        for (String card : cards) {
            String[] cardParts = card.split("_");
            if (cardParts.length == 2) {
                try {
                    GameEngine.Rank rank = GameEngine.Rank.valueOf(cardParts[0]);
                    GameEngine.Suit suit = GameEngine.Suit.valueOf(cardParts[1]);
                    container.getChildren().add(createCardBox(rank, suit));
                } catch (IllegalArgumentException e) {
                    showError("Carta non valida", "Formato carta errato: " + card);
                }

            }
        }
    }

    private StackPane createCardBox(GameEngine.Rank rank, GameEngine.Suit suit) {
        Rectangle cardRect = new Rectangle(60, 90);
        cardRect.setFill(CARD_COLOR);
        cardRect.setStroke(CARD_BORDER);
        cardRect.setArcWidth(10);
        cardRect.setArcHeight(10);
        cardRect.setStrokeWidth(1);
        Text cardText = new Text(getCardSymbol(rank, suit));
        cardText.setFont(Font.font(14));
        cardText.setFill(getSuitColor(suit));

        StackPane cardBox = new StackPane(cardRect, cardText);
        cardBox.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 5, 0, 1, 1);");
        cardBox.setOnMouseEntered(e -> {
            cardRect.setStroke(Color.GOLD); // Bordo dorato al passaggio
            cardRect.setStrokeWidth(3);
        });
        cardBox.setOnMouseExited(e -> {
            cardRect.setStroke(CARD_BORDER);
            cardRect.setStrokeWidth(1);
        });

        return cardBox;
    }

    public void highlightCurrentPlayer(boolean isCurrent) {
        String style = isCurrent ? "-fx-border-color: #00FF00; -fx-border-width: 3;" : "";
        playerCards.setStyle(style);
    }

    private String getCardSymbol(GameEngine.Rank rank, GameEngine.Suit suit) {
        String rankSymbol = rank.toString().substring(0, 1);
        if (rank == GameEngine.Rank.TEN)
            rankSymbol = "10";

        String suitSymbol = "";
        switch (suit) {
            case HEARTS:
                suitSymbol = "♥";
                break;
            case DIAMONDS:
                suitSymbol = "♦";
                break;
            case CLUBS:
                suitSymbol = "♣";
                break;
            case SPADES:
                suitSymbol = "♠";
                break;
        }

        return rankSymbol + suitSymbol;
    }

    private Paint getSuitColor(GameEngine.Suit suit) {
        switch (suit) {
            case HEARTS:
                return HEARTS_COLOR;
            case DIAMONDS:
                return DIAMONDS_COLOR;
            case CLUBS:
                return CLUBS_COLOR;
            case SPADES:
                return SPADES_COLOR;
            default:
                return Color.BLACK;
        }
    }

    private void handleRaise(String amount) {
        try {
            int amt = Integer.parseInt(amount);
            if (amt <= 0) {
                showError("Importo non valido", "Inserisci un numero positivo");
            } else if (amt > maxRaise) {
                showError("Importo eccessivo", "Il massimo è " + maxRaise);
            } else {
                client.sendMessage("raise " + amt);
            }
        } catch (NumberFormatException e) {
            showError("Formato errato", "Inserisci un numero valido");
        }
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void appendChatMessage(String message) {
        Platform.runLater(() -> {
            chatArea.appendText(message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE); // Scroll alla fine
        });
    }

    public void updatePot(String potMessage) {
        potLabel.setText(potMessage);
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
    }

    public Optional<String> showNameDialog() {
        TextInputDialog dialog = new TextInputDialog("Player");
        dialog.setTitle("Player Name");
        dialog.setHeaderText("Benvenuto al tavolo di poker!");
        dialog.setContentText("Inserisci il tuo nome:");

        Optional<String> result = dialog.showAndWait();
        return result;
    }
}
