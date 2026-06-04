package ug.co.dntech.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

/**
 * MongoDB connection singleton.
 * Reads the MONGO_URI environment variable; falls back to localhost.
 * On first connection, ensures a default admin user exists in the
 * "admin_users" collection.
 */
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
                    // Fallback to provided Atlas URI
                    uri = "mongodb+srv://variandn04_db_user:<db_password>@dntech.3zkd8mo.mongodb.net/?appName=Dntech";
                }

                mongoClient = MongoClients.create(uri);
                // The database name is "dntech"
                database = mongoClient.getDatabase("dntech");
                System.out.println("Connected to MongoDB successfully.");

                // Seed default admin user if none exists
                initAdminUser();
            } catch (Exception e) {
                System.err.println("Error connecting to MongoDB: " + e.getMessage());
            }
        }
        return database;
    }

    /**
     * Creates a default admin user (admin / admin123) if the admin_users
     * collection is empty. The password is stored as a bcrypt hash.
     */
    private static void initAdminUser() {
        try {
            MongoCollection<Document> users = database.getCollection("admin_users");
            if (users.countDocuments() == 0) {
                String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
                Document admin = new Document("username", "admin")
                        .append("email", "admin@dntech.co.ug")
                        .append("password", hashedPassword)
                        .append("role", "superadmin");
                users.insertOne(admin);
                System.out.println("Default admin user created (admin / admin123).");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not seed admin user: " + e.getMessage());
        }
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}
