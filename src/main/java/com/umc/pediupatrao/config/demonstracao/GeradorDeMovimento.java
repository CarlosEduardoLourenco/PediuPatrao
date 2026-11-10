package com.umc.pediupatrao.config.demonstracao;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.MotivoCancelamento;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.RegistroDeCancelamento;
import com.umc.pediupatrao.entity.RegistroDeSaida;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoSaida;
import com.umc.pediupatrao.service.CalculadoraDePedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeradorDeMovimento {

    public static final int DIAS_DE_HISTORICO = 90;
    public static final int PEDIDOS_EM_ANDAMENTO = 6;

    private static final double TENDENCIA_INICIAL = 0.85;
    private static final double CRESCIMENTO_NO_PERIODO = 0.30;
    private static final double CHANCE_DE_CANCELAMENTO = 0.07;
    private static final double CHANCE_DE_ENTREGA = 0.65;
    private static final double CHANCE_DE_DESCONTO = 0.12;
    private static final double CHANCE_DE_BEBIDA = 0.70;
    private static final String ATENDENTE = "atendente01";
    private static final String GERENTE = "gerente01";
    private static final String OBSERVACAO_DE_OUTRO_MOTIVO = "Endereço fora da área de entrega";

    private static final Map<DayOfWeek, Integer> PEDIDOS_POR_DIA_DA_SEMANA = new EnumMap<>(Map.of(
            DayOfWeek.MONDAY, 9, DayOfWeek.TUESDAY, 8, DayOfWeek.WEDNESDAY, 10, DayOfWeek.THURSDAY, 12,
            DayOfWeek.FRIDAY, 19, DayOfWeek.SATURDAY, 23, DayOfWeek.SUNDAY, 17));

    private record Ponderado<T>(T valor, int peso) {
    }

    private static final List<Ponderado<Integer>> HORAS_DE_MOVIMENTO = List.of(
            new Ponderado<>(18, 1), new Ponderado<>(19, 3), new Ponderado<>(20, 5),
            new Ponderado<>(21, 4), new Ponderado<>(22, 2));

    private static final List<Ponderado<Integer>> QUANTIDADE_DE_PRATOS = List.of(
            new Ponderado<>(1, 10), new Ponderado<>(2, 7), new Ponderado<>(3, 3));

    private static final List<Ponderado<Integer>> PERCENTUAIS_DE_DESCONTO = List.of(
            new Ponderado<>(5, 4), new Ponderado<>(10, 4), new Ponderado<>(15, 2), new Ponderado<>(20, 1));

    private static final List<Ponderado<MotivoCancelamento>> MOTIVOS_DE_CANCELAMENTO = List.of(
            new Ponderado<>(MotivoCancelamento.CLIENTE_DESISTIU, 4),
            new Ponderado<>(MotivoCancelamento.DEMORA_NA_PREPARACAO, 3),
            new Ponderado<>(MotivoCancelamento.ITEM_INDISPONIVEL, 2),
            new Ponderado<>(MotivoCancelamento.ERRO_NO_PEDIDO, 2),
            new Ponderado<>(MotivoCancelamento.PAGAMENTO_RECUSADO, 1),
            new Ponderado<>(MotivoCancelamento.OUTRO, 1));

    private final Random sorteio;
    private final LocalDateTime agora;
    private final List<Ponderado<OpcaoDoCardapio>> pratos;
    private final List<Ponderado<OpcaoDoCardapio>> bebidas;
    private final List<Cliente> clientes;
    private final List<Entregador> entregadores;

    public GeradorDeMovimento(long semente, LocalDateTime agora, List<OpcaoDoCardapio> cardapio,
            List<Cliente> clientes, List<Entregador> entregadores) {
        this.sorteio = new Random(semente);
        this.agora = agora;
        this.pratos = ponderar(cardapio.stream().filter(opcao -> !opcao.ehBebida()).toList());
        this.bebidas = ponderar(cardapio.stream().filter(OpcaoDoCardapio::ehBebida).toList());
        this.clientes = clientes;
        this.entregadores = entregadores;
    }

    public List<Pedido> gerar() {
        List<LocalDateTime> entradas = sortearEntradas();
        int primeiroEmAndamento = Math.max(0, entradas.size() - PEDIDOS_EM_ANDAMENTO);
        List<Pedido> pedidos = new ArrayList<>();
        for (int indice = 0; indice < entradas.size(); indice++) {
            LocalDateTime entrada = entradas.get(indice);
            pedidos.add(indice >= primeiroEmAndamento ? pedidoEmAndamento(entrada) : pedidoEncerrado(entrada));
        }
        return pedidos;
    }

    private List<LocalDateTime> sortearEntradas() {
        LocalDate primeiroDia = agora.toLocalDate().minusDays(DIAS_DE_HISTORICO - 1L);
        List<LocalDateTime> entradas = new ArrayList<>();
        for (int diaDoPeriodo = 0; diaDoPeriodo < DIAS_DE_HISTORICO; diaDoPeriodo++) {
            LocalDate dia = primeiroDia.plusDays(diaDoPeriodo);
            int quantidade = quantidadeDePedidos(dia, diaDoPeriodo);
            for (int pedido = 0; pedido < quantidade; pedido++) {
                LocalDateTime entrada = dia.atTime(sortear(HORAS_DE_MOVIMENTO), sorteio.nextInt(60));
                if (entrada.isBefore(agora)) {
                    entradas.add(entrada);
                }
            }
        }
        entradas.sort(null);
        return entradas;
    }

    private int quantidadeDePedidos(LocalDate dia, int diaDoPeriodo) {
        double tendencia = TENDENCIA_INICIAL + CRESCIMENTO_NO_PERIODO * diaDoPeriodo / (DIAS_DE_HISTORICO - 1);
        double oscilacao = 0.8 + 0.4 * sorteio.nextDouble();
        return (int) Math.round(PEDIDOS_POR_DIA_DA_SEMANA.get(dia.getDayOfWeek()) * tendencia * oscilacao);
    }

    private Pedido pedidoEncerrado(LocalDateTime entrada) {
        Pedido pedido = novoPedido(entrada);
        if (sorteio.nextDouble() < CHANCE_DE_CANCELAMENTO) {
            cancelar(pedido, entrada);
        } else {
            LocalDateTime momento = entrada;
            while (pedido.getStatus() != StatusPedido.FINALIZADO) {
                momento = avancarUmaEtapa(pedido, momento);
            }
        }
        return pedido;
    }

    private Pedido pedidoEmAndamento(LocalDateTime entrada) {
        Pedido pedido = novoPedido(entrada);
        int etapas = sorteio.nextInt(4);
        LocalDateTime momento = entrada;
        for (int etapa = 0; etapa < etapas; etapa++) {
            momento = avancarUmaEtapa(pedido, momento);
        }
        return pedido;
    }

    private Pedido novoPedido(LocalDateTime entrada) {
        List<ItemPedido> itens = sortearItens();
        BigDecimal subtotal = CalculadoraDePedido.somarSubtotais(itens);
        BigDecimal percentual = sorteio.nextDouble() < CHANCE_DE_DESCONTO
                ? BigDecimal.valueOf(sortear(PERCENTUAIS_DE_DESCONTO))
                : BigDecimal.ZERO;
        BigDecimal desconto = CalculadoraDePedido.calcularDesconto(subtotal, percentual);
        Cliente cliente = clientes.get(sorteio.nextInt(clientes.size()));
        String responsavel = sorteio.nextInt(5) == 0 ? GERENTE : ATENDENTE;

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setTipoSaida(sorteio.nextDouble() < CHANCE_DE_ENTREGA ? TipoSaida.ENTREGA : TipoSaida.RETIRADA);
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal);
        pedido.setDescontoPercentual(percentual.setScale(2, RoundingMode.HALF_UP));
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(CalculadoraDePedido.calcularTotal(subtotal, desconto));
        pedido.setDataHoraEntrada(entrada);
        pedido.setUsuarioEntrada(responsavel);
        pedido.passarPara(StatusPedido.RECEBIDO, entrada, responsavel);
        return pedido;
    }

    private List<ItemPedido> sortearItens() {
        Map<OpcaoDoCardapio, Integer> quantidades = new LinkedHashMap<>();
        int pratosNoPedido = sortear(QUANTIDADE_DE_PRATOS);
        for (int prato = 0; prato < pratosNoPedido; prato++) {
            OpcaoDoCardapio opcao = sortear(pratos);
            quantidades.merge(opcao, quantidadeDe(opcao), Integer::sum);
        }
        if (!bebidas.isEmpty() && sorteio.nextDouble() < CHANCE_DE_BEBIDA) {
            quantidades.merge(sortear(bebidas), 1 + sorteio.nextInt(2), Integer::sum);
        }
        return quantidades.entrySet().stream()
                .map(entrada -> CalculadoraDePedido.montarItem(entrada.getKey().produto(), entrada.getValue()))
                .toList();
    }

    private int quantidadeDe(OpcaoDoCardapio opcao) {
        return opcao.vendidaEmUnidades() ? entre(4, 10) : 1 + (sorteio.nextInt(4) == 0 ? 1 : 0);
    }

    private LocalDateTime avancarUmaEtapa(Pedido pedido, LocalDateTime anterior) {
        StatusPedido proximo = pedido.getStatus().proximo(pedido.getTipoSaida()).orElseThrow();
        LocalDateTime momento = limitadoAoAgora(anterior.plusMinutes(minutosAte(proximo, pedido.getTipoSaida())));
        pedido.passarPara(proximo, momento, ATENDENTE);
        if (proximo.registraSaida()) {
            pedido.setSaida(registroDeSaida(proximo, momento));
        }
        return momento;
    }

    private void cancelar(Pedido pedido, LocalDateTime entrada) {
        LocalDateTime momento = sorteio.nextBoolean() ? avancarUmaEtapa(pedido, entrada) : entrada;
        momento = limitadoAoAgora(momento.plusMinutes(entre(3, 15)));
        MotivoCancelamento motivo = sortear(MOTIVOS_DE_CANCELAMENTO);
        String observacao = motivo.exigeObservacao() ? OBSERVACAO_DE_OUTRO_MOTIVO : null;
        pedido.passarPara(StatusPedido.CANCELADO, momento, GERENTE);
        pedido.setCancelamento(new RegistroDeCancelamento(motivo, observacao, GERENTE, momento));
    }

    private RegistroDeSaida registroDeSaida(StatusPedido etapa, LocalDateTime momento) {
        if (etapa != StatusPedido.SAIU_PARA_ENTREGA) {
            return new RegistroDeSaida(momento, ATENDENTE, null, null);
        }
        Entregador entregador = entregadores.get(sorteio.nextInt(entregadores.size()));
        return new RegistroDeSaida(momento, ATENDENTE, entregador.getId(), entregador.getNome());
    }

    private int minutosAte(StatusPedido etapa, TipoSaida tipoSaida) {
        return switch (etapa) {
            case EM_PREPARACAO -> entre(2, 8);
            case PRONTO -> entre(12, 30);
            case SAIU_PARA_ENTREGA -> entre(2, 6);
            case RETIRADO -> entre(3, 15);
            case FINALIZADO -> tipoSaida == TipoSaida.ENTREGA ? entre(15, 35) : entre(1, 3);
            case RECEBIDO, CANCELADO -> 0;
        };
    }

    private LocalDateTime limitadoAoAgora(LocalDateTime momento) {
        return momento.isAfter(agora) ? agora : momento;
    }

    private int entre(int minimo, int maximo) {
        return minimo + sorteio.nextInt(maximo - minimo + 1);
    }

    private <T> T sortear(List<Ponderado<T>> opcoes) {
        int alvo = sorteio.nextInt(opcoes.stream().mapToInt(Ponderado::peso).sum());
        for (Ponderado<T> opcao : opcoes) {
            alvo -= opcao.peso();
            if (alvo < 0) {
                return opcao.valor();
            }
        }
        throw new IllegalStateException("Lista de opções ponderadas vazia");
    }

    private static List<Ponderado<OpcaoDoCardapio>> ponderar(List<OpcaoDoCardapio> opcoes) {
        return opcoes.stream().map(opcao -> new Ponderado<>(opcao, opcao.popularidade())).toList();
    }
}
