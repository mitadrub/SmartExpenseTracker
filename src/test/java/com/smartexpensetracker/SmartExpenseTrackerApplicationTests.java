package com.smartexpensetracker;

import com.smartexpensetracker.model.User;
import com.smartexpensetracker.service.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartExpenseTrackerApplicationTests {

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
        Assertions.assertNotNull(jwtService);
    }

    @Test
    void testJwtTokenGenerationAndValidation() {
        // Arrange
        User user = new User();
        user.setUsername("jwtUser");
        user.setPassword("password");

        // Act
        String token = jwtService.generateToken(user);

        // Assert
        Assertions.assertNotNull(token);
        Assertions.assertTrue(jwtService.isTokenValid(token, user));
        Assertions.assertEquals("jwtUser", jwtService.extractUsername(token));
    }

    @Test
    void testJwtTokenWithExtraClaims() {
        // Arrange
        User user = new User();
        user.setUsername("claimsUser");
        user.setPassword("password");
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "admin");
        claims.put("department", "finance");

        // Act
        String token = jwtService.generateToken(claims, user);

        // Assert
        Assertions.assertNotNull(token);
        Assertions.assertTrue(jwtService.isTokenValid(token, user));

        // functions to extract
        java.util.function.Function<io.jsonwebtoken.Claims, String> roleResolver = c -> c.get("role", String.class);
        String role = jwtService.extractClaim(token, roleResolver);
        Assertions.assertEquals("admin", role);
    }
}
