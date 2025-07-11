import java.util.*;
import java.security.SecureRandom;

public class RaSeLogicLayer {
    // Reed-Solomon Kernel
    static class RSKernel {
        private final Map<String, List<String>> storageMap = new HashMap<>();
        private final SecureRandom random = new SecureRandom();
        
        public Map<String, String> encodeAndStore(String dataId, String data, int dataShards, int parityShards) {
            System.out.println("\n[RS Kernel] Processing data ID: " + dataId);
            
            // Simulate RS encoding
            List<String> shards = new ArrayList<>();
            for (int i = 0; i < dataShards; i++) {
                shards.add("DATA-" + dataId + "-" + (i+1) + ":" + data.substring(i % data.length()));
            }
            for (int i = 0; i < parityShards; i++) {
                shards.add("PARITY-" + dataId + "-" + (i+1) + ":" + (data.hashCode() + i));
            }
            
            // Storage decision (simplified)
            String storageNode = "Node-" + (random.nextInt(3) + 1);
            storageMap.computeIfAbsent(storageNode, k -> new ArrayList<>()).addAll(shards);
            
            System.out.println("Stored shards across: " + storageNode);
            System.out.println("Shard locations secret: " + storageMap.keySet());
            
            return Map.of(
                "status", "encoded",
                "dataId", dataId,
                "storageNode", storageNode
            );
        }
        
        public String retrieveData(String dataId) {
            System.out.println("\n[RS Kernel] Retrieving data ID: " + dataId);
            
            // Find all shards across nodes
            List<String> allShards = new ArrayList<>();
            storageMap.values().forEach(allShards::addAll);
            
            // Filter and reconstruct (simplified)
            StringBuilder reconstructed = new StringBuilder();
            for (String shard : allShards) {
                if (shard.contains("DATA-" + dataId)) {
                    reconstructed.append(shard.split(":")[1].charAt(0));
                }
            }
            
            return reconstructed.length() > 0 ? reconstructed.toString() : "DATA_NOT_FOUND";
        }
    }

    // Database Interface
    interface DatabaseAdapter {
        void store(String key, Map<String, String> value);
        Map<String, String> retrieve(String key);
    }

    // MongoDB Implementation (NoSQL)
    static class MongoAdapter implements DatabaseAdapter {
        private final Map<String, Map<String, String>> db = new HashMap<>();
        
        public void store(String key, Map<String, String> value) {
            System.out.println("[MongoDB] Storing under key: " + key);
            db.put(key, value);
        }
        
        public Map<String, String> retrieve(String key) {
            System.out.println("[MongoDB] Retrieving key: " + key);
            return db.getOrDefault(key, Map.of("status", "not_found"));
        }
    }

    // PostgreSQL Implementation (SQL)
    static class PostgresAdapter implements DatabaseAdapter {
        private final Map<String, Map<String, String>> db = new HashMap<>();
        
        public void store(String key, Map<String, String> value) {
            System.out.println("[PostgreSQL] Storing with transaction: " + key);
            db.put(key, value);
        }
        
        public Map<String, String> retrieve(String key) {
            System.out.println("[PostgreSQL] Querying for: " + key);
            return db.getOrDefault(key, Map.of("status", "not_found"));
        }
    }

    // Main Logic Layer
    private final RSKernel rsKernel = new RSKernel();
    private final DatabaseAdapter database;
    private final Map<String, String> accessKeys = Map.of(
        "admin", "ADMIN-KEY-123",
        "doctor", "DOCTOR-KEY-456"
    );

    public RaSeLogicLayer(DatabaseAdapter database) {
        this.database = database;
    }

    public void handleRequest(String userType, String action, String dataId, String data) {
        // Authentication
        if (!accessKeys.containsKey(userType)) {
            System.out.println("ACCESS DENIED: Invalid user type");
            return;
        }
        
        System.out.println("\n=== Processing " + userType.toUpperCase() + " request ===");
        
        if ("store".equalsIgnoreCase(action)) {
            // Store workflow
            Map<String, String> result = rsKernel.encodeAndStore(dataId, data, 4, 2);
            database.store(dataId, result);
            System.out.println("Storage complete!");
        } 
        else if ("retrieve".equalsIgnoreCase(action)) {
            // Retrieve workflow
            Map<String, String> meta = database.retrieve(dataId);
            String recovered = rsKernel.retrieveData(dataId);
            System.out.println("Recovered data: " + recovered);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Database selection
        System.out.println("Select database (1=MongoDB, 2=PostgreSQL):");
        DatabaseAdapter db = scanner.nextInt() == 1 ? new MongoAdapter() : new PostgresAdapter();
        
        RaSeLogicLayer system = new RaSeLogicLayer(db);
        
        // Demo interaction
        System.out.println("\n=== RaSe System Demo ===");
        while (true) {
            System.out.println("\nEnter request (userType action dataId data) or 'exit':");
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) break;
            if (input.trim().isEmpty()) continue;
            
            String[] parts = input.split(" ");
            if (parts.length < 3) {
                System.out.println("Invalid format. Example: doctor store patient123 'Medical Data'");
                continue;
            }
            
            String data = parts.length > 3 ? parts[3] : "";
            system.handleRequest(parts[0], parts[1], parts[2], data);
        }
        
        scanner.close();
        System.out.println("System shutdown");
    }
}