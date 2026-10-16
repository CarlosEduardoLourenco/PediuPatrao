package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.Cliente;
import java.time.LocalDateTime;

public class ClienteFormulario {

    private String id;
    private String nome;
    private String telefone;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private boolean termoAceito;
    private LocalDateTime termoAceitoEm;

    public static ClienteFormulario de(Cliente cliente) {
        ClienteFormulario formulario = new ClienteFormulario();
        formulario.id = cliente.getId();
        formulario.nome = cliente.getNome();
        formulario.telefone = cliente.getTelefone();
        formulario.cep = cliente.getCep();
        formulario.logradouro = cliente.getLogradouro();
        formulario.numero = cliente.getNumero();
        formulario.complemento = cliente.getComplemento();
        formulario.bairro = cliente.getBairro();
        formulario.cidade = cliente.getCidade();
        formulario.estado = cliente.getEstado();
        formulario.termoAceitoEm = cliente.getTermoAceitoEm();
        formulario.termoAceito = cliente.getTermoAceitoEm() != null;
        return formulario;
    }

    public ClienteRequisicao paraRequisicao() {
        return new ClienteRequisicao(nome, telefone, cep, logradouro, numero, complemento, bairro, cidade,
                estado, termoAceito);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isTermoAceito() {
        return termoAceito;
    }

    public void setTermoAceito(boolean termoAceito) {
        this.termoAceito = termoAceito;
    }

    public LocalDateTime getTermoAceitoEm() {
        return termoAceitoEm;
    }
}
