package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.EventoDeAuditoria;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoEntidade;
import com.umc.pediupatrao.entity.TipoOperacao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PedidoAuditor {

    private final AuditoriaService auditoriaService;

    public PedidoAuditor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    public void criacao(Pedido pedido) {
        registrar(TipoOperacao.CRIACAO_PEDIDO, pedido, List.of(
                new AlteracaoCampo("status", null, pedido.getStatus().name()),
                new AlteracaoCampo("valorTotal", null, texto(pedido.getValorTotal())),
                new AlteracaoCampo("quantidadeDeItens", null, String.valueOf(pedido.getItens().size()))), null);
    }

    public void mudancaDeStatus(Pedido pedido, StatusPedido statusAnterior) {
        List<AlteracaoCampo> alteracoes = new ArrayList<>();
        alteracoes.add(new AlteracaoCampo("status", statusAnterior.name(), pedido.getStatus().name()));
        if (pedido.getSaida() != null && pedido.getSaida().possuiEntregador()) {
            alteracoes.add(new AlteracaoCampo("entregador", null, pedido.getSaida().entregadorNome()));
        }
        registrar(TipoOperacao.MUDANCA_STATUS, pedido, alteracoes, null);
    }

    public void desconto(Pedido pedido, BigDecimal percentualAnterior, BigDecimal totalAnterior) {
        registrar(TipoOperacao.DESCONTO, pedido, List.of(
                new AlteracaoCampo("descontoPercentual", texto(percentualAnterior),
                        texto(pedido.getDescontoPercentual())),
                new AlteracaoCampo("valorTotal", texto(totalAnterior), texto(pedido.getValorTotal()))), null);
    }

    public void cancelamento(Pedido pedido, StatusPedido statusAnterior) {
        registrar(TipoOperacao.CANCELAMENTO, pedido, List.of(
                new AlteracaoCampo("status", statusAnterior.name(), pedido.getStatus().name())),
                pedido.getCancelamento().justificativa());
    }

    private void registrar(TipoOperacao operacao, Pedido pedido, List<AlteracaoCampo> alteracoes,
            String justificativa) {
        auditoriaService.registrar(new EventoDeAuditoria(operacao, TipoEntidade.PEDIDO, pedido.getId(),
                alteracoes, justificativa));
    }

    private String texto(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
