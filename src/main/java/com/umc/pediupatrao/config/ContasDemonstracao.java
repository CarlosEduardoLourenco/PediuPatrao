package com.umc.pediupatrao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("pediupatrao.demonstracao")
public record ContasDemonstracao(String senhaAdmin, String senhaGerente, String senhaAtendente) {
}
