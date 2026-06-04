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
 * Job application endpoints:
 *   GET    /api/applications  — List all applications
 *   POST   /api/applications  — Submit a new application
 *   PUT    /api/applications  — Update application status
 *   DELETE /api/applications  — Delete an application (?id=...)
 */
@WebServlet("/api/applications")
public class ApplicationApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("applications");
            List<Document> applications = new ArrayList<>();
            collection.find().sort(new Document("createdAt", -1)).into(applications);

            for (Document doc : applications) {
                doc.put("_id", doc.getObjectId("_id").toHexString());
            }

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Applications retrieved", applications);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            ApplicationRequest appReq = gson.fromJson(jsonBody, ApplicationRequest.class);

            if (appReq == null || appReq.name == null || appReq.email == null || appReq.position == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Name, email, and position are required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> collection = db.getCollection("applications");
            Document doc = new Document("name", appReq.name)
                    .append("email", appReq.email)
                    .append("phone", appReq.phone != null ? appReq.phone : "")
                    .append("position", appReq.position)
                    .append("coverLetter", appReq.coverLetter != null ? appReq.coverLetter : "")
                    .append("cvUrl", appReq.cvUrl != null ? appReq.cvUrl : "")
                    .append("status", "new")
                    .append("createdAt", new Date());

            collection.insertOne(doc);
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_CREATED, "Application submitted successfully");

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

            MongoCollection<Document> collection = db.getCollection("applications");
            var result = collection.updateOne(
                    Filters.eq("_id", new ObjectId(update.id)),
                    Updates.set("status", update.status)
            );

            if (result.getModifiedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Application status updated");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Application not found");
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

            MongoCollection<Document> collection = db.getCollection("applications");
            var result = collection.deleteOne(Filters.eq("_id", new ObjectId(id)));

            if (result.getDeletedCount() > 0) {
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Application deleted");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Application not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class ApplicationRequest {
        String name;
        String email;
        String phone;
        String position;
        String coverLetter;
        String cvUrl;
    }

    private static class StatusUpdate {
        String id;
        String status;
    }
}
