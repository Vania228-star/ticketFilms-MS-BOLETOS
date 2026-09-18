package com.ticketfilms.ms_boletos.client;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.ticketfilms.ms_boletos.dto.ConfirmarAsientosRequest;
import com.ticketfilms.ms_boletos.exception.AsientoNoDisponibleException;
import com.ticketfilms.ms_boletos.exception.AsientosServiceException;
import com.ticketfilms.ms_boletos.exception.ReservaAjenaException;
import com.ticketfilms.ms_boletos.exception.ReservaExpiradaException;

import lombok.RequiredArgsConstructor;

// Llama a ms-asientos PUT /api/asientos/confirmar para pasar los asientos
// de RESERVADO a OCUPADO. Se reenvía el MISMO JWT que llegó a ms-boletos,
// porque ms-asientos valida ahí que la reserva pertenezca a ese usuario.
@Component
@RequiredArgsConstructor
public class AsientosClient {

    private final RestClient asientosRestClient;

    public void confirmarAsientos(String bearerToken, Long funcionId, List<Long> asientoIds) {
        ConfirmarAsientosRequest request = new ConfirmarAsientosRequest(funcionId, asientoIds);

        try {
            asientosRestClient.put()
                    .uri("/api/asientos/confirmar")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw traducirError(ex);
        }
    }

    private RuntimeException traducirError(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String cuerpo = ex.getResponseBodyAsString();

        if (status.value() == 403) {
            return new ReservaAjenaException(cuerpo);
        }
        if (status.value() == 410) {
            return new ReservaExpiradaException(cuerpo);
        }
        if (status.is4xxClientError()) {
            return new AsientoNoDisponibleException(cuerpo);
        }
        return new AsientosServiceException(
                "ms-asientos no pudo confirmar los asientos (status " + status.value() + ")", ex);
    }
}
