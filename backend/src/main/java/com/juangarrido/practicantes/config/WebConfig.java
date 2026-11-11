package com.juangarrido.practicantes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Angular maneja sus propias rutas en el navegador. Si el usuario recarga la página en
 * /analista/practicantes, el servidor no tiene ese archivo: se reenvía a index.html y
 * Angular resuelve la ruta.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/registro").setViewName("forward:/index.html");
        registry.addViewController("/analista").setViewName("forward:/index.html");
        registry.addViewController("/analista/**").setViewName("forward:/index.html");
    }
}
