import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.logging.Logger;

public class RSAExample {
    private static final Logger logger = Logger.getLogger(RSAExample.class.getName());
    private BigInteger n, d, e, p, q;

    private RSAExample (int bitlen){
        SecureRandom random = new SecureRandom();
        p= new BigInteger(bitlen, 100, random);
        q= new BigInteger(bitlen, 100, random);

        n= p.multiply(q);
        BigInteger v = (p.subtract(BigInteger.ONE)).multiply(p.subtract(BigInteger.ONE));

        do{
            e= BigInteger.probablePrime(bitlen, random);
        } while (e.gcd(v).equals(BigInteger.ONE));

        d= e.modInverse(v);
    }

    private BigInteger criptato(BigInteger testo){
        return testo.modPow(e, n);
    }

    private BigInteger decriptato(BigInteger criptato){
        return criptato.modPow(d, n);
    }

    public static void main(String[] args) {
        
    }
    
}