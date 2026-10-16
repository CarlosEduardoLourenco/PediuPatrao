package com.umc.pediupatrao.config;

import static com.umc.pediupatrao.entity.Perfil.ADMIN;
import static com.umc.pediupatrao.entity.Perfil.ATENDENTE;
import static com.umc.pediupatrao.entity.Perfil.GERENTE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import com.umc.pediupatrao.security.ManipuladorDeAcessoNegado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String PAGINA_DE_LOGIN = "/login";

    private static final String[] RECURSOS_PUBLICOS = {
        PAGINA_DE_LOGIN, "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**"
    };

    private final ManipuladorDeAcessoNegado manipuladorDeAcessoNegado;

    public SecurityConfig(ManipuladorDeAcessoNegado manipuladorDeAcessoNegado) {
        this.manipuladorDeAcessoNegado = manipuladorDeAcessoNegado;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers(RECURSOS_PUBLICOS).permitAll()
                        .requestMatchers("/usuarios/**", "/api/usuarios/**", "/admin")
                                .hasRole(ADMIN.name())
                        .requestMatchers("/auditoria/**", "/api/auditoria/**")
                                .hasAnyRole(ADMIN.name(), GERENTE.name())
                        .requestMatchers(POST, "/api/pedidos/*/cancelar", "/api/pedidos/*/desconto")
                                .hasRole(GERENTE.name())
                        .requestMatchers(POST, "/api/pedidos", "/api/pedidos/*/avancar")
                                .hasAnyRole(GERENTE.name(), ATENDENTE.name())
                        .requestMatchers(GET, "/pedidos/novo")
                                .hasAnyRole(GERENTE.name(), ATENDENTE.name())
                        .requestMatchers(GET, "/pedidos/**", "/api/pedidos/**", "/produtos", "/api/produtos/**",
                                "/funcionarios", "/api/funcionarios/**", "/entregadores", "/api/entregadores/**",
                                "/horarios", "/api/horarios/**")
                                .hasAnyRole(ADMIN.name(), GERENTE.name(), ATENDENTE.name())
                        .requestMatchers(POST, "/api/clientes/*/inativar")
                                .hasRole(GERENTE.name())
                        .requestMatchers(GET, "/clientes", "/api/clientes/**")
                                .hasAnyRole(GERENTE.name(), ATENDENTE.name())
                        .requestMatchers("/clientes/**", "/api/clientes/**")
                                .hasRole(ATENDENTE.name())
                        .requestMatchers("/produtos/**", "/api/produtos/**", "/funcionarios/**",
                                "/api/funcionarios/**", "/entregadores/**", "/api/entregadores/**",
                                "/horarios/**", "/api/horarios/**")
                                .hasRole(GERENTE.name())
                        .requestMatchers("/", "/configuracoes").authenticated()
                        .anyRequest().denyAll())
                .formLogin(login -> login
                        .loginPage(PAGINA_DE_LOGIN)
                        .permitAll())
                .logout(saida -> saida
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .exceptionHandling(erros -> erros
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                SecurityConfig::ehRequisicaoDeApi)
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint(PAGINA_DE_LOGIN),
                                requisicao -> !ehRequisicaoDeApi(requisicao))
                        .accessDeniedHandler(manipuladorDeAcessoNegado));
        return http.build();
    }

    private static boolean ehRequisicaoDeApi(HttpServletRequest requisicao) {
        return requisicao.getRequestURI().startsWith("/api/");
    }
}
