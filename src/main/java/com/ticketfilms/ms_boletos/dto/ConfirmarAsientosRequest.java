package com.ticketfilms.ms_boletos.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Espejo de ReservaRequestDto de ms-asientos: {funcionId, asientosSolicitados}
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarAsientosRequest {
    private Long funcionId;
    private List<Long> asientosSolicitados;
}
