package ug.co.dntech.api;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet("/api/auth/login")
public class AuthApiServlet extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            LoginRequest loginReq = gson.fromJson(jsonBody, LoginRequest.class);

            if (loginReq == null || loginReq.username == null || loginReq.password == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\": false, \"message\": \"Username and password required\"}");
                return;
            }

            // In a real application, fetch user from MongoDB and use jBcrypt to verify password
            // For now, hardcode admin/admin123 for demo
            if ("admin".equals(loginReq.username) && "admin123".equals(loginReq.password)) {
                HttpSession session = req.getSession(true);
                session.setAttribute("user", "admin");

                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\": true, \"message\": \"Login successful\", \"redirect\": \"dashboard.html\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"success\": false, \"message\": \"Invalid credentials\"}");
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\": false, \"message\": \"Server error: " + e.getMessage() + "\"}");
        }
    }

    private static class LoginRequest {
        String username;
        String password;
    }
}
