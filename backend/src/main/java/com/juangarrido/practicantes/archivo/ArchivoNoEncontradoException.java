package com.juangarrido.practicantes.archivo;

public class ArchivoNoEncontradoException extends RuntimeException {

    public ArchivoNoEncontradoException(String nombre) {
        super("No se encontró el archivo " + nombre);
    }
}
