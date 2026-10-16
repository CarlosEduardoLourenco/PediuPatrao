package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "auditoria")
@CompoundIndex(def = "{'entidade': 1, 'entidadeId': 1}")
public record RegistroAuditoria(
        @Id String id,
        String usuario,
        Perfil perfil,
        @Indexed LocalDateTime dataHora,
        TipoOperacao operacao,
        TipoEntidade entidade,
        String entidadeId,
        List<AlteracaoCampo> alteracoes,
        String justificativa) {

    public RegistroAuditoria withId(String novoId) {
        return new RegistroAuditoria(novoId, usuario, perfil, dataHora, operacao, entidade, entidadeId,
                alteracoes, justificativa);
    }
}
