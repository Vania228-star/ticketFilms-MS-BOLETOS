package com.ticketfilms.ms_boletos.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ticketfilms.ms_boletos.client.AsientosClient;
import com.ticketfilms.ms_boletos.dto.AsientoCompraDto;
import com.ticketfilms.ms_boletos.dto.BoletoResponseDto;
import com.ticketfilms.ms_boletos.dto.ConfirmarCompraRequestDto;
import com.ticketfilms.ms_boletos.exception.BoletoNoEncontradoException;
import com.ticketfilms.ms_boletos.exception.CategoriaInvalidaException;
import com.ticketfilms.ms_boletos.model.Boleto;
import com.ticketfilms.ms_boletos.model.BoletoAsiento;
import com.ticketfilms.ms_boletos.model.CategoriaAsiento;
import com.ticketfilms.ms_boletos.model.EstadoBoleto;
import com.ticketfilms.ms_boletos.repository.BoletoRepository;
import com.ticketfilms.ms_boletos.service.support.CodigoBoletoGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoletoService {

    private final BoletoRepository boletoRepository;
    private final CodigoBoletoGenerator codigoBoletoGenerator;
    private final AsientosClient asientosClient;
    private final TransactionTemplate transactionTemplate;

    // Sin @Transactional a propósito: cada paso usa su propia transacción
    // (TransactionTemplate) para que el estado del boleto quede guardado
    // aunque falle un paso posterior.
    public BoletoResponseDto confirmarCompra(String usuarioId, String bearerToken, ConfirmarCompraRequestDto request) {

        List<AsientoCompraDto> asientosDto = request.getAsientos();
        if (asientosDto == null || asientosDto.isEmpty()) {
            throw new IllegalArgumentException("La compra debe incluir al menos un asiento");
        }

        List<Long> asientoIds = asientosDto.stream()
                .map(AsientoCompraDto::getAsientoId)
                .collect(Collectors.toList());

        // Paso 1: construir (valida categorías y precios) y guardar como PENDIENTE
        Boleto boleto = construirBoleto(usuarioId, request, asientosDto);
        String codigo = boleto.getCodigoBoleto();
        transactionTemplate.execute(status -> boletoRepository.save(boleto));

        // Paso 2: confirmar los asientos en ms-asientos
        try {
            asientosClient.confirmarAsientos(bearerToken, request.getFuncionId(), asientoIds);
        } catch (RuntimeException e) {
            cambiarEstado(codigo, EstadoBoleto.CANCELADO);
            throw e;
        }

        // Paso 3: marcar CONFIRMADO. Si esto fallara, el boleto queda PENDIENTE
        // con los asientos ya OCUPADO: es detectable y corregible.
        return transactionTemplate.execute(status -> {
            Boleto b = boletoRepository.findByCodigoBoleto(codigo).orElseThrow();
            b.setEstado(EstadoBoleto.CONFIRMADO);
            return aResponseDto(boletoRepository.save(b));
        });
    }

    private Boleto construirBoleto(String usuarioId, ConfirmarCompraRequestDto request,
                                   List<AsientoCompraDto> asientosDto) {
        BigDecimal total = asientosDto.stream()
                .map(AsientoCompraDto::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Boleto boleto = Boleto.builder()
                .codigoBoleto(codigoBoletoGenerator.generar())
                .usuarioId(usuarioId)
                .funcionId(request.getFuncionId())
                .eventoId(request.getEventoId())
                .tituloEvento(request.getTituloEvento())
                .fechaHoraFuncion(request.getFechaHoraFuncion())
                .precioTotal(total)
                .cantidadAsientos(asientosDto.size())
                .estado(EstadoBoleto.PENDIENTE)
                .build();

        asientosDto.forEach(a -> boleto.agregarAsiento(
                BoletoAsiento.builder()
                        .asientoId(a.getAsientoId())
                        .fila(a.getFila())
                        .numero(a.getNumero())
                        .categoria(parsearCategoria(a.getCategoria()))
                        .precioPagado(a.getPrecio())
                        .build()
        ));
        return boleto;
    }

    private CategoriaAsiento parsearCategoria(String categoria) {
        if (categoria == null) {
            throw new CategoriaInvalidaException(null);
        }
        try {
            return CategoriaAsiento.valueOf(categoria.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CategoriaInvalidaException(categoria);
        }
    }

    private void cambiarEstado(String codigo, EstadoBoleto nuevoEstado) {
        transactionTemplate.executeWithoutResult(status ->
                boletoRepository.findByCodigoBoleto(codigo).ifPresent(b -> {
                    b.setEstado(nuevoEstado);
                    boletoRepository.save(b);
                }));
    }

    public List<BoletoResponseDto> obtenerHistorial(String usuarioId) {
        return boletoRepository.findByUsuarioIdOrderByFechaCompraDesc(usuarioId)
                .stream()
                .filter(b -> b.getEstado() == EstadoBoleto.CONFIRMADO)
                .map(this::aResponseDto)
                .collect(Collectors.toList());
    }

    // Solo el dueño puede ver su boleto. Si no existe o es de otro usuario,
    // la respuesta es la misma (404) para no revelar qué códigos existen.
    public BoletoResponseDto obtenerPorCodigo(String usuarioId, String codigoBoleto) {
        Boleto boleto = boletoRepository.findByCodigoBoleto(codigoBoleto)
                .filter(b -> usuarioId.equals(b.getUsuarioId()))
                .orElseThrow(() -> new BoletoNoEncontradoException(codigoBoleto));
        return aResponseDto(boleto);
    }

    private BoletoResponseDto aResponseDto(Boleto boleto) {
        return BoletoResponseDto.builder()
                .codigoBoleto(boleto.getCodigoBoleto())
                .tituloEvento(boleto.getTituloEvento())
                .fechaHoraFuncion(boleto.getFechaHoraFuncion())
                .precioTotal(boleto.getPrecioTotal())
                .cantidadAsientos(boleto.getCantidadAsientos())
                .estado(boleto.getEstado().name())
                .fechaCompra(boleto.getFechaCompra())
                .asientos(boleto.getAsientos().stream()
                        .map(a -> a.getFila() + a.getNumero())
                        .collect(Collectors.toList()))
                .build();
    }
}
