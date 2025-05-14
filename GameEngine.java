package com.ProjTPSIT5A;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.ProjTPSIT5A.PokerServer.NetworkException;

public class GameEngine {
    private static final Logger logger = Logger.getLogger(GameEngine.class.getName());

    private List<String> players = new ArrayList<>();
    private Map<String, List<Card>> playerCards = new HashMap<>();
    private Deck deck = new Deck();
    private List<Card> communityCards = new ArrayList<>();
    private int currentPlayer = 0;
    private int maxBet = 100;
    private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> currentTimer;
    private Map<String, PokerServer.ClientHandler> clientMap = new HashMap<>();

    // Eccezioni
    public static class PokerException extends Exception {
        public PokerException(String message) {
            super(message);
        }
    }

    public static class InvalidBetException extends PokerException {
        public InvalidBetException(int max) {
            super("Massimo: " + max);
        }

        public InvalidBetException(String msg) {
            super(msg);
        } // Costruttore aggiunto
    }

    public static class DeckEmptyException extends PokerException {
        public DeckEmptyException() {
            super("Mazzo esaurito");
        }
    }

    public static class NotYourTurnException extends PokerException {
        public NotYourTurnException(String player) {
            super(player + ": Non è il tuo turno!");
        }
    }

    public static class InvalidPlayerException extends PokerException {
        public InvalidPlayerException(String player) {
            super("Giocatore non valido: " + player);
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
            if (index >= cards.size())
                throw new DeckEmptyException();
            return cards.get(index++);
        }
    }

    // Metodi principali
    public void addPlayer(String playerName, PokerServer.ClientHandler client) throws PokerException {
        if (players.contains(playerName)) {
            throw new InvalidPlayerException("Nome già in uso: " + playerName);
        }

        try {
            players.add(playerName);
            playerCards.put(playerName, new ArrayList<>());
            clientMap.put(playerName, client);
            dealCards(playerName);

            List<Card> cards = playerCards.get(playerName);
            String card1 = cards.get(0).rank + "_" + cards.get(0).suit;
            String card2 = cards.get(1).rank + "_" + cards.get(1).suit;

            try {
                client.sendMessage("CARD:PLAYER:" + card1 + "," + card2);
                logger.info("Carte inviate a " + playerName);
            } catch (PokerServer.NetworkException e) {
                // Gestione errori di rete
                logger.log(Level.SEVERE, "Errore invio carte a " + playerName, e);
                removePlayer(playerName); // Pulizia dello stato
                throw new PokerException("Connessione fallita per " + playerName);
            }

        } catch (DeckEmptyException e) {
            throw new PokerException("Impossibile aggiungere giocatore: " + e.getMessage());
        }
    }

    public void processCommand(String command, PokerServer.ClientHandler client) throws PokerException {
        cancelTimer();
        String player = client.playerName;

        if (!players.get(currentPlayer).equals(player)) {
            throw new NotYourTurnException(player);
        }

        try {
            if (command.startsWith("bet ")) {
                handleBet(command, client.playerName);
            } else if (command.equals("fold")) {
                handleFold(player);
            }
            nextPlayer();
            startTimer();
        } catch (NumberFormatException e) {
            throw new InvalidBetException("Formato non valido");
        }
    }

    private PokerServer.ClientHandler getCurrentClient() {
        if (players.isEmpty())
            return null;
        return clientMap.get(players.get(currentPlayer));
    }

    private void handleBet(String command, String player) throws InvalidBetException {
        int amount = Integer.parseInt(command.substring(4));
        validateBet(amount);
        logger.info(player + " scommette " + amount);
    }

    private void validateBet(int amount) throws InvalidBetException {
        if (amount <= 0) {
            throw new InvalidBetException("Importo deve essere positivo");
        }
        if (amount > maxBet) {
            throw new InvalidBetException(maxBet);
        }
    }

    private void handleFold(String player) {
        int index = players.indexOf(player);
        players.remove(player);
        playerCards.remove(player);
        clientMap.remove(player);

        if (currentPlayer >= index && currentPlayer > 0) {
            currentPlayer--;
        }
        logger.info(player + " folds");
    }

    // Metodi di supporto
    private void dealCards(String player) throws DeckEmptyException {
        if (deck.cards.size() - deck.index < 2) {
            throw new DeckEmptyException();
        }
        playerCards.get(player).add(deck.draw());
        playerCards.get(player).add(deck.draw());
    }

    private void nextPlayer() {
        if (players.isEmpty())
            return;
        currentPlayer = (currentPlayer + 1) % players.size();
        logger.info("Turno di " + players.get(currentPlayer));
    }

    public void removePlayer(String playerName) {
        players.remove(playerName);
        playerCards.remove(playerName);
        clientMap.remove(playerName);
        logger.info(playerName + " rimosso per errori di rete");
    }

    private void startTimer() {
        currentTimer = timer.schedule(() -> {
            PokerServer.ClientHandler current = getCurrentClient();
            try {
                processCommand("fold", current);
            } catch (PokerException e) {
                logger.warning("Timeout fallito: " + e.getMessage());
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void cancelTimer() {
        if (currentTimer != null) {
            currentTimer.cancel(true);
        }
    }

    public HandRank evaluatePlayerHand(String playerName) throws InvalidPlayerException {
        if (!playerCards.containsKey(playerName)) {
            throw new InvalidPlayerException(playerName);
        }

        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerCards.get(playerName));
        allCards.addAll(communityCards);

        return HandEvaluator.evaluateHand(allCards);
    }

}
