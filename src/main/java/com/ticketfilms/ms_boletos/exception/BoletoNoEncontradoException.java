package com.ticketfilms.ms_boletos.exception;

public class BoletoNoEncontradoException extends RuntimeException {
    public BoletoNoEncontradoException(String codigo) {
        super("Boleto no encontrado: " + codigo);
    }
}
