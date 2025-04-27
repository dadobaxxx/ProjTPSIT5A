import java.util.*;
import java.util.concurrent.*;

public class GameEngine {
    private List<String> players = new ArrayList<>();
    private Map<String, List<Card>> playerCards = new HashMap<>();
    private Deck deck = new Deck();
    private List<Card> communityCards = new ArrayList<>();
    private int currentPlayer = 0;
    private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> currentTimer;
    private Map<String, PokerServer.ClientHandler> clientMap = new HashMap<>();
    private int maxBet = 100;

    public static class PokerException extends Exception {
        public PokerException(String message) {
            super(message);
        }
    }

    public static class InvalidBetException extends PokerException {
        public InvalidBetException(String message) {
            super(message);
        }
    }

    public static class DeckEmptyException extends PokerException {
        public DeckEmptyException() {
            super("Mazzo esaurito");
        }
    }

    public enum HandRank {
        HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND, STRAIGHT,
        FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH, ROYAL_FLUSH
    }

    public enum Suit {
        HEARTS, DIAMONDS, CLUBS, SPADES
    }

    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
        EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);

        public final int value;

        Rank(int value) {
            this.value = value;
        }
    }

    public static class Card {
        public final Rank rank;
        public final Suit suit;

        public Card(Rank rank, Suit suit) {
            this.rank = rank;
            this.suit = suit;
        }
    }

    public static class Deck {
        private List<Card> cards = new ArrayList<>();
        private int index = 0;

        public Deck() {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    cards.add(new Card(rank, suit));
                }
            }
            Collections.shuffle(cards);
        }

        public Card draw() throws DeckEmptyException {
            if (index >= cards.size()) {
                throw new DeckEmptyException();
            }
            return cards.get(index++);
        }
    }

    public void addPlayer(String playerName, PokerServer.ClientHandler client) {
        try {
            players.add(playerName);
            playerCards.put(playerName, new ArrayList<>());
            clientMap.put(playerName, client);
            dealCards(playerName);
        } catch (DeckEmptyException e) {
            client.sendMessage("ERRORE: Impossibile distribuire carte - " + e.getMessage());
        }
    }

    private void dealCards(String player) throws DeckEmptyException {
        playerCards.get(player).add(deck.draw());
        playerCards.get(player).add(deck.draw());
    }

    public void processCommand(String command, PokerServer.ClientHandler client) {
        cancelTimer();

        try {
            if (command.startsWith("bet ")) {
                int amount = Integer.parseInt(command.substring(4));
                validateBet(amount, client.playerName);
            } else if (command.equals("fold")) {
                int playerIndex = players.indexOf(client.playerName);
                players.remove(client.playerName);
                clientMap.remove(client.playerName);
                playerCards.remove(client.playerName);
                if (currentPlayer >= playerIndex && currentPlayer > 0) {
                    currentPlayer--;
                }
            }
            nextPlayer();
            startTimer();
        } catch (NumberFormatException e) {
            client.sendMessage("ERRORE: Importo non valido");
        } catch (InvalidBetException e) {
            client.sendMessage("ERRORE: " + e.getMessage());
        }
    }

    private void startTimer() {
        final PokerServer.ClientHandler currentClient = getCurrentClient();
        currentTimer = timer.schedule(() -> {
            PokerServer.broadcastMessage("Timeout per " + players.get(currentPlayer), null);
            processCommand("fold", currentClient);
        }, 30, TimeUnit.SECONDS);
    }

    private void cancelTimer() {
        if (currentTimer != null) {
            currentTimer.cancel(true);
        }
    }

    private void nextPlayer() {
        if (players.isEmpty())
            return;
        currentPlayer = (currentPlayer + 1) % players.size();
    }

    private PokerServer.ClientHandler getCurrentClient() {
        if (players.isEmpty())
            return null;
        return clientMap.get(players.get(currentPlayer));
    }

    public HandRank evaluatePlayerHand(String playerName) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerCards.get(playerName));
        allCards.addAll(communityCards);
        return HandEvaluator.evaluateHand(allCards);
    }

    private void validateBet(int amount, String playerName) throws InvalidBetException {
        if (amount <= 0) {
            throw new InvalidBetException("L'importo deve essere maggiore di zero");
        }
        if (amount > maxBet) {
            throw new InvalidBetException("Superato il massimo consentito (" + maxBet + ")");
        }
    }
}
