package com.facimus.procesos.security;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.gestion.service.UsuarioService;

/** Modo sin JWT (seguridad.jwt.habilitado=false): sin token, todo se autentica como el usuario fijo por defecto. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario-fijo-it;DB_CLOSE_DELAY=-1",
        "seguridad.jwt.habilitado=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsuarioFijoIntegracionTest {

    private static final String CLAVE_DEMO = "admin123";
    private static final String PROCESO_DEMO = "Ventas Demo";
    private static final String PROCESO_BETA = "Compras Beta";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProcesoService procesoService;

    @BeforeAll
    void crearProcesosEnDosEmpresas() {
        crearProceso("admin@demo.com", PROCESO_DEMO);
        crearProceso("admin@beta.com", PROCESO_BETA);
    }

    @Test
    @DisplayName("Sin token responde 200 y solo lista procesos de la empresa del usuario fijo")
    void UsuarioFijo_listarProcesos_sinToken_devuelveSoloSuEmpresa() throws Exception {
        mockMvc.perform(get("/api/v1/procesos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nombre").value(hasItem(PROCESO_DEMO)))
                .andExpect(jsonPath("$.content[*].nombre").value(not(hasItem(PROCESO_BETA))));
    }

    private void crearProceso(String emailAdmin, String nombre) {
        Usuario admin = usuarioService.autenticar(emailAdmin, CLAVE_DEMO);
        procesoService.crear(admin.getEmpresa().getId(), admin.getId(), nombre, "Proceso de prueba", "Pruebas");
    }
}
