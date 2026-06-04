package ug.co.dntech.api;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import ug.co.dntech.utils.ApiResponse;
import ug.co.dntech.utils.DBUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Dashboard statistics endpoint:
 *   GET /api/dashboard/stats — Returns aggregate counts for the admin dashboard
 */
@WebServlet("/api/dashboard/stats")
public class DashboardApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            // Count new enquiries (status = "new")
            MongoCollection<Document> enquiries = db.getCollection("enquiries");
            long newEnquiries = enquiries.countDocuments(Filters.eq("status", "new"));

            // Count total job applications
            MongoCollection<Document> applications = db.getCollection("applications");
            long jobApplications = applications.countDocuments();

            // Count training signups (all training enquiries)
            MongoCollection<Document> trainingEnquiries = db.getCollection("training_enquiries");
            long trainingSignups = trainingEnquiries.countDocuments();

            // Count active job listings
            MongoCollection<Document> jobs = db.getCollection("jobs");
            long activeJobs = jobs.countDocuments(Filters.eq("status", "active"));

            // Count totals for extra context
            long totalEnquiries = enquiries.countDocuments();
            long newApplications = applications.countDocuments(Filters.eq("status", "new"));
            long newTrainingEnquiries = trainingEnquiries.countDocuments(Filters.eq("status", "new"));

            Document stats = new Document("newEnquiries", newEnquiries)
                    .append("totalEnquiries", totalEnquiries)
                    .append("jobApplications", jobApplications)
                    .append("newApplications", newApplications)
                    .append("trainingSignups", trainingSignups)
                    .append("newTrainingEnquiries", newTrainingEnquiries)
                    .append("activeJobs", activeJobs);

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Dashboard stats retrieved", stats);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }
}
