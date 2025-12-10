package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<Category> getCategories(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        // Assuming we want to return global categories + user categories,
        // effectively we just fetch user categories for now.
        return categoryRepository.findByUserId(user.getId());
    }

    public Category createCategory(String name, String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Category category = Category.builder()
                .name(name)
                .user(user)
                .build();
        return categoryRepository.save(category);
    }
}
