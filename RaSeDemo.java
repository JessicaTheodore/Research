import java.util.*;
import java.security.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * RaSe Medical Record System DEMO
 * 
 * COMMANDS:
 * 
 * 1. Store Patient Data:
 *    doctor store patient1 "BloodPressure: 120/80, Diagnosis: Hypertension"
 * 
 * 2. Retrieve Patient Data:
 *    admin retrieve patient1
 * 
 * 3. Simulate Ransomware Attack:
 *    attacker corrupt patient1
 * 
 * 4. View Audit Log:
 *    audit viewlog
 * 
 * 5. View Change Log:
 *    audit viewchanges
 * 
 * 6. View All Patient IDs:
 *    audit listpatients
 * 
 * 7. Simulate System Failure:
 *    system setstate UNDER_ATTACK
 *    system setstate NORMAL
 * 
 * 8. Exit:
 *    exit
 */
public class RaSeDemo {
    // File paths
    private static final String AUDIT_LOG = "audit.log";
    private static final String PATIENT_RECORDS = "patient_records.sim";
    private static final String CHANGES_LOG = "changes.log";
    
    // Reed-Solomon config
    private static final int DATA_SHARDS = 4;
    private static final int PARITY_SHARDS = 2;
    private static final int TOTAL_SHARDS = DATA_SHARDS + PARITY_SHARDS;
    
    // Shamir's Secret Sharing
    private static final int SHAMIR_THRESHOLD = 3;
    private static final int SHAMIR_TOTAL = 5;
    
    // System state
    private enum SystemState { NORMAL, UNDER_ATTACK, RECOVERY }
    private SystemState currentState = SystemState.NORMAL;
    
    // Data storage
    private Map<String, PatientRecord> patientRecords = new HashMap<>();
    private Map<Integer, String> keyShares = new HashMap<>();
    
    //Galois Field tables:  Reed-Solomon GF(2^8) tables
private static final int GF_SIZE = 256;
private static final int PRIMITIVE_POLY = 0x11D;
private static final int[] GF_LOG = new int[GF_SIZE + 1];
private static final int[] GF_EXP = new int[GF_SIZE * 2];

static {
    // Initialize Galois Field tables
    int x = 1;
    for (int i = 0; i < GF_SIZE; i++) {
        GF_EXP[i] = x;
        GF_EXP[i + GF_SIZE] = x;
        GF_LOG[x] = i;
        x <<= 1;
        if ((x & GF_SIZE) != 0) {
            x ^= PRIMITIVE_POLY;
        }
    }
    GF_LOG[0] = GF_SIZE * 2;
}

    public static void main(String[] args) {
        new RaSeDemo().runDemo();
    }

    private void initializeGaloisField() {
        int x = 1;
        for (int i = 0; i < GF_SIZE; i++) {
            GF_EXP[i] = x;
            GF_LOG[x] = i;
            x <<= 1;
            if ((x & GF_SIZE) != 0) {
                x ^= PRIMITIVE_POLY;
            }
        }
    }

    private byte gfMul(byte a, byte b) {
        if (a == 0 || b == 0) return 0;
        return (byte) GF_EXP[(GF_LOG[a & 0xFF] + GF_LOG[b & 0xFF]) % (GF_SIZE - 1)];
    }

    // Reed-Solomon encoding
    private List<String> rsEncode(String data) {
    byte[] bytes = data.getBytes();
    int shardSize = (bytes.length + DATA_SHARDS - 1) / DATA_SHARDS;
    byte[][] shards = new byte[TOTAL_SHARDS][shardSize];
    
    // Distribute data across shards
    for (int i = 0; i < bytes.length; i++) {
        shards[i % DATA_SHARDS][i / DATA_SHARDS] = bytes[i];
    }
    
    // Calculate parity shards
    for (int i = DATA_SHARDS; i < TOTAL_SHARDS; i++) {
        for (int j = 0; j < shardSize; j++) {
            byte value = 0;
            for (int k = 0; k < DATA_SHARDS; k++) {
                value ^= gfMul(shards[k][j], (byte) GF_EXP[i * k]);
            }
            shards[i][j] = value;
        }
    }
    
    return Arrays.stream(shards)
        .map(shard -> Base64.getEncoder().encodeToString(shard))
        .collect(Collectors.toList());
}

    // Shamir's Secret Sharing
    private Map<Integer, String> shamirSplit(String secret) {
        SecureRandom random = new SecureRandom();
        int[] coeffs = new int[SHAMIR_THRESHOLD - 1];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = random.nextInt(Integer.MAX_VALUE);
        }
        
        Map<Integer, String> shares = new HashMap<>();
        for (int x = 1; x <= SHAMIR_TOTAL; x++) {
            int y = secret.hashCode();
            for (int i = 0; i < coeffs.length; i++) {
                y += coeffs[i] * Math.pow(x, i + 1);
            }
            shares.put(x, x + ":" + y);
        }
        return shares;
    }

    // Command implementations
    private void storePatient(String user, String patientId, String data) {
    try {
        String previousData = patientRecords.containsKey(patientId) ? 
            patientRecords.get(patientId).data : null;
            
        List<String> shards = rsEncode(data);
        Map<Integer, String> shares = shamirSplit("KEY-" + System.currentTimeMillis());
        
        patientRecords.put(patientId, new PatientRecord(data, shards));
        keyShares.putAll(shares);
        
        // Save to files
        savePatientRecords();
        logChange(user, patientId, "STORE", previousData, data);
        logAction(user, "STORE", patientId, "Created " + shards.size() + " shards", true);
        
        System.out.println("Stored record for " + patientId);
    } catch (Exception e) {
        logAction(user, "STORE", patientId, "Failed: " + e.getMessage(), false);
        System.err.println("Error: " + e.getMessage());
    }
}

    private String retrievePatient(String user, String patientId) {
        try {
            PatientRecord record = patientRecords.get(patientId);
            if (record == null) throw new Exception("Patient not found");
            
            if (keyShares.size() < SHAMIR_THRESHOLD) {
                throw new SecurityException("Insufficient key shares");
            }
            
            String reconstructed = new String(Base64.getDecoder().decode(record.shards.get(0))); // Simplified
            logAction(user, "RETRIEVE", patientId, "Success", true);
            return reconstructed;
        } catch (Exception e) {
            logAction(user, "RETRIEVE", patientId, "Failed: " + e.getMessage(), false);
            return "ERROR: " + e.getMessage();
        }
    }

    

    private void logAction(String user, String action, String patientId, String details, boolean success) {
        String log = String.format("[%d] [%s] [%s] [%s] %s - %s\n",
            System.currentTimeMillis(),
            user,
            action,
            success ? "SUCCESS" : "FAILED",
            patientId,
            details);
        
        try {
            Files.writeString(Path.of(AUDIT_LOG), log, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log!");
        }
    }

    private void logChange(String user, String patientId, String action, String oldData, String newData) {
    String logEntry = String.format("[%d] [%s] [%s] [%s] %s -> %s\n",
        System.currentTimeMillis(),
        user,
        patientId,
        action,
        oldData != null ? oldData : "NULL",
        newData != null ? newData : "NULL");
    
    try {
        Files.writeString(Path.of(CHANGES_LOG), logEntry, 
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
        System.err.println("Failed to write to changes log!");
    }
}

private void savePatientRecords() {
    try {
        StringBuilder records = new StringBuilder("=== Simulated Patient Records ===\n");
        patientRecords.forEach((id, record) -> 
            records.append(id).append(":").append(record.data).append("\n"));
        
        Files.writeString(Path.of(PATIENT_RECORDS), records.toString());
    } catch (IOException e) {
        System.err.println("Failed to save patient records!");
    }
}

    // Demo CLI
    public void runDemo() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== RaSe Medical Record DEMO ===");
        printHelp();
        
        while (true) {
            System.out.print("\nDEMO> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) break;
            
            String[] parts = input.split(" ", 3);
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "doctor":
                    case "admin":
                        if (parts.length < 3) throw new Exception("Invalid command");
                        String action = parts[1].toLowerCase();
                        String patientId = parts[2].split(" ")[0];
                        String data = parts.length > 2 ? parts[2].substring(patientId.length()).trim() : "";
                        
                        if (action.equals("store")) {
                            storePatient(parts[0], patientId, data);
                        } else if (action.equals("retrieve")) {
                            System.out.println(retrievePatient(parts[0], patientId));
                        } else {
                            throw new Exception("Invalid action");
                        }
                        break;
                        
                    case "attacker":
                        if (parts[1].equals("corrupt")) {
                            System.out.println("Simulating attack on " + parts[2]);
                            logAction("ATTACKER", "CORRUPT", parts[2], "2 shards corrupted", false);
                        }
                        break;
                        
                    case "audit":
                        if (parts[1].equals("viewlog")) {
                            System.out.println("\n=== AUDIT LOG ===");
                            Files.lines(Path.of(AUDIT_LOG)).forEach(System.out::println);
                        }
                        break;
                        
                    case "system":
                        if (parts[1].equals("setstate")) {
                            currentState = SystemState.valueOf(parts[2].toUpperCase());
                            System.out.println("System state: " + currentState);
                        }
                        break;
                        
                    default:
                        printHelp();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        
        scanner.close();
    }

    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  doctor store PATIENT_ID \"DATA\"");
        System.out.println("  admin retrieve PATIENT_ID");
        System.out.println("  attacker corrupt PATIENT_ID");
        System.out.println("  audit viewlog");
        System.out.println("  system setstate NORMAL|UNDER_ATTACK|RECOVERY");
        System.out.println("  exit");
    }

    private static class PatientRecord {
        String data;
        List<String> shards;
        
        public PatientRecord(String data, List<String> shards) {
            this.data = data;
            this.shards = shards;
        }
    }
}