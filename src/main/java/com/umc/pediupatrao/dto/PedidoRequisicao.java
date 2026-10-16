package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.TipoSaida;
import java.util.List;

public record PedidoRequisicao(String clienteId, TipoSaida tipoSaida, List<ItemRequisicao> itens) {
}
