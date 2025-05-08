import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Jessica Theodore
 * Simple Shamir's Secret Sharing Demo
 * Uses BigInteger for cryptographic security
 * Perfect for learning the core concepts
 */
public class SimpleSSSDemo {

    // We use SecureRandom for cryptographically strong randomness
    private static final SecureRandom random = new SecureRandom();

    public static void main(String[] args) {
        // 1. SETUP - Choose small numbers for demo purposes
        BigInteger secret = new BigInteger("123456789"); // Our secret number
        int totalShares = 5;  // Total shares to generate (n)
        int threshold = 3;    // Minimum shares needed to reconstruct (k)
        BigInteger prime = BigInteger.valueOf(2087); // Small prime for demo
        
        System.out.println("Original Secret: " + secret);
        System.out.println("Generating " + totalShares + " shares with threshold " + threshold);
        System.out.println("Using prime modulus: " + prime + "\n");

        // 2. SPLIT the secret into shares
        Map<Integer, BigInteger> shares = splitSecret(secret, totalShares, threshold, prime);
        
        // Print all generated shares
        System.out.println("Generated Shares:");
        shares.forEach((x, y) -> System.out.println("  Share #" + x + ": " + y));
        
        // 3. RECONSTRUCT - Let's use shares 1, 3, and 5 (any 3 will work)
        System.out.println("\n Reconstructing from shares 1, 3, and 5...");
        Map<Integer, BigInteger> someShares = new HashMap<>();
        someShares.put(1, shares.get(1));
        someShares.put(3, shares.get(3));
        someShares.put(5, shares.get(5));
        
        BigInteger reconstructed = reconstructSecret(someShares, prime);
        System.out.println("Reconstructed Secret: " + reconstructed);
    }

    /**
     * Splits a secret into shares using SSS
     * @param secret The number to split
     * @param n Total number of shares
     * @param k Threshold (minimum shares needed)
     * @param prime Prime number for finite field
     * @return Map of share numbers (x) to values (y)
     */
    public static Map<Integer, BigInteger> splitSecret(BigInteger secret, int n, int k, BigInteger prime) {
        // Create polynomial: f(x) = secret + a₁x + a₂x² + ... + aₖ₋₁xᵏ⁻¹
        BigInteger[] coefficients = new BigInteger[k-1];
        
        // Generate random coefficients (except a₀ which is our secret)
        System.out.println("Polynomial coefficients:");
        System.out.println("  a₀ = " + secret + " (the secret)");
        for (int i = 0; i < k-1; i++) {
            coefficients[i] = new BigInteger(prime.bitLength()-1, random);
            System.out.println("  a" + (i+1) + " = " + coefficients[i]);
        }
        
        // Generate n shares (evaluations of the polynomial)
        Map<Integer, BigInteger> shares = new HashMap<>();
        for (int x = 1; x <= n; x++) {
            BigInteger y = secret; // Start with the secret (a₀)
            BigInteger xValue = BigInteger.valueOf(x);
            
            // Calculate each term of the polynomial
            for (int exp = 1; exp < k; exp++) {
                BigInteger term = coefficients[exp-1].multiply(xValue.pow(exp));
                y = y.add(term);
            }
            
            // Mod by our prime to stay in finite field
            y = y.mod(prime);
            shares.put(x, y);
        }
        
        return shares;
    }

    /**
     * Reconstructs secret from shares using Lagrange interpolation
     * @param shares Map of share numbers to values
     * @param prime Same prime used during splitting
     * @return Reconstructed secret
     */
    public static BigInteger reconstructSecret(Map<Integer, BigInteger> shares, BigInteger prime) {
        BigInteger secret = BigInteger.ZERO;
        
        // For each share we're using to reconstruct...
        for (int i : shares.keySet()) {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;
            
            // Calculate Lagrange basis polynomial
            for (int j : shares.keySet()) {
                if (i != j) {
                    // numerator *= (0 - x_j)
                    numerator = numerator.multiply(BigInteger.valueOf(-j)).mod(prime);
                    // denominator *= (x_i - x_j)
                    denominator = denominator.multiply(BigInteger.valueOf(i - j)).mod(prime);
                }
            }
            
            // y_i * (numerator/denominator)
            BigInteger term = shares.get(i).multiply(numerator)
                                .multiply(denominator.modInverse(prime))
                                .mod(prime);
            
            secret = secret.add(term).mod(prime);
        }
        
        return secret;
    }
}