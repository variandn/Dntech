package ug.co.dntech.api;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
 * Authentication endpoints:
 *   POST /api/auth/login   — Authenticate admin user
 *   POST /api/auth/logout  — Invalidate session
 *   GET  /api/auth/session  — Check if current session is valid
 */
@WebServlet("/api/auth/*")
public class AuthApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
        } else {
            ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if ("/session".equals(pathInfo)) {
            handleSessionCheck(req, resp);
        } else {
            ApiResponse.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            LoginRequest loginReq = gson.fromJson(jsonBody, LoginRequest.class);

            if (loginReq == null || loginReq.username == null || loginReq.password == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Username and password required");
                return;
            }

            // Look up user in MongoDB
            MongoDatabase db = DBUtil.getDatabase();
            if (db == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection failed");
                return;
            }

            MongoCollection<Document> users = db.getCollection("admin_users");
            Document user = users.find(new Document("username", loginReq.username)).first();

            if (user == null) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            // Verify password with bcrypt
            String storedHash = user.getString("password");
            if (!BCrypt.checkpw(loginReq.password, storedHash)) {
                ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            // Create session
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user.getString("username"));
            session.setAttribute("email", user.getString("email"));
            session.setMaxInactiveInterval(60 * 60); // 1 hour

            Document responseData = new Document("redirect", "dashboard.html")
                    .append("username", user.getString("username"));
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Login successful", responseData);

        } catch (Exception e) {
            ApiResponse.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error: " + e.getMessage());
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Logged out successfully");
    }

    private void handleSessionCheck(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            Document data = new Document("username", (String) session.getAttribute("user"))
                    .append("email", (String) session.getAttribute("email"));
            ApiResponse.sendSuccess(resp, HttpServletResponse.SC_OK, "Session active", data);
        } else {
            ApiResponse.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
        }
    }

    private static class LoginRequest {
        String username;
        String password;
    }
}
