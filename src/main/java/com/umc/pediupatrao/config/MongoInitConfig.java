package com.umc.pediupatrao.config;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoInitConfig implements CommandLineRunner {

    private static final List<String> COLECOES = List.of(
            "pedidos", "clientes", "usuarios", "produtos", "horariosAtendimento", "auditoria");

    private final MongoTemplate mongoTemplate;

    public MongoInitConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... argumentos) {
        COLECOES.stream()
                .filter(colecao -> !mongoTemplate.collectionExists(colecao))
                .forEach(mongoTemplate::createCollection);
    }
}
