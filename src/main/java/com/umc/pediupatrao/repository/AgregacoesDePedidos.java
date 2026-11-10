package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.dto.painel.CriterioDeRanking;
import com.umc.pediupatrao.dto.painel.DimensaoDeVenda;
import com.umc.pediupatrao.dto.painel.FiltroDoPainel;
import com.umc.pediupatrao.dto.painel.ProdutoVendido;
import com.umc.pediupatrao.dto.painel.TemposMedios;
import com.umc.pediupatrao.dto.painel.TotaisDeVendas;
import com.umc.pediupatrao.dto.painel.VendaAgrupada;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AgregacoesDePedidos {

    private static final String COLECAO_DE_PEDIDOS = "pedidos";
    private static final String COLECAO_DE_PRODUTOS = "produtos";
    private static final String DATA_DE_ENTRADA = "$dataHoraEntrada";
    private static final String SEM_CATEGORIA = "Sem categoria";
    private static final int ESCALA_MONETARIA = 2;
    private static final double SEGUNDOS_POR_MINUTO = 60.0;

    private final MongoTemplate mongoTemplate;
    private final ZoneId fusoDeGravacao;

    public AgregacoesDePedidos(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.fusoDeGravacao = ZoneId.systemDefault();
    }

    public TotaisDeVendas totaisFinalizados(FiltroDoPainel filtro) {
        Document grupo = new Document("_id", null)
                .append("faturamento", somaDe("$valorTotal"))
                .append("pedidos", contagem())
                .append("descontos", somaDe("$valorDesconto"));
        return primeiro(executar(
                etapa("$match", filtroDoPeriodo(filtro, StatusPedido.FINALIZADO)),
                etapa("$group", grupo)))
                .map(resultado -> new TotaisDeVendas(decimal(resultado, "faturamento"),
                        inteiro(resultado, "pedidos"), decimal(resultado, "descontos")))
                .orElseGet(() -> new TotaisDeVendas(zero(), 0, zero()));
    }

    public TemposMedios temposMediosDosFinalizados(FiltroDoPainel filtro) {
        Document duracoes = new Document("preparo",
                        segundosEntre(dataDaPassagem(StatusPedido.EM_PREPARACAO), dataDaPassagem(StatusPedido.PRONTO)))
                .append("atendimento", segundosEntre(DATA_DE_ENTRADA, dataDaPassagem(StatusPedido.FINALIZADO)));
        Document medias = new Document("_id", null)
                .append("preparo", new Document("$avg", "$preparo"))
                .append("atendimento", new Document("$avg", "$atendimento"));
        return primeiro(executar(
                etapa("$match", filtroDoPeriodo(filtro, StatusPedido.FINALIZADO)),
                etapa("$project", duracoes),
                etapa("$group", medias)))
                .map(resultado -> new TemposMedios(minutos(resultado, "preparo"), minutos(resultado, "atendimento")))
                .orElseGet(TemposMedios::semRegistros);
    }

    public List<VendaAgrupada> vendasFinalizadasPorDia(FiltroDoPainel filtro) {
        Document dia = new Document("$dateToString", new Document("format", "%Y-%m-%d")
                .append("date", DATA_DE_ENTRADA)
                .append("timezone", fusoDeGravacao.getId()));
        return vendasFinalizadasAgrupadasPor(filtro, dia);
    }

    public List<VendaAgrupada> vendasFinalizadasPor(DimensaoDeVenda dimensao, FiltroDoPainel filtro) {
        return vendasFinalizadasAgrupadasPor(filtro, expressaoDaDimensao(dimensao));
    }

    public Map<StatusPedido, Long> pedidosPorStatus(FiltroDoPainel filtro) {
        return contarPorStatus(filtroDoPeriodo(filtro, null));
    }

    public Map<StatusPedido, Long> pedidosEmAndamentoPorStatus(FiltroDoPainel filtro) {
        List<String> emAndamento = Arrays.stream(StatusPedido.values())
                .filter(status -> !status.ehFinal())
                .map(StatusPedido::name)
                .toList();
        Document condicao = new Document("status", new Document("$in", emAndamento));
        incluirTipoSaida(condicao, filtro);
        return contarPorStatus(condicao);
    }

    public Map<String, Long> cancelamentosPorMotivo(FiltroDoPainel filtro) {
        Map<String, Long> quantidades = new HashMap<>();
        executar(
                etapa("$match", filtroDoPeriodo(filtro, StatusPedido.CANCELADO)),
                etapa("$group", new Document("_id", "$cancelamento.motivo").append("quantidade", contagem())))
                .forEach(resultado -> quantidades.put(String.valueOf(resultado.get("_id")),
                        inteiro(resultado, "quantidade")));
        return quantidades;
    }

    public List<ProdutoVendido> produtosMaisVendidos(FiltroDoPainel filtro, String categoria,
            CriterioDeRanking criterio, int limite) {
        List<Document> etapas = new ArrayList<>();
        etapas.add(etapa("$match", filtroDoPeriodo(filtro, StatusPedido.FINALIZADO)));
        etapas.add(etapa("$sort", new Document("dataHoraEntrada", 1)));
        etapas.add(etapa("$unwind", "$itens"));
        if (categoria != null) {
            etapas.add(etapa("$match", new Document("itens.categoria", categoria)));
        }
        etapas.add(etapa("$group", new Document("_id", "$itens.produtoId")
                .append("nome", new Document("$last", "$itens.nomeProduto"))
                .append("categoria", new Document("$last", "$itens.categoria"))
                .append("quantidade", somaDe("$itens.quantidade"))
                .append("faturamento", somaDe("$itens.subtotal"))));
        etapas.add(etapa("$sort", ordenacaoDoRanking(criterio)));
        etapas.add(etapa("$limit", limite));

        AtomicInteger posicao = new AtomicInteger();
        return executar(etapas).stream()
                .map(resultado -> new ProdutoVendido(posicao.incrementAndGet(),
                        String.valueOf(resultado.get("_id")),
                        resultado.getString("nome"),
                        categoriaOuPadrao(resultado.getString("categoria")),
                        inteiro(resultado, "quantidade"),
                        decimal(resultado, "faturamento")))
                .toList();
    }

    public List<String> categoriasConhecidas() {
        TreeSet<String> categorias = new TreeSet<>();
        categorias.addAll(mongoTemplate.findDistinct(new Query(), "tipo", COLECAO_DE_PRODUTOS, String.class));
        categorias.addAll(mongoTemplate.findDistinct(new Query(), "itens.categoria", COLECAO_DE_PEDIDOS,
                String.class));
        categorias.removeIf(categoria -> categoria == null || categoria.isBlank());
        return List.copyOf(categorias);
    }

    private List<VendaAgrupada> vendasFinalizadasAgrupadasPor(FiltroDoPainel filtro, Object chave) {
        Document grupo = new Document("_id", chave)
                .append("faturamento", somaDe("$valorTotal"))
                .append("pedidos", contagem());
        return executar(
                etapa("$match", filtroDoPeriodo(filtro, StatusPedido.FINALIZADO)),
                etapa("$group", grupo),
                etapa("$sort", new Document("_id", 1)))
                .stream()
                .map(resultado -> new VendaAgrupada(String.valueOf(resultado.get("_id")),
                        decimal(resultado, "faturamento"), inteiro(resultado, "pedidos")))
                .toList();
    }

    private Object expressaoDaDimensao(DimensaoDeVenda dimensao) {
        Document dataNoFuso = new Document("date", DATA_DE_ENTRADA).append("timezone", fusoDeGravacao.getId());
        return switch (dimensao) {
            case HORA -> new Document("$hour", dataNoFuso);
            case DIA_DA_SEMANA -> new Document("$isoDayOfWeek", dataNoFuso);
            case TIPO_SAIDA -> "$tipoSaida";
        };
    }

    private Map<StatusPedido, Long> contarPorStatus(Document condicao) {
        Map<StatusPedido, Long> quantidades = new HashMap<>();
        executar(
                etapa("$match", condicao),
                etapa("$group", new Document("_id", "$status").append("quantidade", contagem())))
                .forEach(resultado -> quantidades.put(StatusPedido.valueOf(resultado.getString("_id")),
                        inteiro(resultado, "quantidade")));
        return quantidades;
    }

    private Document filtroDoPeriodo(FiltroDoPainel filtro, StatusPedido status) {
        Document condicao = new Document("dataHoraEntrada", new Document("$gte", paraData(filtro.periodo().inicio()))
                .append("$lt", paraData(filtro.periodo().fimExclusivo())));
        if (status != null) {
            condicao.append("status", status.name());
        }
        incluirTipoSaida(condicao, filtro);
        return condicao;
    }

    private void incluirTipoSaida(Document condicao, FiltroDoPainel filtro) {
        if (filtro.tipoSaida() != null) {
            condicao.append("tipoSaida", filtro.tipoSaida().name());
        }
    }

    private Document dataDaPassagem(StatusPedido status) {
        Document passagensDoStatus = new Document("$filter", new Document("input",
                new Document("$ifNull", List.of("$historicoDeStatus", List.of())))
                .append("as", "passagem")
                .append("cond", new Document("$eq", List.of("$$passagem.status", status.name()))));
        return new Document("$let", new Document("vars",
                new Document("primeira", new Document("$arrayElemAt", List.of(passagensDoStatus, 0))))
                .append("in", "$$primeira.dataHora"));
    }

    private Document segundosEntre(Object inicio, Object fim) {
        return new Document("$dateDiff", new Document("startDate", inicio)
                .append("endDate", fim)
                .append("unit", "second"));
    }

    private Document ordenacaoDoRanking(CriterioDeRanking criterio) {
        Document ordenacao = criterio == CriterioDeRanking.FATURAMENTO
                ? new Document("faturamento", -1).append("quantidade", -1)
                : new Document("quantidade", -1).append("faturamento", -1);
        return ordenacao.append("nome", 1);
    }

    private List<Document> executar(Document... etapas) {
        return executar(List.of(etapas));
    }

    private List<Document> executar(List<Document> etapas) {
        return mongoTemplate.getCollection(COLECAO_DE_PEDIDOS).aggregate(etapas).into(new ArrayList<>());
    }

    private Optional<Document> primeiro(List<Document> resultados) {
        return resultados.stream().findFirst();
    }

    private Document etapa(String operador, Object conteudo) {
        return new Document(operador, conteudo);
    }

    private Document somaDe(String campo) {
        return new Document("$sum", campo);
    }

    private Document contagem() {
        return new Document("$sum", 1);
    }

    private Date paraData(LocalDateTime dataHora) {
        return Date.from(dataHora.atZone(fusoDeGravacao).toInstant());
    }

    private BigDecimal decimal(Document resultado, String campo) {
        Object valor = resultado.get(campo);
        BigDecimal numero = switch (valor) {
            case Decimal128 decimal -> decimal.bigDecimalValue();
            case Number outroNumero -> new BigDecimal(outroNumero.toString());
            case null, default -> BigDecimal.ZERO;
        };
        return numero.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private long inteiro(Document resultado, String campo) {
        Object valor = resultado.get(campo);
        return valor instanceof Number numero ? numero.longValue() : 0;
    }

    private Long minutos(Document resultado, String campo) {
        Object valor = resultado.get(campo);
        return valor instanceof Number segundos ? Math.round(segundos.doubleValue() / SEGUNDOS_POR_MINUTO) : null;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(ESCALA_MONETARIA);
    }

    private String categoriaOuPadrao(String categoria) {
        return categoria == null || categoria.isBlank() ? SEM_CATEGORIA : categoria;
    }
}
