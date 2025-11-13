package org.example.db.postgres;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Herramienta de verificación rápida de SCRAM-SHA-256.
 * Calcula storedKey y serverKey para una combinación password+salt+iteraciones
 * y los imprime en Base64 para comparar con el verificador del servidor.
 *
 * Uso (desde Maven):
 *   mvn -DskipTests -Dexec.mainClass=org.example.db.postgres.ScramCalc exec:java
 * Opcionalmente con propiedades del sistema:
 *   -Dscram.password=postgres -Dscram.salt=G5ndocbEQCzhKpF+/UO/eA== -Dscram.iter=4096
 */
public class ScramCalc {
    public static void main(String[] args) throws Exception {
        String password = System.getProperty("scram.password", "postgres");
        String saltB64 = System.getProperty("scram.salt", "G5ndocbEQCzhKpF+/UO/eA==");
        int iterations = Integer.parseInt(System.getProperty("scram.iter", "4096"));

        byte[] salt = Base64.getDecoder().decode(saltB64);
        byte[] salted = hi(password, salt, iterations);
        byte[] clientKey = hmac(salted, "Client Key");
        byte[] storedKey = sha256(clientKey);
        byte[] serverKey = hmac(salted, "Server Key");

        System.out.println("SCRAM parameters:");
        System.out.println("  password='" + password + "' (length=" + password.length() + ")");
        System.out.println("  salt=" + saltB64 + " (len=" + salt.length + ") iterations=" + iterations);
        System.out.println("Derived keys (Base64):");
        System.out.println("  storedKey=" + Base64.getEncoder().encodeToString(storedKey));
        System.out.println("  serverKey=" + Base64.getEncoder().encodeToString(serverKey));
        System.out.println("Verifier format would be: SCRAM-SHA-256$" + iterations + ":" + saltB64 + "=" + Base64.getEncoder().encodeToString(storedKey) + ":" + Base64.getEncoder().encodeToString(serverKey));
        System.out.println("Compare with rolpassword from pg_authid to confirm if password matches.");
    }

    private static byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static byte[] sha256(byte[] data) throws Exception { return MessageDigest.getInstance("SHA-256").digest(data); }
    private static byte[] hi(String password, byte[] salt, int iterations) throws InvalidKeySpecException, Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}
