import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.logging.Logger;

public class rsaEnpowered {
    private static final Logger logger = Logger.getLogger(rsaEnpowered.class.getName());
    private BigInteger n, d, e, p, q;

    private rsaEnpowered(int bitlen) {
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

    private static String criptS(rsaEnpowered rsa, String input) {
        String[] inTkn = input.split("\\s+"); // serve a dividere la stringa basandosi su uno o più spazi vuoti
        StringBuilder output = new StringBuilder();

        for (String token : inTkn) {
            BigInteger bigIntTkn = new BigInteger(token.getBytes());
            BigInteger criptTkn = rsa.criptato(bigIntTkn);
            output.append(criptTkn.toString()).append(" ");
        }

        return output.toString().trim(); // rimuove tutti gli spazi dal testo ad eccezione dei singoli spazi tra le parole
    }

    private static String decriptS(rsaEnpowered rsa, String criptato) {
        String[] criptTokens = criptato.split("\\s+"); // serve a dividere la stringa basandosi su uno o più spazi vuoti
        StringBuilder output = new StringBuilder();

        for (String tkn : criptTokens) {
            BigInteger bigIntTkn = new BigInteger(tkn);
            BigInteger outTkn = rsa.decriptato(bigIntTkn);
            output.append(new String(outTkn.toByteArray())).append(" ");
        }

        return output.toString().trim(); // rimuove tutti gli spazi dal testo ad eccezione dei singoli spazi tra le parole
    }

    public static void main(String[] args) {
        int bitlen = 1024;
        rsaEnpowered rsa = new rsaEnpowered(bitlen);
        Scanner inp = new Scanner(System.in);

        String input = inp.nextLine();
        logger.info("Testo utente: " + input);

        String criptato = criptS(rsa, input);
        logger.info("Testo criptato: " + criptato);

        String decriptato = decriptS(rsa, criptato);
        logger.info("Testo decriptato: " + decriptato);
    }
}
