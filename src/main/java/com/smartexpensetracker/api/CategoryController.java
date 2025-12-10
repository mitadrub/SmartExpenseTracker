package com.smartexpensetracker.api;

import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getCategories(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Assuming simple body with name for now
        return ResponseEntity.ok(categoryService.createCategory(category.getName(), userDetails.getUsername()));
    }
}
