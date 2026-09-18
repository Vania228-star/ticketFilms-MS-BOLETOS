package com.ticketfilms.ms_boletos.exception;

public class AsientosServiceException extends RuntimeException {
    public AsientosServiceException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
