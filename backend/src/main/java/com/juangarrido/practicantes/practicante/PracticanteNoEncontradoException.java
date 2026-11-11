package com.juangarrido.practicantes.practicante;

public class PracticanteNoEncontradoException extends RuntimeException {

    public PracticanteNoEncontradoException(Long id) {
        super("No existe un practicante con id " + id);
    }
}
