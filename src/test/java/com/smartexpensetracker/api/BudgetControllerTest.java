package com.smartexpensetracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartexpensetracker.config.JwtAuthenticationFilter;
import com.smartexpensetracker.model.Budget;
import com.smartexpensetracker.service.BudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@AutoConfigureMockMvc(addFilters = false)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    // SecurityConfig dependencies
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser")
    void getBudgets_ShouldReturnList() throws Exception {
        Budget budget = new Budget();
        budget.setId(1L);
        budget.setAmount(BigDecimal.valueOf(100));

        when(budgetService.getBudgets(eq("testuser"), any())).thenReturn(Collections.singletonList(budget));

        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createBudget_ShouldReturnCreated() throws Exception {
        Budget budget = new Budget();
        budget.setAmount(BigDecimal.valueOf(500));

        when(budgetService.createBudget(any(Budget.class), eq("testuser"), any()))
                .thenReturn(budget);

        mockMvc.perform(post("/api/v1/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(budget)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(500));
    }
}
