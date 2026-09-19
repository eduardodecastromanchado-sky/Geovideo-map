package com.geovideomap.monolith.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ------------------------------------------------------------------ POST /register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto dto, HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        try {
            AppUser user = authService.register(dto, ip);
            return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ POST /login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto,
                                   HttpServletRequest req,
                                   HttpServletResponse res) {
        String ip = req.getRemoteAddr();
        try {
            AppUser user = authService.login(dto, req, res, ip);
            return ResponseEntity.ok(UserDto.from(user));
        } catch (DisabledUserException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account disabled");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials");
        }
    }

    // ------------------------------------------------------------------ POST /logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        String email = resolveEmail();
        if (email != null && !"anonymousUser".equals(email)) {
            authService.logLogoutEvent(email, req.getRemoteAddr());
        }
        SecurityContextHolder.clearContext();
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        // Clear remember-me cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("remember-me", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        res.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ GET /me
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        String email = resolveEmail();
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return authService.findByEmail(email)
                .map(u -> {
                    if (u.getEnabled() == 0) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    return ResponseEntity.ok(UserDto.from(u));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ------------------------------------------------------------------ PUT /me
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateProfileDto dto,
                                      HttpServletRequest req) {
        String email = resolveEmail();
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            AppUser user = authService.updateProfile(email, dto, req.getRemoteAddr());
            return ResponseEntity.ok(UserDto.from(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ------------------------------------------------------------------ helpers
    private String resolveEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth) {
            return oauth.getAttribute("email");
        }
        return auth.getName();
    }
}
