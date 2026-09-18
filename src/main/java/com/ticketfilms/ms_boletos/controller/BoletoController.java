package com.ticketfilms.ms_boletos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketfilms.ms_boletos.dto.BoletoResponseDto;
import com.ticketfilms.ms_boletos.dto.ConfirmarCompraRequestDto;
import com.ticketfilms.ms_boletos.service.BoletoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boletos")
@RequiredArgsConstructor
public class BoletoController {

    private final BoletoService boletoService;

    @PostMapping("/compra")
    public ResponseEntity<BoletoResponseDto> confirmarCompra(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody ConfirmarCompraRequestDto request
    ) {
        String usuarioId = extraerUsuarioId(jwt);
        // Se reenvía el JWT crudo a ms-asientos para confirmar los asientos
        // a nombre del mismo usuario que está comprando.
        BoletoResponseDto boleto = boletoService.confirmarCompra(usuarioId, jwt.getTokenValue(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(boleto);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<BoletoResponseDto>> obtenerHistorial(@AuthenticationPrincipal Jwt jwt) {
        String usuarioId = extraerUsuarioId(jwt);
        return ResponseEntity.ok(boletoService.obtenerHistorial(usuarioId));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<BoletoResponseDto> obtenerPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(boletoService.obtenerPorCodigo(codigo));
    }

    private String extraerUsuarioId(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        if (sub != null) return sub;
        return jwt.getClaimAsString("email");
    }
}
