package com.umc.pediupatrao.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {

    private static final ZoneId FUSO_DA_PIZZARIA = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO_DA_PIZZARIA);
    }
}
