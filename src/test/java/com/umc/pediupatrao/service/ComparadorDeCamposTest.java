package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComparadorDeCamposTest {

    @Test
    void deveListarSomenteOsCamposAlterados() {
        Map<String, String> antes = campos("Rua das Flores", "100", "Centro");
        Map<String, String> depois = campos("Av. Brasil", "100", "Centro");

        List<AlteracaoCampo> diferencas = ComparadorDeCampos.diferencas(antes, depois);

        assertEquals(List.of(new AlteracaoCampo("logradouro", "Rua das Flores", "Av. Brasil")), diferencas);
    }

    @Test
    void deveRetornarListaVaziaQuandoNadaMudou() {
        Map<String, String> campos = campos("Rua das Flores", "100", "Centro");

        assertTrue(ComparadorDeCampos.diferencas(campos, campos("Rua das Flores", "100", "Centro")).isEmpty());
    }

    @Test
    void deveTratarTextoVazioEEspacosComoAusenciaDeValor() {
        Map<String, String> antes = campos(null, "100", "Centro");
        Map<String, String> depois = campos("   ", "100", "Centro");

        assertTrue(ComparadorDeCampos.diferencas(antes, depois).isEmpty());
    }

    @Test
    void deveRegistrarValorAnteriorNuloQuandoCampoPassaAExistir() {
        Map<String, String> antes = campos(null, "100", "Centro");
        Map<String, String> depois = campos("Av. Brasil", "100", "Centro");

        List<AlteracaoCampo> diferencas = ComparadorDeCampos.diferencas(antes, depois);

        assertEquals(new AlteracaoCampo("logradouro", null, "Av. Brasil"), diferencas.get(0));
    }

    @Test
    void deveRegistrarValorNovoNuloQuandoCampoForRemovido() {
        Map<String, String> antes = campos("Rua das Flores", "100", "Centro");
        Map<String, String> depois = campos("", "100", "Centro");

        List<AlteracaoCampo> diferencas = ComparadorDeCampos.diferencas(antes, depois);

        assertEquals(new AlteracaoCampo("logradouro", "Rua das Flores", null), diferencas.get(0));
    }

    @Test
    void deveIgnorarEspacosNasPontasAoComparar() {
        Map<String, String> antes = campos("Rua das Flores", "100", "Centro");
        Map<String, String> depois = campos("  Rua das Flores ", "100", "Centro");

        assertTrue(ComparadorDeCampos.diferencas(antes, depois).isEmpty());
    }

    private Map<String, String> campos(String logradouro, String numero, String bairro) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("logradouro", logradouro);
        campos.put("numero", numero);
        campos.put("bairro", bairro);
        return campos;
    }
}
