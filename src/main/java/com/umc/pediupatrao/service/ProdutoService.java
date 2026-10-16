package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ProdutoRequisicao;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.DadosInvalidosException;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listar() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarAtivos() {
        return produtoRepository.findAll().stream()
                .filter(produto -> Boolean.TRUE.equals(produto.getAtivo()))
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public Produto buscar(String id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto criar(ProdutoRequisicao requisicao) {
        exigirDadosValidos(requisicao);
        return produtoRepository.save(preencher(new Produto(), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto atualizar(String id, ProdutoRequisicao requisicao) {
        exigirDadosValidos(requisicao);
        return produtoRepository.save(preencher(buscar(id), requisicao));
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Produto alternarAtivo(String id) {
        Produto produto = buscar(id);
        produto.setAtivo(!Boolean.TRUE.equals(produto.getAtivo()));
        return produtoRepository.save(produto);
    }

    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        produtoRepository.delete(buscar(id));
    }

    private Produto preencher(Produto produto, ProdutoRequisicao requisicao) {
        produto.setNome(requisicao.nome().trim());
        produto.setTipo(requisicao.tipo().trim());
        produto.setPreco(requisicao.preco());
        produto.setAtivo(!Boolean.FALSE.equals(requisicao.ativo()));
        return produto;
    }

    private void exigirDadosValidos(ProdutoRequisicao requisicao) {
        if (requisicao.nome() == null || requisicao.nome().isBlank()) {
            throw new DadosInvalidosException("Informe o nome do produto");
        }
        if (requisicao.tipo() == null || requisicao.tipo().isBlank()) {
            throw new DadosInvalidosException("Informe o tipo do produto");
        }
        if (requisicao.preco() == null || requisicao.preco() <= 0) {
            throw new DadosInvalidosException("O preço do produto deve ser maior que zero");
        }
    }
}
