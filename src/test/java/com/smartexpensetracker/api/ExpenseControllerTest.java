package com.smartexpensetracker.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartexpensetracker.config.JwtAuthenticationFilter;
import com.smartexpensetracker.model.Expense;
import com.smartexpensetracker.service.ExpenseService;
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
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    // SecurityConfig dependencies
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private AuthenticationProvider authenticationProvider;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getExpenses_ShouldReturnList() throws Exception {
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Test");

        when(expenseService.getExpenses(eq("testuser"), any(), any(), any()))
                .thenReturn(Collections.singletonList(expense));

        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Test"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createExpense_ShouldReturnCreated() throws Exception {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.TEN);
        expense.setDate(LocalDate.now());
        expense.setDescription("New");

        when(expenseService.createExpense(any(Expense.class), eq("testuser"), any()))
                .thenReturn(expense);

        mockMvc.perform(post("/api/v1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getExpense_ShouldReturnExpense() throws Exception {
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setDescription("Details");

        when(expenseService.getExpense(eq(1L), eq("testuser"))).thenReturn(expense);

        mockMvc.perform(get("/api/v1/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Details"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateExpense_ShouldReturnUpdated_AndExtractCategoryId() throws Exception {
        Expense expense = new Expense();
        expense.setId(1L);
        expense.setAmount(BigDecimal.TEN);
        expense.setDescription("Updated");
        // category object to test extraction logic
        com.smartexpensetracker.model.Category cat = new com.smartexpensetracker.model.Category();
        cat.setId(5L);
        expense.setCategory(cat);

        when(expenseService.updateExpense(eq(1L), any(Expense.class), eq("testuser"), eq(5L)))
                .thenReturn(expense);

        mockMvc.perform(put("/api/v1/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteExpense_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/1"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(expenseService).deleteExpense(eq(1L), eq("testuser"));
    }
}
