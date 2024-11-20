import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.logging.Logger;

public class RSAExample {
    private static final Logger logger = Logger.getLogger(RSAExample.class.getName());
    private BigInteger n, d, e, p, q;

    private RSAExample(int bitlen) {
        SecureRandom random = new SecureRandom();
        p = new BigInteger(bitlen, 100, random);
        q = new BigInteger(bitlen, 100, random);

        n = p.multiply(q);
        BigInteger v = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        do {
            e = BigInteger.probablePrime(bitlen, random);
        } while (!e.gcd(v).equals(BigInteger.ONE));

        d = e.modInverse(v);
    }

    private BigInteger criptato(BigInteger testo) {
        return testo.modPow(e, n);
    }

    private BigInteger decriptato(BigInteger criptato) {
        return criptato.modPow(d, n);
    }

    private static String criptareStringa(RSAExample rsa, String input) {
        String[] tokens = input.split("\\s+");
        StringBuilder criptatoBuilder = new StringBuilder();

        for (String token : tokens) {
            BigInteger bigIntToken = new BigInteger(token.getBytes());
            BigInteger criptatoToken = rsa.criptato(bigIntToken);
            criptatoBuilder.append(criptatoToken.toString()).append(" ");
        }

        return criptatoBuilder.toString().trim();
    }

    private static String decriptareStringa(RSAExample rsa, String criptato) {
        String[] criptatoTokens = criptato.split("\\s+");
        StringBuilder decriptatoBuilder = new StringBuilder();

        for (String criptatoToken : criptatoTokens) {
            BigInteger bigIntCriptatoToken = new BigInteger(criptatoToken);
            BigInteger decriptatoToken = rsa.decriptato(bigIntCriptatoToken);
            decriptatoBuilder.append(new String(decriptatoToken.toByteArray())).append(" ");
        }

        return decriptatoBuilder.toString().trim();
    }

    public static void main(String[] args) {
        int bitlen = 1024;
        RSAExample rsa = new RSAExample(bitlen);
        Scanner inp = new Scanner(System.in);

        String input = inp.nextLine();
        logger.info("Testo utente: " + input);

        String criptato = criptareStringa(rsa, input);
        logger.info("Testo criptato: " + criptato);

        String decriptato = decriptareStringa(rsa, criptato);
        logger.info("Testo decriptato: " + decriptato);
    }
}
