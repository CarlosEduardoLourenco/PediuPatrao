package com.umc.pediupatrao.config.demonstracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.apoio.PedidoDeExemplo;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SemeadorDeDemonstracaoTest {

    private static final Clock NOITE_DE_SEXTA = Clock.fixed(Instant.parse("2026-11-07T00:30:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    private SemeadorDeDemonstracao semeador;

    @BeforeEach
    void preparar() {
        limparDados();
        semeador = new SemeadorDeDemonstracao(produtoRepository, clienteRepository, entregadorRepository,
                pedidoRepository, NOITE_DE_SEXTA);
    }

    @AfterEach
    void limparDados() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        clienteRepository.deleteAll();
        entregadorRepository.deleteAll();
    }

    @Test
    void devePopularABaseVaziaComCardapioClientesEntregadoresEPedidos() {
        semeador.run();

        assertEquals(CatalogoDeDemonstracao.produtos().size(), produtoRepository.count());
        assertEquals(20, clienteRepository.count());
        assertEquals(3, entregadorRepository.count());
        assertTrue(pedidoRepository.count() > 900);
    }

    @Test
    void deveVincularOsPedidosAosRegistrosSalvos() {
        semeador.run();

        pedidoRepository.findAll().stream().limit(50).forEach(pedido -> {
            assertTrue(clienteRepository.existsById(pedido.getClienteId()));
            pedido.getItens().forEach(item -> assertTrue(produtoRepository.existsById(item.produtoId())));
        });
    }

    @Test
    void naoDeveDuplicarOsDadosAoReiniciarAAplicacao() {
        semeador.run();
        long pedidosDaPrimeiraCarga = pedidoRepository.count();

        semeador.run();

        assertEquals(pedidosDaPrimeiraCarga, pedidoRepository.count());
        assertEquals(CatalogoDeDemonstracao.produtos().size(), produtoRepository.count());
    }

    @Test
    void naoDeveMisturarDadosFicticiosComPedidosJaExistentes() {
        pedidoRepository.save(PedidoDeExemplo.entrandoEm(LocalDateTime.of(2026, 11, 6, 20, 0))
                .com(PedidoDeExemplo.item("p1", "Pizza da Casa", "Pizza", "40.00", 1)).montar());

        semeador.run();

        assertEquals(1, pedidoRepository.count());
        assertEquals(0, produtoRepository.count());
    }
}
