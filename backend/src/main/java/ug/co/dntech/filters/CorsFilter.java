package ug.co.dntech.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
            
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Dynamically echo requesting origin if present to allow credentials sharing
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");

        // If it's a preflight request, respond with 200 OK immediately
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Wrap response to intercept and modify Set-Cookie headers for SameSite=None and Secure
        HttpServletResponseWrapper wrappedResponse = new HttpServletResponseWrapper(response) {
            @Override
            public void addHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    if (!value.toLowerCase().contains("samesite")) {
                        value = value + "; SameSite=None; Secure";
                    }
                }
                super.addHeader(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                if ("Set-Cookie".equalsIgnoreCase(name) && value != null) {
                    if (!value.toLowerCase().contains("samesite")) {
                        value = value + "; SameSite=None; Secure";
                    }
                }
                super.setHeader(name, value);
            }
        };

        chain.doFilter(req, wrappedResponse);
    }
}
