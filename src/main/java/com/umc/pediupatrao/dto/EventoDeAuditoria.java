package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.entity.TipoOperacao;
import java.util.List;

public record EventoDeAuditoria(TipoOperacao operacao, TipoEntidade entidade, String entidadeId,
        List<AlteracaoCampo> alteracoes, String justificativa) {
}
