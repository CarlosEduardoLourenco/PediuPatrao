package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpurgoDeClientes {

    private static final Logger REGISTRO = LoggerFactory.getLogger(ExpurgoDeClientes.class);

    private final ClienteRepository clienteRepository;
    private final ClienteAuditor clienteAuditor;
    private final Clock relogio;

    public ExpurgoDeClientes(ClienteRepository clienteRepository, ClienteAuditor clienteAuditor, Clock relogio) {
        this.clienteRepository = clienteRepository;
        this.clienteAuditor = clienteAuditor;
        this.relogio = relogio;
    }

    @Scheduled(cron = "${pediupatrao.expurgo.cron:0 0 3 * * *}", zone = "America/Sao_Paulo")
    public int expurgarVencidos() {
        List<Cliente> vencidos = clienteRepository
                .findByAtivoFalseAndRetidoAteLessThanEqual(LocalDateTime.now(relogio));
        vencidos.forEach(this::expurgar);
        if (!vencidos.isEmpty()) {
            REGISTRO.info("Clientes expurgados após o prazo de retenção: {}", vencidos.size());
        }
        return vencidos.size();
    }

    private void expurgar(Cliente cliente) {
        clienteAuditor.expurgo(cliente);
        clienteRepository.delete(cliente);
    }
}
