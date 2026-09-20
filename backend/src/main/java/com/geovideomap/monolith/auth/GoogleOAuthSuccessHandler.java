package com.geovideomap.monolith.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After Google OAuth2 completes, upsert the user in app_users and redirect to the
 * frontend (Angular). The browser keeps the session cookie issued by Spring.
 */
@Component
public class GoogleOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public GoogleOAuthSuccessHandler(@Lazy AuthService authService,
                                     @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
        setDefaultTargetUrl(frontendUrl);
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String googleSub = principal.getAttribute("sub");
        String email     = principal.getAttribute("email");
        String name      = principal.getAttribute("name");
        String picture   = principal.getAttribute("picture");
        String ip        = request.getRemoteAddr();

        authService.upsertGoogleUser(googleSub, email, name, picture, ip);

        // Redirect browser to Angular front (session cookie is already set by Spring)
        response.sendRedirect(frontendUrl);
    }
}
