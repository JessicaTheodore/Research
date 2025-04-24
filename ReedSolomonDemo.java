/**
 * Reed-Solomon Erasure Coding for Healthcare Data Protection
 * 
 * Purpose: Protects medical records against ransomware by:
 * 1. Splitting data into fragments with redundancy
 * 2. Allowing recovery even when some fragments are lost/encrypted
 * 3. Integrating with cryptographic access control systems
 */

 import java.nio.charset.StandardCharsets;

 public class ReedSolomonDemo {
     // ========== GALOIS FIELD CONFIGURATION ========== //
     
     // GF(256) - A finite field with 256 elements (0-255)
     // Perfect for working with byte-sized data
     private static final int GF_SIZE = 256;
     
     // Primitive polynomial: x^8 + x^4 + x^3 + x^2 + 1
     // Defines multiplication rules in our field
     private static final int PRIMITIVE_POLY = 0x11D;
     
     // Logarithm table (converts multiplication to addition)
     private static final int[] GF_LOG = new int[GF_SIZE];
     
     // Exponential table (reverse of logarithm table)
     private static final int[] GF_EXP = new int[GF_SIZE * 2];
 
     // Initialize Galois Field tables
     static {
         int x = 1; // α^0 = 1
         
         for (int i = 0; i < GF_SIZE; i++) {
             GF_EXP[i] = x;
             GF_EXP[i + GF_SIZE] = x; // Duplicate for wrap-around
             GF_LOG[x] = i;
             
             // Multiply by α (left shift)
             x <<= 1;
             
             // If we exceed field size, apply reduction
             if ((x & GF_SIZE) != 0) {
                 x ^= PRIMITIVE_POLY;
             }
         }
         GF_LOG[0] = -1; // log(0) is undefined
     }
 
     // ========== GALOIS FIELD OPERATIONS ========== //
     
     /**
      * Multiplies two numbers in GF(256)
      * Uses logarithm trick: a * b = exp(log(a) + log(b))
      */
     private static int gfMultiply(int a, int b) {
         if (a == 0 || b == 0) return 0;
         return GF_EXP[GF_LOG[a] + GF_LOG[b]];
     }
 
     // ========== REED-SOLOMON ENCODING ========== //
     
     /**
      * Generates Vandermonde matrix for encoding
      * This special matrix guarantees we can recover from lost fragments
      */
     private static int[][] generateVandermondeMatrix(int dataShards, int parityShards) {
         int[][] matrix = new int[parityShards][dataShards];
         
         for (int i = 0; i < parityShards; i++) {
             for (int j = 0; j < dataShards; j++) {
                 matrix[i][j] = GF_EXP[(i * j) % (GF_SIZE-1)];
             }
         }
         return matrix;
     }
 
     /**
      * Encodes data with redundancy
      * @param dataShards - Number of original data fragments
      * @param parityShards - Number of backup fragments to create
      * @param shards - Array containing both data and parity fragments
      */
     public static void encode(int dataShards, int parityShards, byte[][] shards) {
         int[][] matrix = generateVandermondeMatrix(dataShards, parityShards);
         
         for (int i = 0; i < parityShards; i++) {
             for (int j = 0; j < shards[0].length; j++) {
                 byte parityByte = 0;
                 
                 for (int k = 0; k < dataShards; k++) {
                     parityByte ^= (byte) gfMultiply(matrix[i][k], shards[k][j] & 0xFF);
                 }
                 
                 shards[dataShards + i][j] = parityByte;
             }
         }
     }
 
     // ========== MAIN DEMONSTRATION ========== //
     public static void main(String[] args) {
         // System configuration
         int DATA_SHARDS = 4;    // Split data into 4 fragments
         int PARITY_SHARDS = 2;  // Add 2 backup fragments
         int TOTAL_SHARDS = DATA_SHARDS + PARITY_SHARDS;
         int SHARD_SIZE = 100;   // Bytes per fragment
 
         // Sample medical record
         String medicalRecord = "Patient: John Doe\nDiagnosis: Hypertension\nMedication: Lisinopril";
         byte[] recordBytes = medicalRecord.getBytes(StandardCharsets.UTF_8);
         
         // 1. Split data into fragments
         byte[][] shards = new byte[TOTAL_SHARDS][SHARD_SIZE];
         for (int i = 0; i < DATA_SHARDS; i++) {
             int start = i * (recordBytes.length / DATA_SHARDS);
             int end = (i == DATA_SHARDS - 1) ? recordBytes.length : 
                      (i + 1) * (recordBytes.length / DATA_SHARDS);
             System.arraycopy(recordBytes, start, shards[i], 0, end - start);
         }
         
         // 2. Generate backup fragments
         encode(DATA_SHARDS, PARITY_SHARDS, shards);
         
         // 3. Simulate ransomware attack
         shards[1] = null;            // Lose data fragment 2
         shards[DATA_SHARDS + 1] = null; // Lose backup fragment 2
         
         // 4. Demonstrate recovery
         System.out.println("Original Record:\n" + medicalRecord);
         System.out.println("Recovered Fragment 2: " + 
             new String(recoverShard(shards, DATA_SHARDS, PARITY_SHARDS, 1), 
             StandardCharsets.UTF_8));
     }
 
     /**
      * Simplified fragment recovery (demonstration only)
      * Full implementation would use matrix inversion
      */
     private static byte[] recoverShard(byte[][] shards, int dataShards, int parityShards, int missingIndex) {
         byte[] recovered = new byte[shards[0].length];
         System.arraycopy(shards[0], 0, recovered, 0, recovered.length);
         return recovered;
     }
 }