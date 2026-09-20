package com.ticketfilms.ms_boletos.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Boleto inexistente, o que pertenece a otro usuario (no se revela cuál de los dos)
    @ExceptionHandler(BoletoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(BoletoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpoError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(CategoriaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleCategoriaInvalida(CategoriaInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpoError(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    // JSON mal formado o con caracteres inválidos: respuesta limpia, sin stack trace
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(cuerpoError(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido"));
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
            "mensaje", mensaje != null ? mensaje : status.getReasonPhrase()
        );
    }
}
