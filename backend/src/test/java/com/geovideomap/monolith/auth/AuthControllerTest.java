package com.geovideomap.monolith.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 *
 * Requires a running MySQL instance with the openclaw schema and existing tables.
 * Run with: ./mvnw test -Dspring.profiles.active=test
 *
 * Each test is @Transactional so it rolls back after execution.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AppUserRepository userRepo;
    @Autowired AppUserEventRepository eventRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String ME_URL       = "/api/auth/me";
    private static final String ME_PUT_URL   = "/api/auth/me";

    @BeforeEach
    void seed() {
        // Ensure a fresh state within the transaction
    }

    // ------------------------------------------------------------------ 1 register_success
    @Test
    @DisplayName("POST /register → 201 + UserDto with displayName")
    void register_success() throws Exception {
        RegisterDto dto = new RegisterDto("test_register@example.com", "P@ssw0rd!", "TestUser");

        mvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test_register@example.com"))
                .andExpect(jsonPath("$.displayName").value("TestUser"))
                .andExpect(jsonPath("$.provider").value("local"));
    }

    // ------------------------------------------------------------------ 2 login_bad_credentials
    @Test
    @DisplayName("POST /login with wrong password → 401")
    void login_bad_credentials() throws Exception {
        // First create a user
        AppUser u = new AppUser();
        u.setEmail("badlogin@example.com");
        u.setPasswordHash(passwordEncoder.encode("correctpass"));
        u.setProvider("local");
        u.setEnabled(1);
        u.setDisplayName("Bad Login");
        userRepo.save(u);

        LoginDto dto = new LoginDto("badlogin@example.com", "wrongpass", false);

        mvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 3 me_without_session
    @Test
    @DisplayName("GET /me without session → 401")
    void me_without_session() throws Exception {
        mvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 4 me_disabled_user
    @Test
    @DisplayName("POST /login with enabled=0 → 403")
    void login_disabled_user() throws Exception {
        AppUser u = new AppUser();
        u.setEmail("disabled@example.com");
        u.setPasswordHash(passwordEncoder.encode("pass"));
        u.setProvider("local");
        u.setEnabled(0); // DISABLED
        u.setDisplayName("Disabled");
        userRepo.save(u);

        LoginDto dto = new LoginDto("disabled@example.com", "pass", false);

        mvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ 5 update_profile_success
    @Test
    @DisplayName("PUT /me with valid session → 200 + updated displayName, event PROFILE_UPDATE")
    void update_profile_success() throws Exception {
        // Create and register user
        RegisterDto reg = new RegisterDto("profile_update@example.com", "P@ssw0rd!", "OldName");
        mvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login to get session cookie
        LoginDto login = new LoginDto("profile_update@example.com", "P@ssw0rd!", false);
        var loginResult = mvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract session cookie
        var sessionCookie = loginResult.getResponse().getCookie("JSESSIONID");

        // PUT /me
        UpdateProfileDto upd = new UpdateProfileDto("NewName", null);
        var req = put(ME_PUT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(upd));
        if (sessionCookie != null) req = req.cookie(sessionCookie);

        mvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));

        // Verify event
        boolean hasEvent = eventRepo.findAll().stream()
                .anyMatch(e -> "PROFILE_UPDATE".equals(e.getEventType()));
        org.junit.jupiter.api.Assertions.assertTrue(hasEvent, "PROFILE_UPDATE event should be recorded");
    }
}
