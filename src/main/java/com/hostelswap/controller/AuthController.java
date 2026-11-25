package com.hostelswap.controller;

import com.hostelswap.model.User;
import com.hostelswap.repository.UserRepository;
import com.hostelswap.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.Set;

// Sign-up, login and logout, with a 6-digit email code.
// Sign-up: the form is checked, a code is emailed, and the account is created only after the code is entered.
// Login:   the password is checked, a code is emailed, and the user is logged in only after the code is entered.
// The three demo sellers have no real inbox, so they log in with their password alone.
@Controller
public class AuthController {
    private static final Set<String> NO_CODE_EMAILS =
            Set.of("ravi.demo@thapar.edu", "priya.demo@thapar.edu", "arjun.demo@thapar.edu");
    private static final long CODE_VALID_MS = 10 * 60 * 1000;   // a code lasts 10 minutes
    private static final long RESEND_WAIT_MS = 60 * 1000;       // a new code at most once a minute
    private static final int MAX_ATTEMPTS = 5;                  // wrong guesses before starting again

    // Session keys used while a code is waiting to be entered
    private static final String P_PURPOSE = "otpPurpose";       // "REGISTER" or "LOGIN"
    private static final String P_EMAIL = "otpEmail";
    private static final String P_HASH = "otpHash";
    private static final String P_EXPIRES = "otpExpires";
    private static final String P_SENT_AT = "otpSentAt";
    private static final String P_ATTEMPTS = "otpAttempts";
    private static final String P_UID = "otpUid";               // login: the user waiting to be logged in
    private static final String P_NAME = "regName";             // sign-up: the details waiting to be saved
    private static final String P_HOSTEL = "regHostel";
    private static final String P_PASSWORD = "regPassword";     // already a BCrypt hash, never the plain password

    private final UserRepository users;
    private final EmailService email;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final String adminEmail;

    public AuthController(UserRepository users, EmailService email,
                          @Value("${hostelswap.admin-email}") String adminEmail) {
        this.users = users;
        this.email = email;
        this.adminEmail = adminEmail.trim().toLowerCase();
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        return session.getAttribute("uid") != null ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String hostel,
                           @RequestParam String password, @RequestParam String confirmPassword,
                           HttpSession session, Model m) {
        String e = email.trim().toLowerCase();
        String error = null;
        if (name.isBlank() || hostel.isBlank()) error = "Please fill all fields.";
        else if (!e.endsWith("@thapar.edu")) error = "Only @thapar.edu email addresses are allowed.";
        else if (password.length() < 6) error = "Password must be at least 6 characters.";
        else if (!password.equals(confirmPassword)) error = "Passwords do not match.";
        else if (users.findByEmail(e).isPresent()) error = "This email is already registered. Try logging in.";

        if (error != null) {
            m.addAttribute("error", error);
            m.addAttribute("name", name);
            m.addAttribute("email", email);
            m.addAttribute("hostel", hostel);
            return "register";
        }

        clearPending(session);
        session.setAttribute(P_PURPOSE, "REGISTER");
        session.setAttribute(P_EMAIL, e);
        session.setAttribute(P_NAME, name.trim());
        session.setAttribute(P_HOSTEL, hostel.trim());
        session.setAttribute(P_PASSWORD, encoder.encode(password));
        try {
            sendCode(session, e);
        } catch (IllegalStateException ex) {
            clearPending(session);
            m.addAttribute("error", ex.getMessage());
            m.addAttribute("name", name);
            m.addAttribute("email", email);
            m.addAttribute("hostel", hostel);
            return "register";
        }
        return "redirect:/verify";
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model m) {
        String e = email.trim().toLowerCase();
        User u = users.findByEmail(e).orElse(null);
        if (u == null || u.isBlocked() || !encoder.matches(password, u.getPassword())) {
            m.addAttribute("error", "Invalid email or password.");
            return "login";
        }
        if (NO_CODE_EMAILS.contains(e)) {
            logIn(session, u);
            return "redirect:/dashboard";
        }

        clearPending(session);
        session.setAttribute(P_PURPOSE, "LOGIN");
        session.setAttribute(P_EMAIL, e);
        session.setAttribute(P_UID, u.getId());
        try {
            sendCode(session, e);
        } catch (IllegalStateException ex) {
            clearPending(session);
            m.addAttribute("error", ex.getMessage());
            return "login";
        }
        return "redirect:/verify";
    }

    @GetMapping("/verify")
    public String verifyForm(HttpSession session, Model m) {
        if (session.getAttribute(P_PURPOSE) == null) return "redirect:/login";
        m.addAttribute("email", session.getAttribute(P_EMAIL));
        m.addAttribute("purpose", session.getAttribute(P_PURPOSE));
        return "verify";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String code, HttpSession session, Model m, RedirectAttributes ra) {
        String purpose = (String) session.getAttribute(P_PURPOSE);
        if (purpose == null) {
            ra.addFlashAttribute("flashErr", "Your code has expired. Please start again.");
            return "redirect:/login";
        }
        long expires = (Long) session.getAttribute(P_EXPIRES);
        if (System.currentTimeMillis() > expires) {
            clearPending(session);
            ra.addFlashAttribute("flashErr", "That code has expired. Please start again.");
            return "REGISTER".equals(purpose) ? "redirect:/register" : "redirect:/login";
        }
        int attempts = (Integer) session.getAttribute(P_ATTEMPTS) + 1;
        session.setAttribute(P_ATTEMPTS, attempts);
        String entered = code == null ? "" : code.trim();
        if (!entered.matches("\\d{6}") || !encoder.matches(entered, (String) session.getAttribute(P_HASH))) {
            if (attempts >= MAX_ATTEMPTS) {
                clearPending(session);
                ra.addFlashAttribute("flashErr", "Too many wrong codes. Please start again.");
                return "REGISTER".equals(purpose) ? "redirect:/register" : "redirect:/login";
            }
            m.addAttribute("error", "That code is not correct. " + (MAX_ATTEMPTS - attempts) + " tries left.");
            m.addAttribute("email", session.getAttribute(P_EMAIL));
            m.addAttribute("purpose", purpose);
            return "verify";
        }

        String e = (String) session.getAttribute(P_EMAIL);
        if ("REGISTER".equals(purpose)) {
            if (users.findByEmail(e).isPresent()) {
                clearPending(session);
                ra.addFlashAttribute("flashErr", "This email is already registered. Try logging in.");
                return "redirect:/login";
            }
            User u = new User();
            u.setName((String) session.getAttribute(P_NAME));
            u.setEmail(e);
            u.setHostel((String) session.getAttribute(P_HOSTEL));
            u.setPassword((String) session.getAttribute(P_PASSWORD));
            u.setRole(e.equals(adminEmail) ? "ADMIN" : "STUDENT");
            users.save(u);
            clearPending(session);
            ra.addFlashAttribute("flash", "Email verified and account created. Please log in.");
            return "redirect:/login";
        }

        User u = users.findById((Long) session.getAttribute(P_UID)).orElse(null);
        clearPending(session);
        if (u == null || u.isBlocked()) {
            ra.addFlashAttribute("flashErr", "This account cannot log in.");
            return "redirect:/login";
        }
        logIn(session, u);
        return "redirect:/dashboard";
    }

    @PostMapping("/verify/resend")
    public String resend(HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute(P_PURPOSE) == null) return "redirect:/login";
        long sentAt = (Long) session.getAttribute(P_SENT_AT);
        long wait = RESEND_WAIT_MS - (System.currentTimeMillis() - sentAt);
        if (wait > 0) {
            ra.addFlashAttribute("flashErr", "Please wait " + (wait / 1000 + 1) + " seconds before asking for a new code.");
            return "redirect:/verify";
        }
        try {
            sendCode(session, (String) session.getAttribute(P_EMAIL));
            ra.addFlashAttribute("flash", "A new code has been sent.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
        }
        return "redirect:/verify";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Creates a new 6-digit code, keeps only its hash in the session, and emails it
    private void sendCode(HttpSession session, String to) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        String purpose = (String) session.getAttribute(P_PURPOSE);
        String action = "REGISTER".equals(purpose) ? "finish creating your HostelSwap account" : "log in to HostelSwap";
        email.send(to, "Your HostelSwap code: " + code,
                "Your HostelSwap verification code is " + code + ".\n\n"
                        + "Enter it to " + action + ". It expires in 10 minutes.\n\n"
                        + "If you didn't ask for this code, you can ignore this email.");
        long now = System.currentTimeMillis();
        session.setAttribute(P_HASH, encoder.encode(code));
        session.setAttribute(P_EXPIRES, now + CODE_VALID_MS);
        session.setAttribute(P_SENT_AT, now);
        session.setAttribute(P_ATTEMPTS, 0);
    }

    private void clearPending(HttpSession session) {
        for (String k : new String[]{P_PURPOSE, P_EMAIL, P_HASH, P_EXPIRES, P_SENT_AT, P_ATTEMPTS,
                P_UID, P_NAME, P_HOSTEL, P_PASSWORD}) {
            session.removeAttribute(k);
        }
    }

    private void logIn(HttpSession session, User u) {
        session.setAttribute("uid", u.getId());
        session.setMaxInactiveInterval(60 * 60);
    }
}
