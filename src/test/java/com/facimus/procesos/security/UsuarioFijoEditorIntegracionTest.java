package com.facimus.procesos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.facimus.procesos.gestion.model.RolAcceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.gestion.service.UsuarioService;

/** Modo sin JWT con un usuario fijo EDITOR: las reglas de rol de SecurityConfig siguen aplicando. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario-fijo-editor-it;DB_CLOSE_DELAY=-1",
        "seguridad.jwt.habilitado=false",
        "seguridad.usuario-fijo.email=" + UsuarioFijoEditorIntegracionTest.EDITOR
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsuarioFijoEditorIntegracionTest {

    static final String EDITOR = "editor@demo.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProcesoService procesoService;

    private Long procesoId;

    @BeforeAll
    void crearEditorYProceso() {
        Usuario admin = usuarioService.autenticar("admin@demo.com", "admin123");
        Long empresaId = admin.getEmpresa().getId();
        usuarioService.crearColaborador(empresaId, "Editor Demo", EDITOR, "editor123", RolAcceso.EDITOR);
        procesoId = procesoService.crear(empresaId, admin.getId(), "Ventas", "Proceso de ventas", "Comercial")
                .getId();
    }

    @Test
    @DisplayName("Sin token, el EDITOR fijo puede consultar procesos: 200")
    void UsuarioFijoEditor_listarProcesos_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/procesos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin token, el EDITOR fijo no puede borrar procesos: 403")
    void UsuarioFijoEditor_borrarProceso_sinToken_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/v1/procesos/" + procesoId))
                .andExpect(status().isForbidden());
    }
}
