// 3. Classi per la gestione del gioco
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class GameManagement {
    private BigInteger n, d, e, p, q;

    public GameManagement(int bitlen) {
        generateKeys(bitlen);
    }

    public GameManagement(BigInteger e, BigInteger n) {
        this.e = e;
        this.n = n;
    }

    private void generateKeys(int bitlen) {
        SecureRandom random = new SecureRandom();
        p = new BigInteger(bitlen, 100, random);
        q = new BigInteger(bitlen, 100, random);
        n = p.multiply(q);
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        
        do {
            e = new BigInteger(bitlen, random);
        } while (e.compareTo(phi) >= 0 || !e.gcd(phi).equals(BigInteger.ONE));
        
        d = e.modInverse(phi);
    }

    public static String criptS(GameManagement rsa, String input) {
        return Arrays.stream(input.split(" "))
            .map(token -> new BigInteger(token.getBytes()))
            .map(bi -> bi.modPow(rsa.e, rsa.n).toString())
            .reduce((a, b) -> a + " " + b).orElse("");
    }

    public static String decriptS(GameManagement rsa, String criptato) {
        return Arrays.stream(criptato.split(" "))
            .map(tkn -> new BigInteger(tkn))
            .map(bi -> new String(bi.modPow(rsa.d, rsa.n).toByteArray()))
            .reduce((a, b) -> a + " " + b).orElse("");
    }

    public BigInteger getE() { return e; }
    public BigInteger getN() { return n; }
}

class Deck {
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

    public Card draw() {
        return cards.get(index++);
    }
}

enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }
enum Rank { TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE }

class Card {
    Rank rank;
    Suit suit;
    
    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }
}
