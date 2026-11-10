package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;

public record PassagemDeStatus(StatusPedido status, LocalDateTime dataHora, String responsavel) {
}
