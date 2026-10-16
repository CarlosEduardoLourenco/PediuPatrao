package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.EventoDeAuditoria;
import com.umc.pediupatrao.dto.FiltroAuditoria;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import com.umc.pediupatrao.security.UsuarioAutenticado;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private static final String USUARIO_DO_SISTEMA = "sistema";
    private static final Set<String> TERMOS_SENSIVEIS = Set.of("password", "senha", "token");
    private static final int TAMANHO_PADRAO_PAGINA = 20;
    private static final int TAMANHO_MAXIMO_PAGINA = 100;

    private final AuditoriaRepository repositorio;
    private final MongoTemplate mongoTemplate;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Clock relogio;

    public AuditoriaService(AuditoriaRepository repositorio, MongoTemplate mongoTemplate,
            UsuarioAutenticado usuarioAutenticado, Clock relogio) {
        this.repositorio = repositorio;
        this.mongoTemplate = mongoTemplate;
        this.usuarioAutenticado = usuarioAutenticado;
        this.relogio = relogio;
    }

    public void registrar(EventoDeAuditoria evento) {
        gravar(usuarioAutenticado.nome(), usuarioAutenticado.perfil(), evento);
    }

    public void registrarDoSistema(EventoDeAuditoria evento) {
        gravar(USUARIO_DO_SISTEMA, null, evento);
    }

    private void gravar(String usuario, Perfil perfil, EventoDeAuditoria evento) {
        List<AlteracaoCampo> alteracoesSeguras = evento.alteracoes().stream()
                .filter(alteracao -> !ehCampoSensivel(alteracao.campo()))
                .toList();
        repositorio.save(new RegistroAuditoria(null, usuario, perfil, LocalDateTime.now(relogio),
                evento.operacao(), evento.entidade(), evento.entidadeId(), alteracoesSeguras,
                evento.justificativa()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Page<RegistroAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamanho) {
        Pageable paginacao = PageRequest.of(Math.max(pagina, 0), tamanhoValido(tamanho),
                Sort.by(Sort.Direction.DESC, "dataHora"));
        Query consulta = montarConsulta(filtro);
        long total = mongoTemplate.count(consulta, RegistroAuditoria.class);
        List<RegistroAuditoria> registros = mongoTemplate.find(consulta.with(paginacao), RegistroAuditoria.class);
        return new PageImpl<>(registros, paginacao, total);
    }

    private Query montarConsulta(FiltroAuditoria filtro) {
        List<Criteria> condicoes = new ArrayList<>();
        if (filtro.entidade() != null) {
            condicoes.add(Criteria.where("entidade").is(filtro.entidade()));
        }
        if (possuiTexto(filtro.entidadeId())) {
            condicoes.add(Criteria.where("entidadeId").is(filtro.entidadeId().trim()));
        }
        if (possuiTexto(filtro.usuario())) {
            condicoes.add(Criteria.where("usuario").is(filtro.usuario().trim()));
        }
        if (filtro.de() != null) {
            condicoes.add(Criteria.where("dataHora").gte(filtro.de().atStartOfDay()));
        }
        if (filtro.ate() != null) {
            condicoes.add(Criteria.where("dataHora").lt(filtro.ate().plusDays(1).atStartOfDay()));
        }
        return condicoes.isEmpty() ? new Query() : new Query(new Criteria().andOperator(condicoes));
    }

    private int tamanhoValido(int tamanho) {
        return tamanho < 1 ? TAMANHO_PADRAO_PAGINA : Math.min(tamanho, TAMANHO_MAXIMO_PAGINA);
    }

    private boolean ehCampoSensivel(String campo) {
        String normalizado = campo.toLowerCase(Locale.ROOT);
        return TERMOS_SENSIVEIS.stream().anyMatch(normalizado::contains);
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
