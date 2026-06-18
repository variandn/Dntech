package ug.co.dntech.utils;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Standardized JSON response helper.
 * Ensures every API endpoint returns a consistent shape:
 *   { "success": true/false, "message": "...", "data": ... }
 */
public class ApiResponse {

    private static final Gson gson = new Gson();

    private boolean success;
    private String message;
    private Object data;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ApiResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // ── Getters (used by Gson serialization & IDE analysis) ─────────

    public boolean isSuccess()  { return success; }
    public String  getMessage() { return message; }
    public Object  getData()    { return data; }

    // ── Static helpers ──────────────────────────────────────────────

    public static void sendSuccess(HttpServletResponse resp, int status, String message) throws IOException {
        send(resp, status, new ApiResponse(true, message));
    }

    public static void sendSuccess(HttpServletResponse resp, int status, String message, Object data) throws IOException {
        send(resp, status, new ApiResponse(true, message, data));
    }

    public static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        send(resp, status, new ApiResponse(false, message));
    }

    private static void send(HttpServletResponse resp, int status, ApiResponse body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(gson.toJson(body));
    }
}
