package com.geovideomap.monolith.auth;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad mínima para los tests @WebMvcTest.
 *
 * Sustituye la cadena de filtros real (que incluye OAuth2 y remember-me)
 * por una cadena simplificada que:
 *  - Deshabilita CSRF para facilitar los POST/PUT con MockMvc
 *  - Exige autenticación en cualquier ruta (igual que producción para las rutas protegidas)
 *  - Devuelve 401 (no 302) cuando no hay sesión
 *  - Permite las rutas públicas de auth sin autenticación
 *
 * No hay DataSource, no hay Hikari, no hay MySQL.
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            // Sin oauth2Login → sin redirect 302
            // Sin rememberMe → sin dependencia de rememberMeKey
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    (req, res, e) -> res.sendError(
                        jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized"
                    )
                )
            );
        return http.build();
    }
}
