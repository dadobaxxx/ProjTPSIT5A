import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class RSAExample {

    private static final Logger logger = Logger.getLogger(RSAExample.class.getName());
    private BigInteger n, d, e;

    // Generazione delle chiavi RSA
    private RSAExample(int bitlen) {
        SecureRandom random = new SecureRandom();
        BigInteger p = new BigInteger(bitlen / 2, 100, random);
        BigInteger q = new BigInteger(bitlen / 2, 100, random);
        n = p.multiply(q);
        BigInteger v = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        e = new BigInteger("65537"); // Valore comunemente utilizzato per e
        d = e.modInverse(v);
    }

    // Criptare un messaggio
    private BigInteger encrypt(BigInteger message) {
        return message.modPow(e, n);
    }

    // Decriptare un messaggio
    private BigInteger decrypt(BigInteger encrypted) {
        return encrypted.modPow(d, n);
    }

    public static void main(String[] args) {
        int bitlen = 2048;
        RSAExample rsa = new RSAExample(bitlen);

        String plaintext = system.in(); //prendi in input i dati ritardato
        logger.info("Testo in chiaro: " + plaintext);

        BigInteger message = new BigInteger(plaintext.getBytes());
        BigInteger encrypted = rsa.encrypt(message);
        logger.info("Messaggio criptato: " + encrypted);

        BigInteger decrypted = rsa.decrypt(encrypted);
        logger.info("Messaggio decriptato: " + new String(decrypted.toByteArray()));
    }
}
