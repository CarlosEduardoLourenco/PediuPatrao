package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "clientes")
public class Cliente {

    @Id
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
    private String endereco;
    private Boolean ativo;
    private LocalDateTime inativadoEm;
    private LocalDateTime retidoAte;
    private LocalDateTime termoAceitoEm;

    public boolean estaAtivo() {
        return !Boolean.FALSE.equals(ativo);
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

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getInativadoEm() {
        return inativadoEm;
    }

    public void setInativadoEm(LocalDateTime inativadoEm) {
        this.inativadoEm = inativadoEm;
    }

    public LocalDateTime getRetidoAte() {
        return retidoAte;
    }

    public void setRetidoAte(LocalDateTime retidoAte) {
        this.retidoAte = retidoAte;
    }

    public LocalDateTime getTermoAceitoEm() {
        return termoAceitoEm;
    }

    public void setTermoAceitoEm(LocalDateTime termoAceitoEm) {
        this.termoAceitoEm = termoAceitoEm;
    }
}
