package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.HorarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SemeadorDeHorarios implements CommandLineRunner {

    private final HorarioService horarioService;

    public SemeadorDeHorarios(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @Override
    public void run(String... argumentos) {
        horarioService.criarGradePadraoSeVazia();
    }
}
