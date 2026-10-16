package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.EventoDeAuditoria;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.entity.TipoOperacao;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ClienteAuditor {

    private static final String JUSTIFICATIVA_EXPURGO = "Prazo de retenção de 1 ano encerrado";

    private final AuditoriaService auditoriaService;

    public ClienteAuditor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    public Map<String, String> fotografar(Cliente cliente) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("nome", cliente.getNome());
        campos.put("telefone", cliente.getTelefone());
        campos.put("cep", cliente.getCep());
        campos.put("logradouro", cliente.getLogradouro());
        campos.put("numero", cliente.getNumero());
        campos.put("complemento", cliente.getComplemento());
        campos.put("bairro", cliente.getBairro());
        campos.put("cidade", cliente.getCidade());
        campos.put("estado", cliente.getEstado());
        campos.put("termoAceitoEm", textoDe(cliente.getTermoAceitoEm()));
        return campos;
    }

    public void criacao(Cliente cliente) {
        registrarDiferencas(TipoOperacao.CRIACAO_CLIENTE, cliente, Collections.emptyMap());
    }

    public void alteracao(Cliente cliente, Map<String, String> fotoAnterior) {
        registrarDiferencas(TipoOperacao.ALTERACAO_CLIENTE, cliente, fotoAnterior);
    }

    public void inativacao(Cliente cliente) {
        auditoriaService.registrar(new EventoDeAuditoria(TipoOperacao.INATIVACAO_CLIENTE, TipoEntidade.CLIENTE,
                cliente.getId(), List.of(
                        new AlteracaoCampo("ativo", "true", "false"),
                        new AlteracaoCampo("retidoAte", null, textoDe(cliente.getRetidoAte()))), null));
    }

    public void expurgo(Cliente cliente) {
        auditoriaService.registrarDoSistema(new EventoDeAuditoria(TipoOperacao.EXPURGO_CLIENTE,
                TipoEntidade.CLIENTE, cliente.getId(),
                List.of(new AlteracaoCampo("situacao", "INATIVO", "EXPURGADO")), JUSTIFICATIVA_EXPURGO));
    }

    private void registrarDiferencas(TipoOperacao operacao, Cliente cliente, Map<String, String> fotoAnterior) {
        List<AlteracaoCampo> alteracoes = ComparadorDeCampos.diferencas(fotoAnterior, fotografar(cliente));
        if (alteracoes.isEmpty()) {
            return;
        }
        auditoriaService.registrar(new EventoDeAuditoria(operacao, TipoEntidade.CLIENTE, cliente.getId(),
                alteracoes, null));
    }

    private String textoDe(LocalDateTime dataHora) {
        return dataHora == null ? null : dataHora.toString();
    }
}
