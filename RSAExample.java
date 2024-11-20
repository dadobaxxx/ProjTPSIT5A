import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Scanner;
import java.util.logging.Logger;

public class RSAExample {
    private static final Logger logger = Logger.getLogger(RSAExample.class.getName());
    private BigInteger n, d, e, p, q;

    private RSAExample (int bitlen){
        SecureRandom random = new SecureRandom();
        p= new BigInteger(bitlen, 100, random);
        q= new BigInteger(bitlen, 100, random);

        n= p.multiply(q);
        BigInteger v = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        do{
            e= BigInteger.probablePrime(bitlen, random);
        } while (!e.gcd(v).equals(BigInteger.ONE));

        d= e.modInverse(v);
    }

    private BigInteger criptato(BigInteger testo){
        return testo.modPow(e, n);
    }

    private BigInteger decriptato(BigInteger criptato){
        return criptato.modPow(d, n);
    }

    public static void main(String[] args) {
        int bitlen = 1024;
        RSAExample rsa= new RSAExample(bitlen);
        Scanner inp = new Scanner (System.in);

        String input = inp.nextLine();
        logger.info("testo utente: " + input);

        BigInteger testo = new BigInteger(input);
        BigInteger criptato = rsa.criptato(testo);
        BigInteger decriptato = rsa.decriptato(criptato);
        logger.info ("criptato: " + criptato);
        logger.info ("decriptato: " + decriptato);
    }
    
}
