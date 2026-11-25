package com.hostelswap.config;

import com.hostelswap.model.Category;
import com.hostelswap.model.Item;
import com.hostelswap.model.User;
import com.hostelswap.repository.CategoryRepository;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Runs every time the app starts.
// 1. Makes sure the admin account exists (needed on Render, where the database is wiped on restart).
//    The admin email comes from hostelswap.admin-email, and the password from hostelswap.admin-password
//    (on Render: environment variables HOSTELSWAP_ADMINEMAIL and HOSTELSWAP_ADMINPASSWORD).
//    If no admin password is set, no admin account is created here, and the admin registers normally.
// 2. Adds a few sample sellers and listings the first time, so the shop is not empty.
// Demo logins: ravi.demo@thapar.edu, priya.demo@thapar.edu, arjun.demo@thapar.edu (password: demo123)
@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final String MARKER_EMAIL = "ravi.demo@thapar.edu";
    private final UserRepository users;
    private final ItemRepository items;
    private final CategoryRepository categories;
    private final String adminEmail;
    private final String adminPassword;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DemoDataSeeder(UserRepository users, ItemRepository items, CategoryRepository categories,
                          @Value("${hostelswap.admin-email:}") String adminEmail,
                          @Value("${hostelswap.admin-password:}") String adminPassword) {
        this.users = users;
        this.items = items;
        this.categories = categories;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    public void run(String... args) {
        ensureAdmin();

        if (users.findByEmail(MARKER_EMAIL).isPresent()) return;

        User ravi = seller("Ravi Kumar", "ravi.demo@thapar.edu", "Hostel A");
        User priya = seller("Priya Sharma", "priya.demo@thapar.edu", "Hostel B");
        User arjun = seller("Arjun Singh", "arjun.demo@thapar.edu", "Hostel C");

        add("Engineering Mathematics Vol. 1", "Clean copy with minimal highlighting. Covers calculus and linear algebra.",
                "Books", "250", "GOOD", "/demo/maths.svg", ravi);
        add("Casio fx-991ES Calculator", "Works perfectly and batteries are included. Used for one semester.",
                "Electronics", "600", "USED", "/demo/calc.svg", priya);
        add("26-inch Hybrid Bicycle", "Gears work well and tyres were replaced last month. Pick-up from Hostel C.",
                "Cycles", "1500", "GOOD", "/demo/bike.svg", arjun);
        add("Wooden Study Table", "Sturdy, with one drawer. Small scratches on the top.",
                "Furniture", "800", "USED", "/demo/table.svg", ravi);
        add("Lab Coat and Safety Goggles", "Unused, size M. Bought for a lab that I no longer need.",
                "Lab Equipment", "300", "NEW", "/demo/lab.svg", priya);
        add("Data Structures Handwritten Notes", "Complete semester notes, neat and indexed by topic.",
                "Stationery", "120", "GOOD", "/demo/notes.svg", arjun);
        add("Mini Fridge 45L", "Cools well and runs quietly. Good for room use.",
                "Other", "3500", "GOOD", "/demo/fridge.svg", ravi);
        add("Wireless Earphones", "Battery lasts about 5 hours. Comes with the charging case.",
                "Electronics", "900", "USED", "/demo/earphones.svg", priya);
    }

    // Creates the admin account if it is missing, or gives the ADMIN role to an existing account with that email.
    private void ensureAdmin() {
        if (adminEmail.isEmpty()) return;
        User existing = users.findByEmail(adminEmail).orElse(null);
        if (existing != null) {
            if (!existing.isAdmin()) {
                existing.setRole("ADMIN");
                users.save(existing);
            }
            return;
        }
        if (adminPassword.isBlank()) return;
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail(adminEmail);
        admin.setHostel("Admin");
        admin.setPassword(encoder.encode(adminPassword));
        admin.setRole("ADMIN");
        users.save(admin);
    }

    private User seller(String name, String email, String hostel) {
        return users.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setHostel(hostel);
            u.setPassword(encoder.encode("demo123"));
            u.setRole("STUDENT");
            return users.save(u);
        });
    }

    private Category category(String name) {
        return categories.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Category c = new Category();
                    c.setName(name);
                    return categories.save(c);
                });
    }

    private void add(String title, String description, String categoryName, String price,
                     String condition, String imagePath, User seller) {
        Item it = new Item();
        it.setTitle(title);
        it.setDescription(description);
        it.setCategory(category(categoryName));
        it.setPrice(new BigDecimal(price));
        it.setCondition(condition);
        it.setImagePath(imagePath);
        it.setSeller(seller);
        items.save(it);
    }
}
