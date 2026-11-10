package com.umc.pediupatrao.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.Pedido;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class IndicesDoPedidoTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void deveCriarOsIndicesUsadosNasConsultasPorPeriodo() {
        Set<String> nomes = mongoTemplate.indexOps(Pedido.class).getIndexInfo().stream()
                .map(IndexInfo::getName)
                .collect(Collectors.toSet());

        assertTrue(nomes.contains("entrada"), "índice por data de entrada ausente: " + nomes);
        assertTrue(nomes.contains("status_por_entrada"), "índice por status e entrada ausente: " + nomes);
    }
}
