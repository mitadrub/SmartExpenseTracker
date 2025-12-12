package com.smartexpensetracker.api;

import com.smartexpensetracker.config.JwtAuthenticationFilter;
import com.smartexpensetracker.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    // SecurityConfig dependencies required for context loading
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser(username = "testuser")
    void getSummary_ShouldReturnSummary() throws Exception {
        AnalyticsService.AnalyticsSummary summary = AnalyticsService.AnalyticsSummary.builder()
                .total(BigDecimal.TEN)
                .build();
        when(analyticsService.getSummary(eq("testuser"), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAlerts_ShouldReturnList() throws Exception {
        when(analyticsService.getAlerts("testuser")).thenReturn(Collections.singletonList("Alert 1"));

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Alert 1"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getTrends_ShouldUseDefaultDates_WhenParamsMissing() throws Exception {
        // Mock service response
        when(analyticsService.getTrends(eq("testuser"), any(java.time.LocalDate.class), any(java.time.LocalDate.class)))
                .thenReturn(Collections.emptyMap());

        // Perform request without params
        mockMvc.perform(get("/api/v1/analytics/trends"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getForecast_ShouldReturnForecast() throws Exception {
        AnalyticsService.Forecast forecast = AnalyticsService.Forecast.builder()
                .predictedTotal(BigDecimal.valueOf(500))
                .confidence(0.85)
                .build();

        when(analyticsService.getForecast("testuser")).thenReturn(forecast);

        mockMvc.perform(get("/api/v1/forecast/next-month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedTotal").value(500))
                .andExpect(jsonPath("$.confidence").value(0.85));
    }
}
