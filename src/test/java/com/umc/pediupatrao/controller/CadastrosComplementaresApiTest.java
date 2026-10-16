package com.umc.pediupatrao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.umc.pediupatrao.repository.EntregadorRepository;
import com.umc.pediupatrao.repository.FuncionarioRepository;
import com.umc.pediupatrao.repository.HorarioAtendimentoRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CadastrosComplementaresApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private EntregadorRepository entregadorRepository;

    @Autowired
    private HorarioAtendimentoRepository horarioRepository;

    @BeforeEach
    void preparar() {
        limpar();
    }

    @AfterEach
    void limpar() {
        funcionarioRepository.deleteAll();
        entregadorRepository.deleteAll();
        horarioRepository.deleteAll();
    }

    @Test
    void deveCadastrarFuncionarioComoGerenteELiberarConsultaAosDemais() throws Exception {
        String resposta = executar("POST", "/api/funcionarios", "gerente01", "GERENTE",
                "{\"nome\": \"Ana\", \"cargo\": \"Pizzaiola\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.id");

        for (String perfil : List.of("ADMIN", "GERENTE", "ATENDENTE")) {
            executar("GET", "/api/funcionarios", "teste", perfil, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nome").value("Ana"));
            executar("GET", "/api/funcionarios/" + id, "teste", perfil, null).andExpect(status().isOk());
        }
    }

    @Test
    void deveNegarEscritaDeFuncionarioAAdminEAtendente() throws Exception {
        for (String perfil : List.of("ADMIN", "ATENDENTE")) {
            executar("POST", "/api/funcionarios", "teste", perfil, "{\"nome\": \"Ana\", \"cargo\": \"Caixa\"}")
                    .andExpect(status().isForbidden());
        }
        assertEquals(0, funcionarioRepository.count());
    }

    @Test
    void deveCadastrarEAtualizarEntregadorComoGerente() throws Exception {
        String resposta = executar("POST", "/api/entregadores", "gerente01", "GERENTE",
                "{\"nome\": \"Carlos\", \"telefone\": \"(11) 98888-0000\", \"veiculo\": \"Moto\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.id");

        executar("PUT", "/api/entregadores/" + id, "gerente01", "GERENTE",
                "{\"nome\": \"Carlos Souza\", \"telefone\": \"(11) 98888-0000\", \"ativo\": false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Souza"))
                .andExpect(jsonPath("$.ativo").value(false));
        executar("POST", "/api/entregadores", "atendente01", "ATENDENTE",
                "{\"nome\": \"X\", \"telefone\": \"1\"}").andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarDadosInvalidosNosCadastros() throws Exception {
        executar("POST", "/api/funcionarios", "gerente01", "GERENTE", "{\"nome\": \"Ana\"}")
                .andExpect(status().isBadRequest());
        executar("POST", "/api/entregadores", "gerente01", "GERENTE", "{\"nome\": \"Carlos\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCriarGradeDeHorariosEPermitirAoGerenteAtualizar() throws Exception {
        executar("GET", "/api/horarios", "atendente01", "ATENDENTE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        executar("PUT", "/api/horarios", "gerente01", "GERENTE", gradeJson("false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].diaDaSemana").value("MONDAY"))
                .andExpect(jsonPath("$[0].aberto").value(false))
                .andExpect(jsonPath("$[1].aberto").value(true))
                .andExpect(jsonPath("$[1].abertura").value("18:00:00"));
    }

    @Test
    void deveNegarAlteracaoDeHorariosAAdminEAtendente() throws Exception {
        for (String perfil : List.of("ADMIN", "ATENDENTE")) {
            executar("PUT", "/api/horarios", "teste", perfil, gradeJson("true")).andExpect(status().isForbidden());
        }
    }

    @Test
    void deveRecusarGradeIncompleta() throws Exception {
        executar("PUT", "/api/horarios", "gerente01", "GERENTE",
                "[{\"diaDaSemana\": \"MONDAY\", \"aberto\": true, \"abertura\": \"18:00\", \"fechamento\": \"23:00\"}]")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveInformarASituacaoDeFuncionamentoATodosOsPerfis() throws Exception {
        for (String perfil : List.of("ADMIN", "GERENTE", "ATENDENTE")) {
            executar("GET", "/api/horarios/situacao", "teste", perfil, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensagem").isNotEmpty());
        }
    }

    @Test
    void deveRenderizarAsTelasConformeOPerfil() throws Exception {
        executar("PUT", "/api/horarios", "gerente01", "GERENTE", gradeJson("true")).andExpect(status().isOk());

        executar("GET", "/funcionarios", "atendente01", "ATENDENTE", null)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Novo Funcionário"))));
        executar("GET", "/funcionarios", "gerente01", "GERENTE", null)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Novo Funcionário")));
        executar("GET", "/entregadores", "gerente01", "GERENTE", null)
                .andExpect(content().string(containsString("Novo Entregador")));
        executar("GET", "/entregadores/novo", "atendente01", "ATENDENTE", null).andExpect(status().isForbidden());
        executar("GET", "/horarios", "gerente01", "GERENTE", null)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Segunda-feira")))
                .andExpect(content().string(containsString("Salvar horários")));
        executar("GET", "/horarios", "atendente01", "ATENDENTE", null)
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Salvar horários"))));
        executar("GET", "/", "atendente01", "ATENDENTE", null)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("agora")));
    }

    private String gradeJson(String segundaAberta) {
        String[] dias = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        StringBuilder grade = new StringBuilder("[");
        for (int posicao = 0; posicao < dias.length; posicao++) {
            boolean aberto = posicao != 0 || Boolean.parseBoolean(segundaAberta);
            grade.append(posicao == 0 ? "" : ",")
                    .append("{\"diaDaSemana\": \"").append(dias[posicao]).append("\", \"aberto\": ").append(aberto)
                    .append(", \"abertura\": \"18:00\", \"fechamento\": \"23:00\"}");
        }
        return grade.append("]").toString();
    }

    private ResultActions executar(String metodo, String caminho, String usuario, String perfil, String corpo)
            throws Exception {
        MockHttpServletRequestBuilder requisicao = MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), caminho)
                .with(user(usuario).roles(perfil))
                .with(csrf());
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mockMvc.perform(requisicao);
    }
}
