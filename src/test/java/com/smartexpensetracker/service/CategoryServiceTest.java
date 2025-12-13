package com.smartexpensetracker.service;

import com.smartexpensetracker.dao.CategoryRepository;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.Category;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void getCategories_ShouldReturnUserCategories() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<Category> result = categoryService.getCategories("testuser");
        assertNotNull(result);
        verify(categoryRepository).findByUserId(1L);
    }

    @Test
    void createCategory_ShouldSave() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category());

        Category result = categoryService.createCategory("New Cat", "testuser");
        assertNotNull(result);
        verify(categoryRepository).save(any(Category.class));
    }
}
