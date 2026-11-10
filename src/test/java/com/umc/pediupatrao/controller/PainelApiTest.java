package com.umc.pediupatrao.controller;

import static com.umc.pediupatrao.apoio.PedidoDeExemplo.entrandoEm;
import static com.umc.pediupatrao.apoio.PedidoDeExemplo.item;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PainelApiTest {

    private static final String OUTUBRO = "de=2026-10-01&ate=2026-10-31";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @BeforeEach
    void registrarVendas() {
        pedidoRepository.save(entrandoEm(LocalDateTime.of(2026, 10, 5, 19, 10)).para(TipoSaida.ENTREGA)
                .com(item("calabresa", "Calabresa", "Pizza", "45.00", 2))
                .com(item("guarana", "Guaraná 2L", "Bebida", "12.00", 1))
                .comDesconto("10.20").preparadoEm(20).atendidoEm(40).montar());
        pedidoRepository.save(entrandoEm(LocalDateTime.of(2026, 10, 9, 20, 30)).para(TipoSaida.RETIRADA)
                .com(item("mussarela", "Mussarela", "Pizza", "40.00", 1))
                .preparadoEm(10).atendidoEm(30).montar());
        pedidoRepository.save(entrandoEm(LocalDateTime.of(2026, 10, 6, 19, 0))
                .com(item("calabresa", "Calabresa", "Pizza", "45.00", 1))
                .canceladoPor(MotivoCancelamento.ERRO_NO_PEDIDO).montar());
    }

    @AfterEach
    void limparDados() {
        pedidoRepository.deleteAll();
    }

    @Test
    void deveEntregarOsIndicadoresDeVendasDoPeriodo() throws Exception {
        comoGerente("/api/painel/indicadores?" + OUTUBRO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.de").value("2026-10-01"))
                .andExpect(jsonPath("$.periodo.ate").value("2026-10-31"))
                .andExpect(jsonPath("$.faturamento").value(131.8))
                .andExpect(jsonPath("$.pedidosFinalizados").value(2))
                .andExpect(jsonPath("$.ticketMedio").value(65.9))
                .andExpect(jsonPath("$.descontosConcedidos").value(10.2))
                .andExpect(jsonPath("$.pedidosCancelados").value(1))
                .andExpect(jsonPath("$.taxaDeCancelamento").value(33.3))
                .andExpect(jsonPath("$.tempoMedioDePreparoEmMinutos").value(15))
                .andExpect(jsonPath("$.tempoMedioDeAtendimentoEmMinutos").value(35))
                .andExpect(jsonPath("$.comparativo.periodoAnterior.de").value("2026-08-31"))
                .andExpect(jsonPath("$.comparativo.variacaoDoFaturamento").isEmpty());
    }

    @Test
    void deveFiltrarOsIndicadoresPeloTipoDeSaida() throws Exception {
        comoGerente("/api/painel/indicadores?" + OUTUBRO + "&tipoSaida=RETIRADA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(40.0))
                .andExpect(jsonPath("$.pedidosFinalizados").value(1));
    }

    @Test
    void deveEntregarASerieSemanalComTodosOsIntervalos() throws Exception {
        comoGerente("/api/painel/vendas/serie?" + OUTUBRO + "&granularidade=SEMANA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularidade").value("SEMANA"))
                .andExpect(jsonPath("$.pontos", hasSize(5)))
                .andExpect(jsonPath("$.pontos[1].inicio").value("2026-10-05"))
                .andExpect(jsonPath("$.pontos[1].faturamento").value(131.8));
    }

    @Test
    void deveUsarASerieDiariaPorPadrao() throws Exception {
        comoGerente("/api/painel/vendas/serie?" + OUTUBRO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granularidade").value("DIA"))
                .andExpect(jsonPath("$.pontos", hasSize(31)));
    }

    @Test
    void deveEntregarADistribuicaoPorDiaDaSemana() throws Exception {
        comoGerente("/api/painel/vendas/distribuicao?" + OUTUBRO + "&dimensao=DIA_DA_SEMANA")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fatias", hasSize(7)))
                .andExpect(jsonPath("$.fatias[0].rotulo").value("Seg"))
                .andExpect(jsonPath("$.fatias[0].pedidos").value(1))
                .andExpect(jsonPath("$.fatias[4].rotulo").value("Sex"))
                .andExpect(jsonPath("$.fatias[4].pedidos").value(1));
    }

    @Test
    void deveEntregarASituacaoDosPedidos() throws Exception {
        comoGerente("/api/painel/pedidos/situacao?" + OUTUBRO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noPeriodo", hasSize(7)))
                .andExpect(jsonPath("$.totalNoPeriodo").value(3))
                .andExpect(jsonPath("$.emAndamentoAgora", hasSize(5)))
                .andExpect(jsonPath("$.totalEmAndamentoAgora").value(0));
    }

    @Test
    void deveEntregarOsCancelamentosPorMotivo() throws Exception {
        comoGerente("/api/painel/pedidos/cancelamentos?" + OUTUBRO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.motivos", hasSize(6)))
                .andExpect(jsonPath("$.motivos[1].motivo").value("ERRO_NO_PEDIDO"))
                .andExpect(jsonPath("$.motivos[1].descricao").value("Erro no registro do pedido"))
                .andExpect(jsonPath("$.motivos[1].quantidade").value(1));
    }

    @Test
    void deveEntregarORankingDeProdutos() throws Exception {
        comoGerente("/api/painel/produtos/mais-vendidos?" + OUTUBRO + "&ordenarPor=FATURAMENTO&limite=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criterio").value("FATURAMENTO"))
                .andExpect(jsonPath("$.produtos", hasSize(2)))
                .andExpect(jsonPath("$.produtos[0].posicao").value(1))
                .andExpect(jsonPath("$.produtos[0].nome").value("Calabresa"))
                .andExpect(jsonPath("$.produtos[0].faturamento").value(90.0))
                .andExpect(jsonPath("$.produtos[1].nome").value("Mussarela"));
    }

    @Test
    void deveFiltrarORankingPorCategoria() throws Exception {
        comoGerente("/api/painel/produtos/mais-vendidos?" + OUTUBRO + "&categoria=Bebida")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Bebida"))
                .andExpect(jsonPath("$.produtos", hasSize(1)))
                .andExpect(jsonPath("$.produtos[0].nome").value("Guaraná 2L"));
    }

    @Test
    void deveEntregarAsOpcoesDeFiltro() throws Exception {
        comoGerente("/api/painel/filtros")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias", hasItem("Pizza")))
                .andExpect(jsonPath("$.tiposSaida", hasSize(2)))
                .andExpect(jsonPath("$.granularidades", hasSize(3)))
                .andExpect(jsonPath("$.diasDoPeriodoMaximo").value(366))
                .andExpect(jsonPath("$.periodoPadrao.ate").isNotEmpty());
    }

    @ParameterizedTest(name = "{0} deve responder 400 com a mensagem \"{1}\"")
    @CsvSource(delimiter = '|', value = {
        "/api/painel/indicadores?de=2026-10-31&ate=2026-10-01|A data inicial (de) não pode ser posterior à data final (ate)",
        "/api/painel/indicadores?de=2025-01-01&ate=2026-10-31|O período consultado pode ter no máximo 366 dias",
        "/api/painel/indicadores?de=2026-13-01|Valor inválido para o parâmetro de",
        "/api/painel/indicadores?tipoSaida=DRONE|Valor inválido para o parâmetro tipoSaida",
        "/api/painel/vendas/serie?granularidade=ANO|Valor inválido para o parâmetro granularidade",
        "/api/painel/vendas/distribuicao?dimensao=CLIENTE|Valor inválido para o parâmetro dimensao",
        "/api/painel/produtos/mais-vendidos?ordenarPor=NOME|Valor inválido para o parâmetro ordenarPor",
        "/api/painel/produtos/mais-vendidos?limite=0|O limite do ranking deve ficar entre 1 e 50",
        "/api/painel/produtos/mais-vendidos?limite=muitos|Valor inválido para o parâmetro limite"
    })
    void deveRecusarParametrosInvalidosComMensagemClara(String caminho, String mensagem) throws Exception {
        comoGerente(caminho)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(mensagem));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveNegarOPainelAosPerfisQueNaoSaoGerente(String perfil) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/painel/indicadores").with(user("teste").roles(perfil)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void deveExigirAutenticacaoParaConsultarOPainel() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/painel/indicadores"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions comoGerente(String caminho) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(caminho).with(user("gerente01").roles("GERENTE")));
    }
}
