package com.umc.pediupatrao.config.demonstracao;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Entregador;
import com.umc.pediupatrao.entity.Produto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public final class CatalogoDeDemonstracao {

    private static final String CIDADE = "Mogi das Cruzes";
    private static final String ESTADO = "SP";

    private record PratoDoCardapio(String nome, String categoria, double preco, int popularidade) {
    }

    private static final List<PratoDoCardapio> CARDAPIO = List.of(
            new PratoDoCardapio("Pizza Calabresa", "Pizza", 45.0, 10),
            new PratoDoCardapio("Pizza Mussarela", "Pizza", 42.0, 9),
            new PratoDoCardapio("Pizza Portuguesa", "Pizza", 48.0, 7),
            new PratoDoCardapio("Pizza Frango com Catupiry", "Pizza", 50.0, 8),
            new PratoDoCardapio("Pizza Margherita", "Pizza", 46.0, 5),
            new PratoDoCardapio("Pizza Quatro Queijos", "Pizza", 52.0, 4),
            new PratoDoCardapio("Esfiha de Carne", "Esfiha", 7.0, 4),
            new PratoDoCardapio("Esfiha de Queijo", "Esfiha", 7.5, 3),
            new PratoDoCardapio("X-Burguer da Casa", "Lanche", 28.0, 3),
            new PratoDoCardapio("Batata Frita Grande", "Porção", 25.0, 3),
            new PratoDoCardapio("Refrigerante Cola 2L", "Bebida", 14.0, 8),
            new PratoDoCardapio("Guaraná 2L", "Bebida", 12.0, 6),
            new PratoDoCardapio("Suco Natural 500ml", "Bebida", 9.0, 3));

    private static final List<String> NOMES_DE_CLIENTES = List.of(
            "Ana Ribeiro", "Bruno Tavares", "Camila Duarte", "Diego Moreira", "Elaine Castro",
            "Fábio Nogueira", "Gabriela Prado", "Henrique Sales", "Isabela Fontes", "João Pedro Lima",
            "Karina Mendes", "Lucas Barreto", "Mariana Teles", "Nicolas Furtado", "Olívia Campos",
            "Paulo Henrique Rocha", "Renata Siqueira", "Samuel Pires", "Tatiane Freitas", "Vinícius Aguiar");

    private static final List<String> BAIRROS = List.of(
            "Centro", "Mogi Moderno", "Vila Oliveira", "Alto do Ipiranga", "Socorro", "Brás Cubas");

    private static final List<String> NOMES_DE_ENTREGADORES = List.of(
            "Rafael Monteiro", "Sérgio Antunes", "Wagner Lopes");

    private CatalogoDeDemonstracao() {
    }

    public static List<Produto> produtos() {
        return CARDAPIO.stream().map(prato -> {
            Produto produto = new Produto();
            produto.setNome(prato.nome());
            produto.setTipo(prato.categoria());
            produto.setPreco(prato.preco());
            produto.setAtivo(true);
            return produto;
        }).toList();
    }

    public static List<OpcaoDoCardapio> opcoes(List<Produto> produtosSalvos) {
        return produtosSalvos.stream()
                .map(produto -> new OpcaoDoCardapio(produto, popularidade(produto)))
                .toList();
    }

    public static List<Cliente> clientes(LocalDateTime cadastradosEm) {
        return IntStream.range(0, NOMES_DE_CLIENTES.size()).mapToObj(indice -> {
            Cliente cliente = new Cliente();
            cliente.setNome(NOMES_DE_CLIENTES.get(indice));
            cliente.setTelefone(String.format("(11) 9%04d-%04d", 7100 + indice * 37, 2300 + indice * 113));
            cliente.setCep(String.format("0870%d-%03d", indice % 10, 100 + indice * 7));
            cliente.setLogradouro("Rua das Acácias");
            cliente.setNumero(String.valueOf(40 + indice * 11));
            cliente.setBairro(BAIRROS.get(indice % BAIRROS.size()));
            cliente.setCidade(CIDADE);
            cliente.setEstado(ESTADO);
            cliente.setAtivo(true);
            cliente.setTermoAceitoEm(cadastradosEm);
            return cliente;
        }).toList();
    }

    public static List<Entregador> entregadores() {
        return IntStream.range(0, NOMES_DE_ENTREGADORES.size()).mapToObj(indice -> {
            Entregador entregador = new Entregador();
            entregador.setNome(NOMES_DE_ENTREGADORES.get(indice));
            entregador.setTelefone(String.format("(11) 9%04d-%04d", 8800 + indice * 41, 5500 + indice * 97));
            entregador.setVeiculo("Moto");
            entregador.setAtivo(true);
            return entregador;
        }).toList();
    }

    private static int popularidade(Produto produto) {
        return CARDAPIO.stream()
                .filter(prato -> prato.nome().equals(produto.getNome()))
                .mapToInt(PratoDoCardapio::popularidade)
                .findFirst()
                .orElse(1);
    }
}
