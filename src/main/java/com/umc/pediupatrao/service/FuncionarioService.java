package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FuncionarioRequisicao;
import com.umc.pediupatrao.entity.Funcionario;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.repository.FuncionarioRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Funcionario> listar() {
        return funcionarioRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public Funcionario buscar(String id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Funcionario criar(FuncionarioRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        return funcionarioRepository.save(preencher(new Funcionario(), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Funcionario atualizar(String id, FuncionarioRequisicao requisicao) {
        exigirDadosObrigatorios(requisicao);
        return funcionarioRepository.save(preencher(buscar(id), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Funcionario alternarAtivo(String id) {
        Funcionario funcionario = buscar(id);
        funcionario.setAtivo(Boolean.FALSE.equals(funcionario.getAtivo()));
        return funcionarioRepository.save(funcionario);
    }

    private Funcionario preencher(Funcionario funcionario, FuncionarioRequisicao requisicao) {
        funcionario.setNome(requisicao.nome().trim());
        funcionario.setCargo(requisicao.cargo().trim());
        funcionario.setTelefone(textoOuNulo(requisicao.telefone()));
        funcionario.setAtivo(!Boolean.FALSE.equals(requisicao.ativo()));
        return funcionario;
    }

    private void exigirDadosObrigatorios(FuncionarioRequisicao requisicao) {
        if (requisicao.nome() == null || requisicao.nome().isBlank()) {
            throw new DadosInvalidosException("Informe o nome do funcionário");
        }
        if (requisicao.cargo() == null || requisicao.cargo().isBlank()) {
            throw new DadosInvalidosException("Informe o cargo do funcionário");
        }
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
