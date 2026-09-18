package com.ticketfilms.ms_boletos.exception;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException(String mensaje) {
        super(mensaje);
    }
}
