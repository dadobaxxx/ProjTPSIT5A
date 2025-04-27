// 3. Classi per la gestione del gioco
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class RSACryptography {
    private BigInteger n, d, e, p, q;

    public RSACryptography(int bitlen) {
        generateKeys(bitlen);
    }

    public RSACryptography(BigInteger e, BigInteger n) {
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

    public static String criptS(RSACryptography rsa, String input) {
        return Arrays.stream(input.split(" "))
            .map(token -> new BigInteger(token.getBytes()))
            .map(bi -> bi.modPow(rsa.e, rsa.n).toString())
            .reduce((a, b) -> a + " " + b).orElse("");
    }

    public static String decriptS(RSACryptography rsa, String criptato) {
        return Arrays.stream(criptato.split(" "))
            .map(tkn -> new BigInteger(tkn))
            .map(bi -> new String(bi.modPow(rsa.d, rsa.n).toByteArray()))
            .reduce((a, b) -> a + " " + b).orElse("");
    }

    public BigInteger getE() { return e; }
    public BigInteger getN() { return n; }
}
