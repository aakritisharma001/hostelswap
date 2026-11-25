package com.hostelswap.controller;

import com.hostelswap.model.User;
import com.hostelswap.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String adminEmail;

    public AuthController(UserRepository users, @Value("${hostelswap.admin-email}") String adminEmail) {
        this.users = users;
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
                           Model m, RedirectAttributes ra) {
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

        User u = new User();
        u.setName(name.trim());
        u.setEmail(e);
        u.setHostel(hostel.trim());
        u.setPassword(encoder.encode(password));
        u.setRole(e.equals(adminEmail) ? "ADMIN" : "STUDENT");
        users.save(u);

        ra.addFlashAttribute("flash", "Account created. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model m) {
        User u = users.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (u == null || u.isBlocked() || !encoder.matches(password, u.getPassword())) {
            m.addAttribute("error", "Invalid email or password.");
            return "login";
        }
        session.setAttribute("uid", u.getId());
        session.setMaxInactiveInterval(60 * 60);
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
