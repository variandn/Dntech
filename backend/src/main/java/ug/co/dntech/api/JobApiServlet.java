package ug.co.dntech.api;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
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
 * Job listing endpoints:
 *   GET    /api/jobs  — List all job postings
 *   POST   /api/jobs  — Create a new job posting
 *   PUT    /api/jobs  — Update an existing job
 *   DELETE /api/jobs  — Delete a job by ID (?id=...)
 */
@WebServlet("/api/jobs")
public class JobApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("jobs");
            List<Document> jobs = new ArrayList<>();
            collection.find().sort(new Document("createdAt", -1)).into(jobs);

            for (Document doc : jobs) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
            }

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Jobs retrieved", jobs);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JobRequest jobReq = gson.fromJson(jsonBody, JobRequest.class);

            if (jobReq == null || jobReq.title == null || jobReq.department == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title and department are required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("jobs");
            Document doc = new Document("title", jobReq.title)
                    .append("department", jobReq.department)
                    .append("type", jobReq.type != null ? jobReq.type : "Full-Time")
                    .append("closingDate", jobReq.closingDate != null ? jobReq.closingDate : "")
                    .append("description", jobReq.description != null ? jobReq.description : "")
                    .append("requirements", jobReq.requirements != null ? jobReq.requirements : "")
                    .append("status", jobReq.status != null ? jobReq.status : "active")
                    .append("createdAt", new Date());

            collection.insertOne(doc);
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_CREATED, "Job posted successfully");

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            JobUpdate update = gson.fromJson(jsonBody, JobUpdate.class);

            if (update == null || update.id == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Job ID is required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("jobs");

            // Build update fields dynamically
            List<Bson> updates = new ArrayList<>();
            if (update.title != null) updates.add(Updates.set("title", update.title));
            if (update.department != null) updates.add(Updates.set("department", update.department));
            if (update.type != null) updates.add(Updates.set("type", update.type));
            if (update.closingDate != null) updates.add(Updates.set("closingDate", update.closingDate));
            if (update.description != null) updates.add(Updates.set("description", update.description));
            if (update.requirements != null) updates.add(Updates.set("requirements", update.requirements));
            if (update.status != null) updates.add(Updates.set("status", update.status));

            if (updates.isEmpty()) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No fields to update");
                return;
            }

            var result = collection.updateOne(
                    Filters.eq("_id", new ObjectId(update.id)),
                    Updates.combine(updates)
            );

            if (result.getModifiedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Job updated successfully");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Job not found");
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

            MongoCollection<Document> collection = db.getCollection("jobs");
            var result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));

            if (result.getDeletedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Job deleted");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Job not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class JobRequest {
        String title;
        String department;
        String type;
        String closingDate;
        String description;
        String requirements;
        String status;
    }

    private static class JobUpdate extends JobRequest {
        String id;
    }
}
