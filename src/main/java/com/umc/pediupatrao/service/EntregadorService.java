package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.EntregadorRequisicao;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.repository.EntregadorRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class EntregadorService {

    private final EntregadorRepository entregadorRepository;

    public EntregadorService(EntregadorRepository entregadorRepository) {
        this.entregadorRepository = entregadorRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Entregador> listar() {
        return entregadorRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public List<Entregador> listarAtivos() {
        return entregadorRepository.findAll().stream()
                .filter(entregador -> !Boolean.FALSE.equals(entregador.getAtivo()))
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public Entregador buscar(String id) {
        return entregadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entregador", id));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Entregador criar(EntregadorRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        return entregadorRepository.save(preencher(new Entregador(), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Entregador atualizar(String id, EntregadorRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        return entregadorRepository.save(preencher(buscar(id), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Entregador alternarAtivo(String id) {
        Entregador entregador = buscar(id);
        entregador.setAtivo(Boolean.FALSE.equals(entregador.getAtivo()));
        return entregadorRepository.save(entregador);
    }

    private Entregador preencher(Entregador entregador, EntregadorRequisicao requisicao) {
        entregador.setNome(requisicao.nome().trim());
        entregador.setTelefone(requisicao.telefone().trim());
        entregador.setVeiculo(textoOuNulo(requisicao.veiculo()));
        entregador.setAtivo(!Boolean.FALSE.equals(requisicao.ativo()));
        return entregador;
    }

    private void exigirDadosObrigatorios(EntregadorRequisicao requisicao) {
        if (requisicao.nome() == null || requisicao.nome().isBlank()) {
            throw new DadosInvalidosException("Informe o nome do entregador");
        }
        if (requisicao.telefone() == null || requisicao.telefone().isBlank()) {
            throw new DadosInvalidosException("Informe o telefone do entregador");
        }
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
