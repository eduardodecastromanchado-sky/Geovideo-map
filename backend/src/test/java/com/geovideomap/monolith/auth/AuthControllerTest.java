package com.geovideomap.monolith.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests para AuthController.
 *
 * @WebMvcTest carga solo la capa web.
 * TestSecurityConfig reemplaza la cadena de filtros real:
 *   - Sin OAuth2 → sin redirect 302
 *   - Sin CSRF   → POST/PUT no necesitan token
 *   - Sin DB     → sin Hikari / MySQL
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // Dependencia del controlador
    @MockBean AuthService authService;

    // Beans que SecurityConfig real necesita; al importar TestSecurityConfig
    // estos no se usan, pero evitan errores de bean no encontrado si alguna
    // auto-configuración de Boot los busca.
    @MockBean UserDetailsServiceImpl userDetailsService;
    @MockBean GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL    = "/api/auth/login";
    private static final String ME_URL       = "/api/auth/me";
    private static final String ME_PUT_URL   = "/api/auth/me";

    // ------------------------------------------------------------------ helpers
    private AppUser buildUser(Long id, String email, String displayName, int enabled) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setProvider("local");
        u.setEnabled(enabled);
        return u;
    }

    // ------------------------------------------------------------------ 1 register_success
    @Test
    @DisplayName("POST /register → 201 + UserDto con displayName")
    void register_success() throws Exception {
        AppUser saved = buildUser(1L, "test_register@example.com", "TestUser", 1);
        when(authService.register(any(RegisterDto.class), anyString())).thenReturn(saved);

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
    @DisplayName("POST /login con contraseña incorrecta → 401")
    void login_bad_credentials() throws Exception {
        when(authService.login(any(LoginDto.class), any(), any(), anyString()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginDto dto = new LoginDto("badlogin@example.com", "wrongpass", false);

        mvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 3 me_without_session
    @Test
    @DisplayName("GET /me sin sesión → 401")
    void me_without_session() throws Exception {
        mvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ 4 login_disabled_user
    @Test
    @DisplayName("POST /login con enabled=0 → 403")
    void login_disabled_user() throws Exception {
        when(authService.login(any(LoginDto.class), any(), any(), anyString()))
                .thenThrow(new DisabledUserException("Account disabled"));

        LoginDto dto = new LoginDto("disabled@example.com", "pass", false);

        mvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ 5 update_profile_success
    @Test
    @DisplayName("PUT /me con sesión válida → 200 + displayName actualizado, evento PROFILE_UPDATE")
    @WithMockUser(username = "profile_update@example.com", roles = "USER")
    void update_profile_success() throws Exception {
        AppUser updated = buildUser(42L, "profile_update@example.com", "NewName", 1);
        when(authService.updateProfile(
                anyString(), any(UpdateProfileDto.class), anyString()))
                .thenReturn(updated);

        UpdateProfileDto upd = new UpdateProfileDto("NewName", null);

        mvc.perform(put(ME_PUT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(upd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("NewName"));

        // El controlador delegó al servicio → el servicio (real, en producción) registraría PROFILE_UPDATE
        verify(authService).updateProfile(
                org.mockito.ArgumentMatchers.eq("profile_update@example.com"),
                any(UpdateProfileDto.class),
                anyString());
    }
}
