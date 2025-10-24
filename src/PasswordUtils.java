import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// --- NEW FILE ---
// This class handles all password hashing and verification for security.
public class PasswordUtils {

    // Generates a new random "salt" for hashing
    private static byte[] getSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    // Hashes a password with a given salt
    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find hashing algorithm", e);
        }
    }

    // --- PUBLIC METHODS ---

    // Call this when REGISTERING a new user
    // It creates a new salt and returns a "salt:hash" string for database storage
    public static String generateSecurePassword(String password) {
        try {
            byte[] salt = getSalt();
            String hashedPassword = hashPassword(password, salt);
            String saltString = Base64.getEncoder().encodeToString(salt);
            return saltString + ":" + hashedPassword; // Store salt and hash together
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating secure password", e);
        }
    }

    // Call this when LOGGING IN a user
    // It compares a plain-text password to the "salt:hash" string from the database
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":");
            if (parts.length != 2) {
                return false; // Malformed hash
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            String storedHash = parts[1];

            // Hash the new plain password with the *old* salt
            String newHash = hashPassword(plainPassword, salt);

            // Compare the hashes
            return newHash.equals(storedHash);
        } catch (Exception e) {
            return false; // Error during decoding or hashing
        }
    }
}