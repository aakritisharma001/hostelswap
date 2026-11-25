package com.hostelswap.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Sends anyone who is not logged in to the login page.
// The email-code pages (/verify and /verify/resend) stay open, because the user is not logged in yet when using them.
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.equals("/verify") || path.equals("/verify/resend")) return true;
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("uid") != null) return true;
        resp.sendRedirect(req.getContextPath() + "/login");
        return false;
    }
}
