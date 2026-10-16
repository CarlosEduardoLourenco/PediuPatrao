package com.umc.pediupatrao.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcessoPorPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} em {1} {2} deve ser negado")
    @CsvSource({
        "GERENTE, GET, /api/usuarios",
        "ATENDENTE, GET, /api/usuarios",
        "GERENTE, POST, /api/usuarios",
        "ATENDENTE, PUT, /api/usuarios/x",
        "ATENDENTE, DELETE, /api/usuarios/x",
        "GERENTE, GET, /usuarios",
        "ATENDENTE, POST, /usuarios/salvar",
        "ADMIN, GET, /api/clientes",
        "ADMIN, POST, /api/clientes",
        "GERENTE, POST, /api/clientes",
        "GERENTE, PUT, /api/clientes/x",
        "GERENTE, DELETE, /api/clientes/x",
        "ADMIN, GET, /clientes",
        "GERENTE, GET, /clientes/novo",
        "ATENDENTE, POST, /api/pedidos/x/cancelar",
        "ADMIN, POST, /api/pedidos/x/cancelar",
        "ATENDENTE, POST, /api/pedidos/x/desconto",
        "ADMIN, POST, /api/pedidos/x/desconto",
        "ADMIN, POST, /api/pedidos",
        "ADMIN, POST, /api/pedidos/x/avancar",
        "ADMIN, POST, /api/produtos",
        "ATENDENTE, POST, /api/produtos",
        "ATENDENTE, PUT, /api/produtos/x",
        "ADMIN, DELETE, /api/produtos/x",
        "ATENDENTE, GET, /api/auditoria",
        "ATENDENTE, GET, /auditoria",
        "GERENTE, GET, /admin",
        "ADMIN, GET, /pedidos/novo",
        "ADMIN, GET, /produtos/novo",
        "ATENDENTE, GET, /produtos/novo",
        "ATENDENTE, POST, /produtos/salvar",
        "GERENTE, POST, /pedidos/novo",
        "ATENDENTE, POST, /api/clientes/x/inativar",
        "ADMIN, POST, /api/clientes/x/inativar",
        "ATENDENTE, POST, /api/funcionarios",
        "ADMIN, POST, /api/entregadores",
        "ATENDENTE, PUT, /api/horarios",
        "ADMIN, GET, /funcionarios/novo",
        "ATENDENTE, GET, /entregadores/novo",
        "ATENDENTE, POST, /funcionarios/salvar",
        "ATENDENTE, GET, /rota-inexistente"
    })
    void deveNegarAcessoSemPermissao(String perfil, String metodo, String caminho) throws Exception {
        mockMvc.perform(requisicao(metodo, caminho).with(user("teste").roles(perfil)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} em {1} {2} deve responder {3}")
    @CsvSource({
        "ADMIN, GET, /api/usuarios, 200",
        "ADMIN, POST, /api/usuarios, 400",
        "GERENTE, GET, /api/clientes, 200",
        "ATENDENTE, GET, /api/clientes, 200",
        "ADMIN, GET, /api/pedidos, 200",
        "GERENTE, GET, /api/pedidos, 200",
        "ATENDENTE, GET, /api/pedidos, 200",
        "ADMIN, GET, /api/produtos, 200",
        "GERENTE, GET, /api/produtos, 200",
        "ATENDENTE, GET, /api/produtos, 200",
        "ADMIN, GET, /, 200",
        "GERENTE, GET, /, 200",
        "ATENDENTE, GET, /, 200",
        "ADMIN, GET, /usuarios, 200",
        "ADMIN, GET, /usuarios/novo, 200",
        "ADMIN, GET, /usuarios/editar/inexistente, 302",
        "GERENTE, GET, /clientes, 200",
        "ATENDENTE, GET, /clientes, 200",
        "ATENDENTE, GET, /clientes/novo, 200",
        "ADMIN, GET, /pedidos, 200",
        "GERENTE, GET, /pedidos, 200",
        "ATENDENTE, GET, /pedidos, 200",
        "GERENTE, GET, /pedidos/novo, 200",
        "ATENDENTE, GET, /pedidos/novo, 200",
        "ADMIN, GET, /produtos, 200",
        "ATENDENTE, GET, /produtos, 200",
        "GERENTE, GET, /produtos/novo, 200",
        "GERENTE, POST, /api/clientes/inexistente/inativar, 404",
        "ADMIN, GET, /funcionarios, 200",
        "ATENDENTE, GET, /entregadores, 200",
        "GERENTE, GET, /horarios, 200",
        "ATENDENTE, GET, /api/horarios/situacao, 200",
        "GERENTE, GET, /funcionarios/novo, 200",
        "GERENTE, GET, /entregadores/novo, 200"
    })
    void deveLiberarAcessoComPermissao(String perfil, String metodo, String caminho, int statusEsperado)
            throws Exception {
        mockMvc.perform(requisicao(metodo, caminho).with(user("teste").roles(perfil)).with(csrf()))
                .andExpect(status().is(statusEsperado));
    }

    @Test
    void deveExigirAutenticacaoNaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRedirecionarParaLoginNasTelasSemAutenticacao() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deveRejeitarEscritaSemTokenCsrf() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/usuarios")
                        .with(user("teste").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveNegarAcessoAUsuarioComPerfilInexistente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/clientes")
                        .with(user("teste").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "login de {0} recebe o perfil {2}")
    @CsvSource({
        "admin, luffy, ADMIN",
        "gerente01, zoro, GERENTE",
        "atendente01, chopper, ATENDENTE"
    })
    void deveAutenticarCadaPerfilComSuaRole(String usuario, String senha, String perfil) throws Exception {
        mockMvc.perform(formLogin().user(usuario).password(senha))
                .andExpect(authenticated().withRoles(perfil));
    }

    @Test
    void deveRecusarSenhaIncorreta() throws Exception {
        mockMvc.perform(formLogin().user("gerente01").password("incorreta"))
                .andExpect(unauthenticated());
    }

    private MockHttpServletRequestBuilder requisicao(String metodo, String caminho) {
        return MockMvcRequestBuilders.request(HttpMethod.valueOf(metodo), caminho)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
    }
}
