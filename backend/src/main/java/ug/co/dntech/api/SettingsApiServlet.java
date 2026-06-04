package ug.co.dntech.api;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import ug.co.dntech.utils.ApiResponse;
import ug.co.dntech.utils.DBUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Settings endpoints:
 *   PUT /api/settings/profile   — Update admin email
 *   PUT /api/settings/password  — Change admin password
 */
@WebServlet("/api/settings/*")
public class SettingsApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        // Require an active session
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        String currentUser = (String) session.getAttribute("user");

        if ("/profile".equals(pathInfo)) {
            handleProfileUpdate(req, resp, currentUser, session);
        } else if ("/password".equals(pathInfo)) {
            handlePasswordChange(req, resp, currentUser);
        } else {
            ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleProfileUpdate(HttpServletRequest req, HttpServletResponse resp,
                                     String currentUser, HttpSession session) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            ProfileRequest profileReq = gson.fromJson(jsonBody, ProfileRequest.class);

            if (profileReq == null || profileReq.email == null || profileReq.email.isEmpty()) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Email is required");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> users = db.getCollection("admin_users");
            var result = users.updateOne(
                    Filters.eq("username", currentUser),
                    Updates.set("email", profileReq.email)
            );

            if (result.getModifiedCount() > 0) {
                session.setAttribute("email", profileReq.email);
                ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Profile updated successfully");
            } else {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
            }

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void handlePasswordChange(HttpServletRequest req, HttpServletResponse resp,
                                      String currentUser) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            PasswordRequest passReq = gson.fromJson(jsonBody, PasswordRequest.class);

            if (passReq == null || passReq.currentPassword == null || passReq.newPassword == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Current and new password are required");
                return;
            }

            if (passReq.newPassword.length() < 6) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 6 characters");
                return;
            }

            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> users = db.getCollection("admin_users");
            Document user = users.find(Filters.eq("username", currentUser)).first();

            if (user == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "User not found");
                return;
            }

            // Verify current password
            if (!BCrypt.checkpw(passReq.currentPassword, user.getString("password"))) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Current password is incorrect");
                return;
            }

            // Hash and save new password
            String newHash = BCrypt.hashpw(passReq.newPassword, BCrypt.gensalt());
            users.updateOne(
                    Filters.eq("username", currentUser),
                    Updates.set("password", newHash)
            );

            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Password changed successfully");

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private static class ProfileRequest {
        String email;
    }

    private static class PasswordRequest {
        String currentPassword;
        String newPassword;
    }
}
