package com.geovideomap.monolith.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final AppUserEventRepository eventRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final RememberMeServices rememberMeServices;

    public AuthService(AppUserRepository userRepo,
                       AppUserEventRepository eventRepo,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager,
                       RememberMeServices rememberMeServices) {
        this.userRepo = userRepo;
        this.eventRepo = eventRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.rememberMeServices = rememberMeServices;
    }

    // ------------------------------------------------------------------ register
    @Transactional
    public AppUser register(RegisterDto dto, String ip) {
        if (userRepo.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        AppUser user = new AppUser();
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setProvider("local");
        user.setEnabled(1);
        String name = (dto.displayName() != null && !dto.displayName().isBlank())
                ? dto.displayName()
                : dto.email().split("@")[0];
        user.setDisplayName(name);
        AppUser saved = userRepo.save(user);
        logEvent(saved.getId(), "REGISTER", null, ip);
        return saved;
    }

    // ------------------------------------------------------------------ login
    @Transactional
    public AppUser login(LoginDto dto, HttpServletRequest req, HttpServletResponse res, String ip) {
        AppUser appUser = userRepo.findByEmail(dto.email())
                .orElse(null);
        if (appUser == null) {
            logEvent(null, "LOGIN_FAIL", "email not found: " + dto.email(), ip);
            throw new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
        }
        if (appUser.getEnabled() == 0) {
            logEvent(appUser.getId(), "DISABLED", "login attempt on disabled account", ip);
            throw new DisabledUserException("Account disabled");
        }

        // Throws BadCredentialsException if wrong password
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        // Persist Security context into session
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        SecurityContextHolder.setContext(sc);
        req.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

        if (dto.rememberMe()) {
            rememberMeServices.loginSuccess(req, res, auth);
        }

        logEvent(appUser.getId(), "LOGIN_OK", null, ip);
        return appUser;
    }

    // ------------------------------------------------------------------ me
    public Optional<AppUser> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // ------------------------------------------------------------------ updateProfile
    @Transactional
    public AppUser updateProfile(String email, UpdateProfileDto dto, String ip) {
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.displayName() == null || dto.displayName().isBlank()) {
            throw new IllegalArgumentException("displayName is required");
        }
        user.setDisplayName(dto.displayName().trim());

        if (dto.password() != null && !dto.password().isBlank()) {
            if (!"local".equals(user.getProvider())) {
                throw new IllegalArgumentException("Cannot change password for OAuth users");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        AppUser saved = userRepo.save(user);
        logEvent(saved.getId(), "PROFILE_UPDATE", null, ip);
        return saved;
    }

    // ------------------------------------------------------------------ upsertGoogleUser
    @Transactional
    public AppUser upsertGoogleUser(String googleSub, String email, String name, String avatarUrl, String ip) {
        // Lookup by google_sub first, then by email
        AppUser user = userRepo.findByGoogleSub(googleSub)
                .or(() -> userRepo.findByEmail(email))
                .orElse(null);

        boolean isNew = (user == null);
        if (isNew) {
            user = new AppUser();
            user.setEmail(email);
            user.setProvider("google");
            user.setEnabled(1);
        }
        user.setGoogleSub(googleSub);
        user.setDisplayName(name != null ? name : email.split("@")[0]);
        user.setAvatarUrl(avatarUrl);
        if (isNew) {
            user.setProvider("google");
        }
        AppUser saved = userRepo.save(user);
        logEvent(saved.getId(), isNew ? "REGISTER" : "LOGIN_GOOGLE", "google", ip);
        return saved;
    }

    // ------------------------------------------------------------------ logLogout (called from controller)
    public void logLogoutEvent(String email, String ip) {
        userRepo.findByEmail(email).ifPresent(u -> logEvent(u.getId(), "LOGOUT", null, ip));
    }

    // ------------------------------------------------------------------ private
    private void logEvent(Long userId, String type, String detail, String ip) {
        AppUserEvent ev = new AppUserEvent();
        ev.setUserId(userId);
        ev.setEventType(type);
        ev.setDetail(detail);
        ev.setIp(ip);
        eventRepo.save(ev);
    }
}
