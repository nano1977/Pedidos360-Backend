package cl.duoc.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Toda la API de administración exige un JWT válido de Azure AD con el rol ROLE_ADMIN.
 * Sin token: 401. Token sin el rol: 403.
 *
 * El rol se obtiene de dos fuentes:
 *  1. El claim "roles" del token (App Role asignado en Azure AD).
 *  2. La lista app.security.admin-users, para cuentas a las que el tenant no permite
 *     asignar App Roles (por ejemplo, usuarios invitados sin permisos de administrador).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.rol-admin}")
    private String rolAdmin;

    @Value("${app.security.admin-users:}")
    private List<String> adminUsers;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority(rolAdmin)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            ));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator()));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter rolesDelToken = new JwtGrantedAuthoritiesConverter();
        rolesDelToken.setAuthoritiesClaimName("roles");
        rolesDelToken.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> permisos = new ArrayList<>(rolesDelToken.convert(jwt));
            String usuario = usuarioDelToken(jwt);
            if (usuario != null && esAdministradorConfigurado(usuario)) {
                permisos.add(new SimpleGrantedAuthority(rolAdmin));
                log.info("[SEGURIDAD] {} recibe {} por la lista app.security.admin-users", usuario, rolAdmin);
            }
            return permisos;
        });
        return converter;
    }

    /** Azure AD envía el correo en distintos claims según la versión del token. */
    private String usuarioDelToken(Jwt jwt) {
        return Stream.of("preferred_username", "upn", "unique_name", "email")
                .map(jwt::getClaimAsString)
                .filter(valor -> valor != null && !valor.isBlank())
                .findFirst()
                .orElse(null);
    }

    private boolean esAdministradorConfigurado(String usuario) {
        return adminUsers.stream()
                .map(String::trim)
                .anyMatch(admin -> !admin.isEmpty() && admin.equalsIgnoreCase(usuario));
    }
}
