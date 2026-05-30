package ug.co.dntech.api;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import ug.co.dntech.utils.DBUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.stream.Collectors;

@WebServlet("/api/enquiries")
public class EnquiryApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            // Read JSON from request body
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            EnquiryRequest requestBody = gson.fromJson(jsonBody, EnquiryRequest.class);

            if (requestBody == null || requestBody.name == null || requestBody.email == null || requestBody.message == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\": false, \"message\": \"Missing required fields\"}");
                return;
            }

            // Save to MongoDB
            MongoDatabase db = DBUtil.getDatabase();
            if (db != null) {
                MongoCollection<Document> collection = db.getCollection("enquiries");
                Document doc = new Document("name", requestBody.name)
                        .append("email", requestBody.email)
                        .append("subject", requestBody.subject != null ? requestBody.subject : "General")
                        .append("message", requestBody.message)
                        .append("status", "new")
                        .append("createdAt", new Date());

                collection.insertOne(doc);
            } else {
                System.err.println("Warning: Database connection failed. Request not saved.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\": false, \"message\": \"Database connection failed\"}");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("{\"success\": true, \"message\": \"Enquiry submitted successfully\"}");

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\": false, \"message\": \"Server error: " + e.getMessage() + "\"}");
        }
    }

    // Handle GET for the admin dashboard
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db != null) {
                MongoCollection<Document> collection = db.getCollection("enquiries");
                java.util.List<Document> enquiries = new java.util.ArrayList<>();
                collection.find().sort(new Document("createdAt", -1)).into(enquiries);

                // Convert ObjectIds to strings to avoid Gson serialization issues with BSON types
                for (Document doc : enquiries) {
                    doc.put("_id", doc.getObjectId("_id").toHexString());
                }

                String jsonResponse = gson.toJson(enquiries);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(jsonResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("[]");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\": false, \"message\": \"Server error: " + e.getMessage() + "\"}");
        }
    }

    private static class EnquiryRequest {
        String name;
        String email;
        String subject;
        String message;
    }
}
