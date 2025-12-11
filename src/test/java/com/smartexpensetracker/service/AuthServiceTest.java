package com.smartexpensetracker.service;

import com.smartexpensetracker.api.dto.AuthenticationRequest;
import com.smartexpensetracker.api.dto.AuthenticationResponse;
import com.smartexpensetracker.api.dto.RegisterRequest;
import com.smartexpensetracker.dao.UserRepository;
import com.smartexpensetracker.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_ShouldSaveUserAndReturnToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user");
        req.setPassword("pass");
        req.setFirstname("First");
        req.setLastname("Last");

        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(repository.save(any(User.class))).thenReturn(new User()); // return value doesn't matter much for save
        when(jwtService.generateToken(any(User.class))).thenReturn("mockToken");

        AuthenticationResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        verify(repository).save(any(User.class));
    }

    @Test
    void authenticate_ShouldAuthAndReturnToken() {
        AuthenticationRequest req = new AuthenticationRequest();
        req.setUsername("user");
        req.setPassword("pass");

        User user = new User();
        user.setUsername("user");

        when(repository.findByUsername("user")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mockAuthToken");

        AuthenticationResponse response = authService.authenticate(req);

        assertNotNull(response);
        assertEquals("mockAuthToken", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
