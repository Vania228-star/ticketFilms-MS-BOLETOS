package com.ticketfilms.ms_boletos.exception;

import java.util.Arrays;

import com.ticketfilms.ms_boletos.model.CategoriaAsiento;

public class CategoriaInvalidaException extends RuntimeException {
    public CategoriaInvalidaException(String categoria) {
        super("Categoría inválida: '" + categoria + "'. Valores permitidos: "
                + Arrays.toString(CategoriaAsiento.values()));
    }
}
