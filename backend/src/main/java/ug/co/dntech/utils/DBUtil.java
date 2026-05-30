package ug.co.dntech.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DBUtil {
    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;

    private DBUtil() {} // Prevent instantiation

    public static MongoDatabase getDatabase() {
        if (database == null) {
            try {
                // Get URI from environment variable (useful for Render deployment)
                String uri = System.getenv("MONGO_URI");
                if (uri == null || uri.isEmpty()) {
                    // Fallback to local MongoDB
                    uri = "mongodb://localhost:27017";
                }
                
                mongoClient = MongoClients.create(uri);
                // The database name is "dntech"
                database = mongoClient.getDatabase("dntech");
                System.out.println("Connected to MongoDB successfully.");
            } catch (Exception e) {
                System.err.println("Error connecting to MongoDB: " + e.getMessage());
            }
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}
