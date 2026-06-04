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
 * Training enquiry endpoints:
 *   GET    /api/training-enquiries  — List all training enquiries
 *   POST   /api/training-enquiries  — Submit a new training enquiry
 *   PUT    /api/training-enquiries  — Update status
 *   DELETE /api/training-enquiries  — Delete by ID (?id=...)
 */
@WebServlet("/api/training-enquiries")
public class TrainingEnquiryApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("training_enquiries");
            List<Document> enquiries = new ArrayList<>();
            collection.find().sort(new Document("createdAt", -1)).into(enquiries);

            for (Document doc : enquiries) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
            }

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Training enquiries retrieved", enquiries);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            TrainingRequest trainingReq = gson.fromJson(jsonBody, TrainingRequest.class);

            if (trainingReq == null || trainingReq.name == null || trainingReq.email == null || trainingReq.course == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and course are required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("training_enquiries");
            Document doc = new Document("name", trainingReq.name)
                    .append("email", trainingReq.email)
                    .append("phone", trainingReq.phone != null ? trainingReq.phone : "")
                    .append("course", trainingReq.course)
                    .append("message", trainingReq.message != null ? trainingReq.message : "")
                    .append("status", "new")
                    .append("createdAt", new Date());

            collection.insertOne(doc);
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_CREATED, "Training enquiry submitted successfully");

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

            MongoCollection<Document> collection = db.getCollection("training_enquiries");
            var result = collection.updateOne(
                    Filters.eq("_id", new ObjectId(update.id)),
                    Updates.set("status", update.status)
            );

            if (result.getModifiedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Status updated successfully");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Training enquiry not found");
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

            MongoCollection<Document> collection = db.getCollection("training_enquiries");
            var result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));

            if (result.getDeletedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Training enquiry deleted");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Training enquiry not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class TrainingRequest {
        String name;
        String email;
        String phone;
        String course;
        String message;
    }

    private static class StatusUpdate {
        String id;
        String status;
    }
}
