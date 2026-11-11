package com.juangarrido.practicantes.practicante;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String correo) {
        super("Ya existe un registro con el correo " + correo);
    }
}
