import java.util.*;
import java.security.SecureRandom;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;

/**
 * RaSe (Ransom Sentinel) Medical Record DEMO System
 * 
 * + Production: Uses complex math (Galois Fields, polynomial interpolation)  
 * - Demo: Uses string operations to visualize the flow
 * Simulates ransomware-resilient healthcare records with:
 * - Reed-Solomon sharding simulation
 * - Comprehensive audit logging
 * - Patient records stored in 'patient_records.sim' file
 * - Attack/failure simulation modes
 * 
 * DEMO COMMANDS:
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
 * 5. View All Patient IDs:
 *    audit listpatients
 * 
 * 6. Simulate System Failure:
 *    system setstate UNDER_ATTACK
 *    system setstate NORMAL
 * 
 * 7. Exit:
 *    exit
 */

public class RaSeDemo {
    // File paths
    private static final String AUDIT_LOG = "audit.log";
    private static final String PATIENT_RECORDS = "patient_records.sim";
    
    // Formatting
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // System state
    private enum SystemState { NORMAL, UNDER_ATTACK, RECOVERY }
    private SystemState currentState = SystemState.NORMAL;
    private int errorCounter = 0;
    
    // Patient records simulation
    private Map<String, String> patientRecords = new HashMap<>();
    
    /**
     * Initialize system files
     */
    public RaSeDemo() {
        initializeFiles();
        loadPatientRecords();
    }
    
    private void initializeFiles() {
        try {
            // Create audit log header
            Files.writeString(Path.of(AUDIT_LOG), 
                "=== RaSe DEMO Audit Log ===\n" +
                "Format: [Timestamp] [User] [Action] [Status] [Details]\n" +
                "=================================\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            // Create empty patient records file if doesn't exist
            if (!Files.exists(Path.of(PATIENT_RECORDS))) {
                Files.writeString(Path.of(PATIENT_RECORDS), 
                    "=== Simulated Patient Records ===\n",
                    StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize files: " + e.getMessage());
        }
    }
    
    /**
     * Load existing patient records from simulation file
     */
    private void loadPatientRecords() {
        try {
            List<String> lines = Files.readAllLines(Path.of(PATIENT_RECORDS));
            for (String line : lines) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    patientRecords.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load patient records");
        }
    }
    
    /**
     * Log an action to the audit trail
     */
    private void logAction(String user, String action, String patientId, String details, boolean success) {
        String logEntry = String.format("[%s] [%s] [%s] [%s] %s - %s\n",
            dateFormat.format(new Date()),
            user,
            action,
            success ? "SUCCESS" : "FAILED",
            patientId,
            details);
            
        try {
            Files.writeString(Path.of(AUDIT_LOG), logEntry, StandardOpenOption.APPEND);
            System.out.println("LOG: " + logEntry.trim());
        } catch (IOException e) {
            System.err.println("Failed to write to audit log!");
        }
    }
    
    /**
     * Simulate Reed-Solomon storage
     */
    private void simulateSharding(String patientId, String data) {
        // In a real system, this would split data into shards with parity
        System.out.println("DEMO: Splitting data for " + patientId + " into 4 data + 2 parity shards");
        logAction("SYSTEM", "SHARDING", patientId, "Created 6 shards (4+2)", true);
        
        // Simulate storing shards in different locations
        String[] locations = {"Node1", "Node2", "Node3"};
        System.out.println("DEMO: Storing shards across " + Arrays.toString(locations));
    }
    
    /**
     * Store patient data (demo version)
     */
    public void storePatientData(String user, String patientId, String data) {
        // Check for attack state failures
        if (currentState == SystemState.UNDER_ATTACK && ++errorCounter % 3 == 0) {
            System.out.println("DEMO ERROR: Storage system unavailable (simulated attack)");
            logAction(user, "STORE", patientId, "Failed - system under attack", false);
            return;
        }
        
        // Store in memory
        patientRecords.put(patientId, data);
        
        // Simulate RS encoding
        simulateSharding(patientId, data);
        
        // Update patient records file
        try {
            Files.writeString(Path.of(PATIENT_RECORDS),
                patientId + ": " + data + "\n",
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Warning: Could not update patient records file");
        }
        
        System.out.println("DEMO: Stored record for " + patientId);
        logAction(user, "STORE", patientId, "Data length: " + data.length(), true);
    }
    
    /**
     * Retrieve patient data (demo version)
     */
    public String retrievePatientData(String user, String patientId) {
        // Check for attack state failures
        if (currentState == SystemState.UNDER_ATTACK && ++errorCounter % 4 == 0) {
            System.out.println("DEMO ERROR: Retrieval failed (simulated attack)");
            logAction(user, "RETRIEVE", patientId, "Failed - system under attack", false);
            return null;
        }
        
        String data = patientRecords.get(patientId);
        if (data == null) {
            logAction(user, "RETRIEVE", patientId, "Patient not found", false);
            return "PATIENT_NOT_FOUND";
        }
        
        // Simulate RS recovery
        System.out.println("DEMO: Reconstructing data from shards for " + patientId);
        logAction(user, "RETRIEVE", patientId, "Recovered " + data.length() + " bytes", true);
        
        return data;
    }
    
    /**
     * Simulate ransomware attack
     */
    public void simulateAttack(String patientId) {
        System.out.println("DEMO: Simulating ransomware attack on " + patientId);
        
        // Corrupt 2 out of 6 shards (below recovery threshold)
        System.out.println("DEMO: Corrupting 2 shards (still recoverable)");
        logAction("ATTACKER", "CORRUPT", patientId, "Corrupted 2/6 shards", false);
        
        // Demonstrate recovery
        System.out.println("DEMO: System detecting corruption...");
        System.out.println("DEMO: Recovering from remaining good shards");
        logAction("SYSTEM", "RECOVER", patientId, "Recovered from attack", true);
    }
    
    /**
     * Display audit log
     */
    public void viewAuditLog() {
        System.out.println("\n=== AUDIT LOG ===");
        try {
            Files.lines(Path.of(AUDIT_LOG)).forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Failed to read audit log");
        }
    }
    
    /**
     * List all patient IDs
     */
    public void listPatients() {
        System.out.println("\n=== PATIENT IDS ===");
        patientRecords.keySet().forEach(System.out::println);
    }
    
    /**
     * Main demo loop
     */
    public void runDemo() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== RaSe Medical Record DEMO ===");
        printHelp();
        
        while (true) {
            System.out.print("\nDEMO> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("exit")) break;
            if (input.isEmpty()) continue;
            
            String[] parts = input.split(" ", 3);
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "doctor":
                    case "admin":
                        if (parts.length < 3) throw new IllegalArgumentException("Invalid command");
                        String action = parts[1].toLowerCase();
                        String patientId = parts[2].split(" ")[0];
                        String data = parts.length > 2 ? parts[2].substring(patientId.length()).trim() : "";
                        
                        if (action.equals("store")) {
                            storePatientData(parts[0], patientId, data);
                        } else if (action.equals("retrieve")) {
                            String record = retrievePatientData(parts[0], patientId);
                            System.out.println("RECORD: " + record);
                        } else {
                            throw new IllegalArgumentException("Invalid action");
                        }
                        break;
                        
                    case "attacker":
                        if (!parts[1].equals("corrupt") || parts.length < 3) {
                            throw new IllegalArgumentException("Use: attacker corrupt PATIENT_ID");
                        }
                        simulateAttack(parts[2]);
                        break;
                        
                    case "audit":
                        if (parts[1].equals("viewlog")) {
                            viewAuditLog();
                        } else if (parts[1].equals("listpatients")) {
                            listPatients();
                        } else {
                            throw new IllegalArgumentException("Invalid audit command");
                        }
                        break;
                        
                    case "system":
                        if (parts[1].equals("setstate") && parts.length == 3) {
                            currentState = SystemState.valueOf(parts[2].toUpperCase());
                            System.out.println("DEMO: System state set to " + currentState);
                            logAction("SYSTEM", "STATE_CHANGE", "", "New state: " + currentState, true);
                        } else {
                            throw new IllegalArgumentException("Use: system setstate NORMAL|UNDER_ATTACK|RECOVERY");
                        }
                        break;
                        
                    case "help":
                        printHelp();
                        break;
                        
                    default:
                        throw new IllegalArgumentException("Unknown command");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                printHelp();
            }
        }
        
        scanner.close();
        System.out.println("DEMO: Shutting down. Audit log saved to " + AUDIT_LOG);
        System.out.println("DEMO: Patient records saved to " + PATIENT_RECORDS);
    }
    
    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  doctor store PATIENT_ID \"DATA\"");
        System.out.println("  admin retrieve PATIENT_ID");
        System.out.println("  attacker corrupt PATIENT_ID");
        System.out.println("  audit viewlog");
        System.out.println("  audit listpatients");
        System.out.println("  system setstate NORMAL|UNDER_ATTACK|RECOVERY");
        System.out.println("  help");
        System.out.println("  exit");
    }
    
    public static void main(String[] args) {
        new RaSeDemo().runDemo();
    }
}