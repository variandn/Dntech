package ug.co.dntech.api;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import ug.co.dntech.utils.ApiResponse;
import ug.co.dntech.utils.DBUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contact enquiry endpoints:
 *   POST   /api/enquiries  — Submit a new enquiry
 *   GET    /api/enquiries  — List all enquiries (admin)
 *   PUT    /api/enquiries  — Update enquiry status
 *   DELETE /api/enquiries  — Delete an enquiry by ID (?id=...)
 */
@WebServlet("/api/enquiries")
public class EnquiryApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            EnquiryRequest requestBody = gson.fromJson(jsonBody, EnquiryRequest.class);

            if (requestBody == null || requestBody.name == null || requestBody.email == null || requestBody.message == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing required fields");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("enquiries");
            Document doc = new Document("name", requestBody.name)
                    .append("email", requestBody.email)
                    .append("phone", requestBody.phone != null ? requestBody.phone : "")
                    .append("subject", requestBody.subject != null ? requestBody.subject : "General")
                    .append("message", requestBody.message)
                    .append("status", "new")
                    .append("createdAt", new Date());

            collection.insertOne(doc);
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_CREATED, "Enquiry submitted successfully");

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("enquiries");
            List<Document> enquiries = new ArrayList<>();
            collection.find().sort(new Document("createdAt", -1)).into(enquiries);

            // Convert ObjectIds to strings for JSON serialization
            for (Document doc : enquiries) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
            }

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Enquiries retrieved", enquiries);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            StatusUpdate update = gson.fromJson(jsonBody, StatusUpdate.class);

            if (update == null || update.id == null || update.status == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID and status are required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("enquiries");
            var result = collection.updateOne(
                    Filters.eq("_id", new ObjectId(update.id)),
                    Updates.set("status", update.status)
            );

            if (result.getModifiedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Status updated successfully");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Enquiry not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String id = req.getParameter("id");
            if (id == null || id.isEmpty()) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID parameter is required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("enquiries");
            var result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));

            if (result.getDeletedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Enquiry deleted");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Enquiry not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class EnquiryRequest {
        String name;
        String email;
        String phone;
        String subject;
        String message;
    }

    private static class StatusUpdate {
        String id;
        String status;
    }
}
