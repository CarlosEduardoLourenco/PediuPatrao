package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;
    private String clienteId;
    private String clienteNome;
    private TipoSaida tipoSaida;
    private StatusPedido status;
    private List<ItemPedido> itens;
    private BigDecimal subtotal;
    private BigDecimal descontoPercentual;
    private BigDecimal valorDesconto;
    private BigDecimal valorTotal;
    private LocalDateTime dataHoraEntrada;
    private String usuarioEntrada;
    private RegistroDeSaida saida;
    private RegistroDeCancelamento cancelamento;

    public RegistroDeCancelamento getCancelamento() {
        return cancelamento;
    }

    public void setCancelamento(RegistroDeCancelamento cancelamento) {
        this.cancelamento = cancelamento;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public TipoSaida getTipoSaida() {
        return tipoSaida;
    }

    public void setTipoSaida(TipoSaida tipoSaida) {
        this.tipoSaida = tipoSaida;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDescontoPercentual() {
        return descontoPercentual;
    }

    public void setDescontoPercentual(BigDecimal descontoPercentual) {
        this.descontoPercentual = descontoPercentual;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }

    public void setDataHoraEntrada(LocalDateTime dataHoraEntrada) {
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public String getUsuarioEntrada() {
        return usuarioEntrada;
    }

    public void setUsuarioEntrada(String usuarioEntrada) {
        this.usuarioEntrada = usuarioEntrada;
    }

    public RegistroDeSaida getSaida() {
        return saida;
    }

    public void setSaida(RegistroDeSaida saida) {
        this.saida = saida;
    }
}
