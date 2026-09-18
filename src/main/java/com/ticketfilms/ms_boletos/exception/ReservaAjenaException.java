package com.ticketfilms.ms_boletos.exception;

public class ReservaAjenaException extends RuntimeException {
    public ReservaAjenaException(String mensaje) {
        super(mensaje);
    }
}
