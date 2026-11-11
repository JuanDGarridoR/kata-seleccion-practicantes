package com.juangarrido.practicantes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    public static final String ROL_ANALISTA = "ANALISTA";

    /*
     * - Sin sesión en servidor: cada petición se autentica con el header Authorization (HTTP Basic).
     * - CSRF deshabilitado porque no hay cookies de sesión que un sitio externo pueda reutilizar.
     * - Spring Security omite el header WWW-Authenticate cuando la petición trae
     *   "X-Requested-With: XMLHttpRequest"; el interceptor de Angular lo envía para que el navegador
     *   no muestre su ventana nativa de login. Clientes como curl o Postman sí reciben el desafío Basic.
     */
    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, "/api/practicantes").permitAll()
                        .requestMatchers("/api/**").hasRole(ROL_ANALISTA)
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /** Un único analista en memoria para el alcance del reto; la contraseña se guarda como hash BCrypt. */
    @Bean
    UserDetailsService usuarios(@Value("${app.analista.usuario}") String usuario,
                                @Value("${app.analista.password-hash}") String passwordHash) {
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario).password(passwordHash).roles(ROL_ANALISTA).build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
