package com.ProjTPSIT5A;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Logger;

public class RSACryptography {
    private static final Logger logger = Logger.getLogger(RSACryptography.class.getName());

    private final BigInteger n;
    private final BigInteger d;
    private final BigInteger e;
    private final BigInteger p;
    private final BigInteger q;

    public static class CryptoException extends Exception {
        public CryptoException(String operation, Throwable cause) {
            super("[CRYPTO] Errore durante " + operation + ": " + cause.getMessage(), cause);
        }
    }

    public RSACryptography(int bitlen) {
        SecureRandom random = new SecureRandom();

        // Genera numeri primi p e q
        this.p = new BigInteger(bitlen / 2, 100, random);
        this.q = new BigInteger(bitlen / 2, 100, random);

        // Calcola n = p*q
        this.n = p.multiply(q);

        // Calcola phi(n) = (p-1)*(q-1)
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        // Esponente pubblico fisso (standard RSA)
        this.e = BigInteger.valueOf(65537);

        // Verifica che e e phi siano coprimi
        if (!e.gcd(phi).equals(BigInteger.ONE)) {
            throw new RuntimeException("e non è coprimo con phi. Rigenera le chiavi.");
        }

        // Calcola esponente privato d
        this.d = e.modInverse(phi);

        logger.info("Chiavi generate: e=" + e + ", n=" + n.toString(16).substring(0, 16) + "...");
    }

    public RSACryptography(BigInteger e, BigInteger n) {
        this.e = e;
        this.n = n;
        this.d = null; // Solo per decrittografia lato client
        this.p = this.q = null;
    }

    public static String criptS(RSACryptography rsa, String input) throws CryptoException {
        try {
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            BigInteger message = new BigInteger(1, bytes); // Usa 1 per garantire positivo
            BigInteger encrypted = message.modPow(rsa.e, rsa.n);
            return encrypted.toString();
        } catch (Exception ex) {
            throw new CryptoException("crittografia", ex);
        }
    }

    public static String encryptForServer(RSACryptography serverRsa, String message) throws CryptoException {
        // Usa SOLO la chiave pubblica del server (e, n)
        return criptS(serverRsa, message);
    }

    public static String decriptS(RSACryptography rsa, String encrypted) {
        if (encrypted == null || encrypted.trim().isEmpty()) {
            logger.warning("Tentativo di decrittare stringa vuota");
            return "";
        }
        if (rsa.d == null) {
            return encrypted;
        }

        try {
            BigInteger encryptedValue = new BigInteger(encrypted);
            if (rsa.d == null) {
                throw new CryptoException("decrittografia",
                        new NullPointerException("Chiave privata 'd' non inizializzata"));
            }
            BigInteger decrypted = encryptedValue.modPow(rsa.d, rsa.n);
            return new String(decrypted.toByteArray(), StandardCharsets.UTF_8);
        } catch (NumberFormatException ex) {
            logger.severe("Formato messaggio non valido: " + encrypted);
            return "";
        } catch (Exception ex) {
            logger.severe("Errore decrittografia: " + ex.getMessage());
            return "";
        }
    }

    public BigInteger getE() {
        return e;
    }

    public BigInteger getN() {
        return n;
    }
}