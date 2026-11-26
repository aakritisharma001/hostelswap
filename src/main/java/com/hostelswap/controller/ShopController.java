package com.hostelswap.controller;

import com.hostelswap.model.Category;
import com.hostelswap.model.Item;
import com.hostelswap.model.User;
import com.hostelswap.repository.CategoryRepository;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.RatingRepository;
import com.hostelswap.repository.SaleRepository;
import com.hostelswap.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
public class ShopController {
    private final ItemRepository items;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final RatingRepository ratings;
    private final SaleRepository sales;

    public ShopController(ItemRepository items, CategoryRepository categories, UserRepository users, RatingRepository ratings, SaleRepository sales) {
        this.items = items; this.categories = categories; this.users = users; this.ratings = ratings; this.sales = sales;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "0") long category,
                            @RequestParam(defaultValue = "newest") String sort,
                            @RequestParam(required = false) BigDecimal minPrice,
                            @RequestParam(required = false) BigDecimal maxPrice,
                            Model m) {
        String like = (q == null || q.isBlank()) ? "%" : "%" + q.trim().toLowerCase() + "%";
        List<Item> list = new ArrayList<>(items.search(like, category));
        if (minPrice != null) list.removeIf(i -> i.getPrice().compareTo(minPrice) < 0);
        if (maxPrice != null) list.removeIf(i -> i.getPrice().compareTo(maxPrice) > 0);
        if ("price_asc".equals(sort)) list.sort(Comparator.comparing(Item::getPrice));
        else if ("price_desc".equals(sort)) list.sort(Comparator.comparing(Item::getPrice).reversed());

        m.addAttribute("items", list);
        m.addAttribute("categories", categories.findAll());
        m.addAttribute("q", q);
        m.addAttribute("selected", category);
        m.addAttribute("sort", sort);
        m.addAttribute("minPrice", minPrice);
        m.addAttribute("maxPrice", maxPrice);
        return "dashboard";
    }

    @GetMapping("/item/{id}")
    public String itemDetail(@PathVariable Long id, Model m, RedirectAttributes ra) {
        Item it = items.findById(id).filter(i -> !i.isDeleted()).orElse(null);
        if (it == null) {
            ra.addFlashAttribute("flashErr", "Item not found.");
            return "redirect:/dashboard";
        }
        Double avg = ratings.averageFor(it.getSeller().getId());
        if (avg != null) m.addAttribute("sellerRating", String.format("%.1f", avg));
        m.addAttribute("item", it);
        m.addAttribute("sellerDeals", sales.countAsSeller(it.getSeller().getId()));
        return "item";
    }

    @GetMapping("/add-item")
    public String addForm(Model m) {
        m.addAttribute("categories", categories.findAll());
        return "add-item";
    }

    @PostMapping("/add-item")
    public String add(@ModelAttribute(name = "me", binding = false) User me,
                      @RequestParam String title, @RequestParam(required = false) String description,
                      @RequestParam Long category, @RequestParam BigDecimal price, @RequestParam String condition,
                      @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) throws IOException {
        if (title.isBlank() || price.signum() <= 0) {
            ra.addFlashAttribute("flashErr", "Title and a price above 0 are required.");
            return "redirect:/add-item";
        }
        Item it = new Item();
        it.setTitle(title.trim());
        it.setDescription(description);
        it.setCategory(categories.findById(category).orElseThrow());
        it.setPrice(price);
        it.setCondition(condition);
        it.setSeller(me);
        applyImage(it, image);
        items.save(it);
        it.setImagePath("/item-image/" + it.getId());
        items.save(it);
        ra.addFlashAttribute("flash", "Listing created.");
        return "redirect:/dashboard";
    }

    @GetMapping("/edit-item/{id}")
    public String editForm(@PathVariable Long id, @ModelAttribute(name = "me", binding = false) User me,
                           Model m, RedirectAttributes ra) {
        Item it = items.findById(id).orElse(null);
        if (it == null || it.isDeleted() || !it.getSeller().getId().equals(me.getId()) || !"AVAILABLE".equals(it.getStatus())) {
            ra.addFlashAttribute("flashErr", "You can only edit your own available listings.");
            return "redirect:/profile";
        }
        m.addAttribute("item", it);
        m.addAttribute("categories", categories.findAll());
        return "edit-item";
    }

    @PostMapping("/edit-item/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute(name = "me", binding = false) User me,
                       @RequestParam String title, @RequestParam(required = false) String description,
                       @RequestParam Long category, @RequestParam BigDecimal price, @RequestParam String condition,
                       @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) throws IOException {
        Item it = items.findById(id).orElse(null);
        if (it == null || it.isDeleted() || !it.getSeller().getId().equals(me.getId()) || !"AVAILABLE".equals(it.getStatus())) {
            ra.addFlashAttribute("flashErr", "You can only edit your own available listings.");
            return "redirect:/profile";
        }
        if (title.isBlank() || price.signum() <= 0) {
            ra.addFlashAttribute("flashErr", "Title and a price above 0 are required.");
            return "redirect:/edit-item/" + id;
        }
        it.setTitle(title.trim());
        it.setDescription(description);
        it.setCategory(categories.findById(category).orElseThrow());
        it.setPrice(price);
        it.setCondition(condition);
        applyImage(it, image);
        items.save(it);
        if (image != null && !image.isEmpty()) {
            it.setImagePath("/item-image/" + it.getId());
            items.save(it);
        }
        ra.addFlashAttribute("flash", "Listing updated.");
        return "redirect:/profile";
    }

    @PostMapping("/delete-item/{id}")
    public String delete(@PathVariable Long id, @ModelAttribute(name = "me", binding = false) User me, RedirectAttributes ra) {
        Item it = items.findById(id).orElse(null);
        if (it != null && !it.isDeleted() && it.getSeller().getId().equals(me.getId()) && "AVAILABLE".equals(it.getStatus())) {
            it.setDeleted(true);
            items.save(it);
            ra.addFlashAttribute("flash", "Listing deleted.");
        } else {
            ra.addFlashAttribute("flashErr", "Only available listings you own can be deleted.");
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile")
    public String profile(@ModelAttribute(name = "me", binding = false) User me, Model m) {
        m.addAttribute("myItems", items.findBySellerIdAndDeletedFalseOrderByIdDesc(me.getId()));
        m.addAttribute("sellerDeals", sales.countAsSeller(me.getId()));
        m.addAttribute("ratings", ratings.findByRatedIdOrderByIdDesc(me.getId()));
        Double avg = ratings.averageFor(me.getId());
        if (avg != null) m.addAttribute("avgRating", String.format("%.1f", avg));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute(name = "me", binding = false) User me,
                                @RequestParam String name, @RequestParam String hostel, RedirectAttributes ra) {
        if (name.isBlank() || hostel.isBlank()) {
            ra.addFlashAttribute("flashErr", "Name and hostel cannot be empty.");
        } else {
            me.setName(name.trim());
            me.setHostel(hostel.trim());
            users.save(me);
            ra.addFlashAttribute("flash", "Profile updated.");
        }
        return "redirect:/profile";
    }

    // Keeps the uploaded photo bytes on the item, so they are stored in the database
    private void applyImage(Item it, MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return;
        it.setImageData(image.getBytes());
        it.setImageType(image.getContentType() == null ? "image/jpeg" : image.getContentType());
    }

    // Serves a listing's photo from the database
    @GetMapping("/item-image/{id}")
    public ResponseEntity<byte[]> itemImage(@PathVariable Long id) {
        return items.findById(id)
                .filter(i -> i.getImageData() != null)
                .map(i -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(i.getImageType() == null ? "image/jpeg" : i.getImageType()))
                        .body(i.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }

    // Saves an uploaded photo into the uploads folder and returns its web path
    private String saveImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String original = image.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
        String name = UUID.randomUUID() + ext;
        Path dir = Path.of("uploads").toAbsolutePath();
        Files.createDirectories(dir);
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, dir.resolve(name));
        }
        return "/uploads/" + name;
    }
}
