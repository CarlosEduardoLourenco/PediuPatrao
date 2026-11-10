package com.umc.pediupatrao.config.demonstracao;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class SemeadorDeDemonstracao implements CommandLineRunner {

    static final long SEMENTE_DO_SORTEIO = 20261109L;

    private static final Logger REGISTRO = LoggerFactory.getLogger(SemeadorDeDemonstracao.class);

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final EntregadorRepository entregadorRepository;
    private final PedidoRepository pedidoRepository;
    private final Clock relogio;

    public SemeadorDeDemonstracao(ProdutoRepository produtoRepository, ClienteRepository clienteRepository,
            EntregadorRepository entregadorRepository, PedidoRepository pedidoRepository, Clock relogio) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.entregadorRepository = entregadorRepository;
        this.pedidoRepository = pedidoRepository;
        this.relogio = relogio;
    }

    @Override
    public void run(String... argumentos) {
        if (pedidoRepository.count() > 0 || produtoRepository.count() > 0) {
            REGISTRO.info("A base de demonstração já possui dados; nenhum registro foi gerado");
            return;
        }
        LocalDateTime agora = LocalDateTime.now(relogio);
        List<Produto> produtos = produtoRepository.saveAll(CatalogoDeDemonstracao.produtos());
        List<Cliente> clientes = clienteRepository.saveAll(
                CatalogoDeDemonstracao.clientes(agora.minusDays(GeradorDeMovimento.DIAS_DE_HISTORICO)));
        List<Entregador> entregadores = entregadorRepository.saveAll(CatalogoDeDemonstracao.entregadores());
        List<Pedido> pedidos = pedidoRepository.saveAll(new GeradorDeMovimento(SEMENTE_DO_SORTEIO, agora,
                CatalogoDeDemonstracao.opcoes(produtos), clientes, entregadores).gerar());

        REGISTRO.info("Base de demonstração criada: {} produtos, {} clientes, {} entregadores e {} pedidos",
                produtos.size(), clientes.size(), entregadores.size(), pedidos.size());
    }
}
