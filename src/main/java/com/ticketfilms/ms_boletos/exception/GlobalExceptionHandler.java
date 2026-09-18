package com.ticketfilms.ms_boletos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(HttpStatus.BAD_REQUEST, detalle));
    }

    // Asiento(s) ya no disponibles (no existen o no estaban RESERVADO) al confirmar
    @ExceptionHandler(AsientoNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleAsientoNoDisponible(AsientoNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpoError(HttpStatus.CONFLICT, ex.getMessage()));
    }

    // La reserva en ms-asientos pertenece a otro usuario
    @ExceptionHandler(ReservaAjenaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaAjena(ReservaAjenaException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(cuerpoError(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    // La reserva temporal (5 min) ya expiró en ms-asientos
    @ExceptionHandler(ReservaExpiradaException.class)
    public ResponseEntity<Map<String, Object>> handleReservaExpirada(ReservaExpiradaException ex) {
        return ResponseEntity.status(HttpStatus.GONE).body(cuerpoError(HttpStatus.GONE, ex.getMessage()));
    }

    // ms-asientos no respondió o devolvió un error inesperado
    @ExceptionHandler(AsientosServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAsientosServiceDown(AsientosServiceException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(cuerpoError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    private Map<String, Object> cuerpoError(HttpStatus status, String mensaje) {
        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "mensaje", mensaje
        );
    }
}
