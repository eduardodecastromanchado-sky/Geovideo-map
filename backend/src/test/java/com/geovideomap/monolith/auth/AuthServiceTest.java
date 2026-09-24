package com.geovideomap.monolith.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios puros de AuthService.
 * Sin Spring context, sin DataSource, sin MySQL.
 * Cubre todas las ramas de negocio relevantes para el paquete auth en SonarQube.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AppUserRepository    userRepo;
    @Mock AppUserEventRepository eventRepo;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock RememberMeServices   rememberMeServices;
    @Mock HttpServletRequest   request;
    @Mock HttpServletResponse  response;
    @Mock HttpSession          session;

    @InjectMocks AuthService service;

    // ========================================================================
    // Helpers
    // ========================================================================

    private AppUser buildUser(Long id, String email, String displayName,
                               String provider, int enabled) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(email);
        u.setDisplayName(displayName);
        u.setProvider(provider);
        u.setEnabled(enabled);
        u.setPasswordHash("$2a$hash");
        return u;
    }

    // ========================================================================
    // register
    // ========================================================================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("email duplicado → IllegalArgumentException")
        void duplicateEmail_throws() {
            AppUser existing = buildUser(1L, "a@b.com", "Existing", "local", 1);
            when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.register(
                    new RegisterDto("a@b.com", "Pass1!", "User"), "1.2.3.4"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");

            verify(userRepo, never()).save(any());
            verify(eventRepo, never()).save(any());
        }

        @Test
        @DisplayName("displayName presente → se usa el displayName del DTO")
        void withDisplayName_usesProvidedName() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            AppUser saved = buildUser(1L, "a@b.com", "TestUser", "local", 1);
            when(userRepo.save(any())).thenReturn(saved);

            AppUser result = service.register(
                    new RegisterDto("a@b.com", "Pass1!", "TestUser"), "1.2.3.4");

            assertThat(result.getDisplayName()).isEqualTo("TestUser");
            verify(passwordEncoder).encode("Pass1!");
            // Verifica que el evento REGISTER se grabó
            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("REGISTER");
        }

        @Test
        @DisplayName("displayName null → se usa la parte local del email")
        void nullDisplayName_usesEmailLocalPart() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");

            // Capturamos el AppUser que se pasa a save para verificar displayName
            ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
            AppUser saved = buildUser(1L, "johnny@example.com", "johnny", "local", 1);
            when(userRepo.save(userCaptor.capture())).thenReturn(saved);

            service.register(new RegisterDto("johnny@example.com", "Pass1!", null), "1.2.3.4");

            assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("johnny");
        }

        @Test
        @DisplayName("displayName blank → se usa la parte local del email")
        void blankDisplayName_usesEmailLocalPart() {
            when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");

            ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
            AppUser saved = buildUser(1L, "johnny@example.com", "johnny", "local", 1);
            when(userRepo.save(userCaptor.capture())).thenReturn(saved);

            service.register(new RegisterDto("johnny@example.com", "Pass1!", "   "), "1.2.3.4");

            assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("johnny");
        }
    }

    // ========================================================================
    // login
    // ========================================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @BeforeEach
        void setupSession() {
            // req.getSession(true) debe devolver una sesión válida en el happy path
            lenient().when(request.getSession(true)).thenReturn(session);
        }

        @Test
        @DisplayName("email no encontrado → BadCredentialsException + evento LOGIN_FAIL con userId null")
        void emailNotFound_throws() {
            when(userRepo.findByEmail("nope@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(
                    new LoginDto("nope@x.com", "pass", false), request, response, "1.1.1.1"))
                    .isInstanceOf(BadCredentialsException.class);

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("LOGIN_FAIL");
            assertThat(evCaptor.getValue().getUserId()).isNull();
        }

        @Test
        @DisplayName("usuario deshabilitado (enabled=0) → DisabledUserException + evento DISABLED")
        void disabledUser_throws() {
            AppUser disabled = buildUser(7L, "off@x.com", "Off", "local", 0);
            when(userRepo.findByEmail("off@x.com")).thenReturn(Optional.of(disabled));

            assertThatThrownBy(() -> service.login(
                    new LoginDto("off@x.com", "pass", false), request, response, "1.1.1.1"))
                    .isInstanceOf(DisabledUserException.class)
                    .hasMessageContaining("disabled");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("DISABLED");
            assertThat(evCaptor.getValue().getUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("contraseña incorrecta (authManager lanza BadCredentials) → burbujea sin evento extra")
        void wrongPassword_bubbles() {
            AppUser user = buildUser(3L, "u@x.com", "User", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));
            when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

            assertThatThrownBy(() -> service.login(
                    new LoginDto("u@x.com", "wrong", false), request, response, "1.1.1.1"))
                    .isInstanceOf(BadCredentialsException.class);

            // Ningún evento de éxito o fallo grabado (el servicio no captura esta excepción)
            verify(eventRepo, never()).save(any());
        }

        @Test
        @DisplayName("happy path sin rememberMe=false → LOGIN_OK + retorna usuario, rememberMe NO llamado")
        void happyPath_noRememberMe() {
            AppUser user = buildUser(5L, "ok@x.com", "OK", "local", 1);
            when(userRepo.findByEmail("ok@x.com")).thenReturn(Optional.of(user));
            Authentication auth = mock(Authentication.class);
            when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(auth);

            AppUser result = service.login(
                    new LoginDto("ok@x.com", "P@ss!", false), request, response, "5.5.5.5");

            assertThat(result.getId()).isEqualTo(5L);

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("LOGIN_OK");

            verify(rememberMeServices, never()).loginSuccess(any(), any(), any());
        }

        @Test
        @DisplayName("rememberMe=true → rememberMeServices.loginSuccess llamado")
        void rememberMe_callsService() {
            AppUser user = buildUser(5L, "ok@x.com", "OK", "local", 1);
            when(userRepo.findByEmail("ok@x.com")).thenReturn(Optional.of(user));
            Authentication auth = mock(Authentication.class);
            when(authManager.authenticate(any())).thenReturn(auth);

            service.login(new LoginDto("ok@x.com", "P@ss!", true), request, response, "5.5.5.5");

            verify(rememberMeServices).loginSuccess(request, response, auth);
        }
    }

    // ========================================================================
    // findByEmail
    // ========================================================================

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("delega a userRepo y retorna el Optional")
        void delegates_toRepo() {
            AppUser user = buildUser(1L, "a@b.com", "A", "local", 1);
            when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(user));

            Optional<AppUser> result = service.findByEmail("a@b.com");

            assertThat(result).contains(user);
        }
    }

    // ========================================================================
    // updateProfile
    // ========================================================================

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("usuario no encontrado → IllegalArgumentException")
        void userNotFound_throws() {
            when(userRepo.findByEmail("x@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProfile(
                    "x@x.com", new UpdateProfileDto("NewName", null), "1.1.1.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("displayName null → IllegalArgumentException")
        void nullDisplayName_throws() {
            AppUser user = buildUser(1L, "u@x.com", "Old", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.updateProfile(
                    "u@x.com", new UpdateProfileDto(null, null), "1.1.1.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayName is required");
        }

        @Test
        @DisplayName("displayName blank → IllegalArgumentException")
        void blankDisplayName_throws() {
            AppUser user = buildUser(1L, "u@x.com", "Old", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.updateProfile(
                    "u@x.com", new UpdateProfileDto("   ", null), "1.1.1.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("displayName is required");
        }

        @Test
        @DisplayName("cambio de password en usuario Google → IllegalArgumentException")
        void passwordChangeOnOAuthUser_throws() {
            AppUser googleUser = buildUser(2L, "g@x.com", "G", "google", 1);
            googleUser.setPasswordHash(null);
            when(userRepo.findByEmail("g@x.com")).thenReturn(Optional.of(googleUser));

            assertThatThrownBy(() -> service.updateProfile(
                    "g@x.com", new UpdateProfileDto("NewName", "newpass"), "1.1.1.1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot change password for OAuth users");

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("actualización de nombre + password en usuario local → PROFILE_UPDATE + passwordEncoder llamado")
        void updateNameAndPassword_localUser() {
            AppUser user = buildUser(3L, "u@x.com", "OldName", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpass")).thenReturn("$newHash");
            AppUser saved = buildUser(3L, "u@x.com", "NewName", "local", 1);
            when(userRepo.save(any())).thenReturn(saved);

            AppUser result = service.updateProfile(
                    "u@x.com", new UpdateProfileDto("NewName", "newpass"), "1.1.1.1");

            assertThat(result.getDisplayName()).isEqualTo("NewName");
            verify(passwordEncoder).encode("newpass");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("PROFILE_UPDATE");
        }

        @Test
        @DisplayName("actualización de nombre sin password → passwordEncoder NO llamado")
        void updateNameOnly_noPasswordEncoder() {
            AppUser user = buildUser(3L, "u@x.com", "OldName", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));
            AppUser saved = buildUser(3L, "u@x.com", "NewName", "local", 1);
            when(userRepo.save(any())).thenReturn(saved);

            service.updateProfile("u@x.com", new UpdateProfileDto("NewName", null), "1.1.1.1");

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepo).save(any());
        }

        @Test
        @DisplayName("actualización con password blank → passwordEncoder NO llamado")
        void blankPassword_noPasswordEncoder() {
            AppUser user = buildUser(3L, "u@x.com", "OldName", "local", 1);
            when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));
            AppUser saved = buildUser(3L, "u@x.com", "NewName", "local", 1);
            when(userRepo.save(any())).thenReturn(saved);

            service.updateProfile("u@x.com", new UpdateProfileDto("NewName", "  "), "1.1.1.1");

            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ========================================================================
    // upsertGoogleUser
    // ========================================================================

    @Nested
    @DisplayName("upsertGoogleUser()")
    class UpsertGoogleUser {

        @Test
        @DisplayName("usuario completamente nuevo → provider=google, evento REGISTER")
        void newUser_createsAndRegisters() {
            when(userRepo.findByGoogleSub("sub123")).thenReturn(Optional.empty());
            when(userRepo.findByEmail("new@g.com")).thenReturn(Optional.empty());
            AppUser saved = buildUser(10L, "new@g.com", "New User", "google", 1);
            when(userRepo.save(any())).thenReturn(saved);

            AppUser result = service.upsertGoogleUser(
                    "sub123", "new@g.com", "New User", "http://pic", "1.1.1.1");

            assertThat(result.getProvider()).isEqualTo("google");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("REGISTER");
        }

        @Test
        @DisplayName("usuario existente por googleSub → actualiza datos, evento LOGIN_GOOGLE")
        void existingByGoogleSub_updatesAndLogsGoogleLogin() {
            AppUser existing = buildUser(11L, "ex@g.com", "OldName", "google", 1);
            existing.setGoogleSub("sub456");
            when(userRepo.findByGoogleSub("sub456")).thenReturn(Optional.of(existing));
            when(userRepo.save(any())).thenReturn(existing);

            service.upsertGoogleUser(
                    "sub456", "ex@g.com", "UpdatedName", "http://newpic", "1.1.1.1");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("LOGIN_GOOGLE");
        }

        @Test
        @DisplayName("existente por email (migración local→Google) → vincula sub, evento LOGIN_GOOGLE")
        void existingByEmail_linksGoogleSub() {
            AppUser localUser = buildUser(12L, "local@g.com", "Local", "local", 1);
            when(userRepo.findByGoogleSub("subNew")).thenReturn(Optional.empty());
            when(userRepo.findByEmail("local@g.com")).thenReturn(Optional.of(localUser));
            when(userRepo.save(any())).thenReturn(localUser);

            service.upsertGoogleUser(
                    "subNew", "local@g.com", "Local Google", "http://pic", "1.1.1.1");

            ArgumentCaptor<AppUser> savedUser = ArgumentCaptor.forClass(AppUser.class);
            verify(userRepo).save(savedUser.capture());
            assertThat(savedUser.getValue().getGoogleSub()).isEqualTo("subNew");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("LOGIN_GOOGLE");
        }

        @Test
        @DisplayName("name=null → displayName usa parte local del email")
        void nullName_usesEmailLocalPart() {
            when(userRepo.findByGoogleSub("subX")).thenReturn(Optional.empty());
            when(userRepo.findByEmail("user@domain.com")).thenReturn(Optional.empty());
            ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
            AppUser saved = buildUser(20L, "user@domain.com", "user", "google", 1);
            when(userRepo.save(userCaptor.capture())).thenReturn(saved);

            service.upsertGoogleUser("subX", "user@domain.com", null, null, "1.1.1.1");

            assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("user");
        }
    }

    // ========================================================================
    // logLogoutEvent
    // ========================================================================

    @Nested
    @DisplayName("logLogoutEvent()")
    class LogLogout {

        @Test
        @DisplayName("email existe → graba evento LOGOUT")
        void emailFound_savesLogoutEvent() {
            AppUser user = buildUser(9L, "bye@x.com", "Bye", "local", 1);
            when(userRepo.findByEmail("bye@x.com")).thenReturn(Optional.of(user));

            service.logLogoutEvent("bye@x.com", "9.9.9.9");

            ArgumentCaptor<AppUserEvent> evCaptor = ArgumentCaptor.forClass(AppUserEvent.class);
            verify(eventRepo).save(evCaptor.capture());
            assertThat(evCaptor.getValue().getEventType()).isEqualTo("LOGOUT");
            assertThat(evCaptor.getValue().getUserId()).isEqualTo(9L);
        }

        @Test
        @DisplayName("email no existe → eventRepo.save NO llamado (silencioso)")
        void emailNotFound_noEvent() {
            when(userRepo.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

            service.logLogoutEvent("ghost@x.com", "9.9.9.9");

            verify(eventRepo, never()).save(any());
        }
    }
}
