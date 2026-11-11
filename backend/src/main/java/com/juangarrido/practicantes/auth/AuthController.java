package com.juangarrido.practicantes.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Con HTTP Basic no hay "login" en el servidor: cada petición trae sus credenciales.
 * El frontend llama a este endpoint protegido solo para comprobar que el usuario y la
 * contraseña son correctos antes de guardarlos en la sesión del navegador.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/sesion")
    public SesionResponse sesion(Authentication autenticacion) {
        return new SesionResponse(autenticacion.getName());
    }

    public record SesionResponse(String usuario) {
    }
}
