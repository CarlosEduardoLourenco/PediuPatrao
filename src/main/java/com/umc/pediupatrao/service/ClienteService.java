package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ClienteRequisicao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    static final long ANOS_DE_RETENCAO = 1;

    private final ClienteRepository clienteRepository;
    private final ClienteAuditor clienteAuditor;
    private final Clock relogio;

    public ClienteService(ClienteRepository clienteRepository, ClienteAuditor clienteAuditor, Clock relogio) {
        this.clienteRepository = clienteRepository;
        this.clienteAuditor = clienteAuditor;
        this.relogio = relogio;
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public List<Cliente> listarAtivos() {
        return clienteRepository.findAll().stream().filter(Cliente::estaAtivo).toList();
    }

    @PreAuthorize("hasAnyRole('GERENTE', 'ATENDENTE')")
    public Cliente buscar(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente criar(ClienteRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        if (!requisicao.aceitouOTermo()) {
            throw new DadosInvalidosException("É necessário o aceite do termo de uso para cadastrar o cliente");
        }
        Cliente novo = preencher(new Cliente(), requisicao);
        novo.setAtivo(true);
        novo.setTermoAceitoEm(LocalDateTime.now(relogio));

        Cliente cliente = clienteRepository.save(novo);
        clienteAuditor.criacao(cliente);
        return cliente;
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente atualizar(String id, ClienteRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        Cliente existente = buscar(id);
        exigirClienteAtivo(existente);
        Map<String, String> fotoAnterior = clienteAuditor.fotografar(existente);

        preencher(existente, requisicao);
        if (requisicao.aceitouOTermo() && existente.getTermoAceitoEm() == null) {
            existente.setTermoAceitoEm(LocalDateTime.now(relogio));
        }
        Cliente atualizado = clienteRepository.save(existente);
        clienteAuditor.alteracao(atualizado, fotoAnterior);
        return atualizado;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Cliente inativar(String id) {
        Cliente cliente = buscar(id);
        exigirClienteAtivo(cliente);
        LocalDateTime agora = LocalDateTime.now(relogio);

        cliente.setAtivo(false);
        cliente.setInativadoEm(agora);
        cliente.setRetidoAte(agora.plusYears(ANOS_DE_RETENCAO));
        Cliente inativado = clienteRepository.save(cliente);
        clienteAuditor.inativacao(inativado);
        return inativado;
    }

    private void exigirClienteAtivo(Cliente cliente) {
        if (!cliente.estaAtivo()) {
            throw new RegraNegocioException("Cliente inativo não pode ser alterado");
        }
    }

    private Cliente preencher(Cliente cliente, ClienteRequisicao requisicao) {
        cliente.setNome(requisicao.nome().trim());
        cliente.setTelefone(requisicao.telefone().trim());
        cliente.setCep(limpar(requisicao.cep()));
        cliente.setLogradouro(limpar(requisicao.logradouro()));
        cliente.setNumero(limpar(requisicao.numero()));
        cliente.setComplemento(limpar(requisicao.complemento()));
        cliente.setBairro(limpar(requisicao.bairro()));
        cliente.setCidade(limpar(requisicao.cidade()));
        cliente.setEstado(limpar(requisicao.estado()));
        return cliente;
    }

    private void exigirDadosObrigatorios(ClienteRequisicao requisicao) {
        if (!possuiTexto(requisicao.nome())) {
            throw new DadosInvalidosException("Informe o nome do cliente");
        }
        if (!possuiTexto(requisicao.telefone())) {
            throw new DadosInvalidosException("Informe o telefone do cliente");
        }
    }

    private String limpar(String valor) {
        return possuiTexto(valor) ? valor.trim() : null;
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
