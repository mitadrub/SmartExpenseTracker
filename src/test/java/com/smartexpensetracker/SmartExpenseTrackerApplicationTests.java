package com.smartexpensetracker;

import com.smartexpensetracker.model.User;
import com.smartexpensetracker.service.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
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

                java.util.function.Function<io.jsonwebtoken.Claims, String> roleResolver = c -> c.get("role",
                                String.class);
                String role = jwtService.extractClaim(token, roleResolver);
                Assertions.assertEquals("admin", role);
        }

        @Test
        void testDtoCoverage() {
                // RegisterRequest
                com.smartexpensetracker.api.dto.RegisterRequest req1 = new com.smartexpensetracker.api.dto.RegisterRequest();
                req1.setFirstname("F");
                req1.setLastname("L");
                req1.setUsername("U");
                req1.setPassword("P");

                Assertions.assertEquals("F", req1.getFirstname());
                Assertions.assertEquals("L", req1.getLastname());
                Assertions.assertEquals("U", req1.getUsername());
                Assertions.assertEquals("P", req1.getPassword());
                Assertions.assertNotNull(req1.toString());

                com.smartexpensetracker.api.dto.RegisterRequest req2 = com.smartexpensetracker.api.dto.RegisterRequest
                                .builder()
                                .firstname("F").lastname("L").username("U").password("P").build();
                Assertions.assertEquals(req1, req2);
                Assertions.assertEquals(req1.hashCode(), req2.hashCode());

                com.smartexpensetracker.api.dto.RegisterRequest req3 = new com.smartexpensetracker.api.dto.RegisterRequest(
                                "F",
                                "L", "U", "P");
                Assertions.assertEquals(req1, req3);

                Assertions.assertNotEquals(req1, new Object());
                Assertions.assertNotEquals(req1, new com.smartexpensetracker.api.dto.RegisterRequest());

                // AuthenticationRequest
                com.smartexpensetracker.api.dto.AuthenticationRequest auth1 = new com.smartexpensetracker.api.dto.AuthenticationRequest();
                auth1.setUsername("U");
                auth1.setPassword("P");
                Assertions.assertEquals("U", auth1.getUsername());
                Assertions.assertEquals("P", auth1.getPassword());
                Assertions.assertNotNull(auth1.toString());

                com.smartexpensetracker.api.dto.AuthenticationRequest auth2 = com.smartexpensetracker.api.dto.AuthenticationRequest
                                .builder()
                                .username("U").password("P").build();
                Assertions.assertEquals(auth1, auth2);
                Assertions.assertEquals(auth1.hashCode(), auth2.hashCode());

                com.smartexpensetracker.api.dto.AuthenticationRequest auth3 = new com.smartexpensetracker.api.dto.AuthenticationRequest(
                                "U", "P");
                Assertions.assertEquals(auth1, auth3);

                Assertions.assertNotEquals(auth1, new Object());
                Assertions.assertNotEquals(auth1, new com.smartexpensetracker.api.dto.AuthenticationRequest());

                // AuthenticationResponse
                com.smartexpensetracker.api.dto.AuthenticationResponse resp1 = new com.smartexpensetracker.api.dto.AuthenticationResponse();
                resp1.setToken("T");
                Assertions.assertEquals("T", resp1.getToken());
                Assertions.assertNotNull(resp1.toString());

                com.smartexpensetracker.api.dto.AuthenticationResponse resp2 = com.smartexpensetracker.api.dto.AuthenticationResponse
                                .builder()
                                .token("T").build();
                Assertions.assertEquals(resp1, resp2);
                Assertions.assertEquals(resp1.hashCode(), resp2.hashCode());

                com.smartexpensetracker.api.dto.AuthenticationResponse resp3 = new com.smartexpensetracker.api.dto.AuthenticationResponse(
                                "T");
                Assertions.assertEquals(resp1, resp3);

                Assertions.assertNotEquals(resp1, new Object());
                Assertions.assertNotEquals(resp1, new com.smartexpensetracker.api.dto.AuthenticationResponse());
        }
}
