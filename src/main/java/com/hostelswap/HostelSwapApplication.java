package com.hostelswap;

import com.hostelswap.model.Category;
import com.hostelswap.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HostelSwapApplication {

    public static void main(String[] args) {
        SpringApplication.run(HostelSwapApplication.class, args);
    }

    @Bean
    CommandLineRunner seedCategories(CategoryRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                for (String n : new String[]{"Books", "Electronics", "Cycles", "Furniture", "Lab Equipment", "Stationery", "Other"}) {
                    Category c = new Category();
                    c.setName(n);
                    repo.save(c);
                }
            }
        };
    }
}
