package com.facimus.procesos.transaccionalidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.repository.ProcesoRepository;
import com.facimus.procesos.gestion.service.EmpresaOrquestadorService;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.gestion.service.UsuarioService;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.service.EventoMensajeService;
import com.facimus.procesos.modelado.service.LaneService;
import com.facimus.procesos.modelado.service.MensajeService;
import com.facimus.procesos.modelado.service.PoolService;

/**
 * HU-25/HU-27: un mensaje interno exige un Message Throw y un Message Catch con el mismo nombre que el
 * mensaje, en pools distintos del mismo proceso. Antes era imposible porque el nombre de nodo era unico
 * en todo el proceso.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mensaje-interno-it;DB_CLOSE_DELAY=-1")
class MensajeInternoIntegracionTest {

    @Autowired private EmpresaOrquestadorService empresaOrquestadorService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ProcesoRepository procesoRepository;
    @Autowired private ProcesoService procesoService;
    @Autowired private PoolService poolService;
    @Autowired private LaneService laneService;
    @Autowired private RolProcesoService rolProcesoService;
    @Autowired private EventoMensajeService eventoMensajeService;
    @Autowired private MensajeService mensajeService;

    @Test
    @DisplayName("Throw y Catch con el mismo nombre en pools distintos permiten crear el mensaje y publicar")
    void mensaje_interno_completo_se_publica() {
        Empresa empresa = empresaOrquestadorService.registrarConAdministrador(
                "Acme Msg", "900444001", "msg@acme.com", "Admin", "admin-msg@acme.com", "password123");
        Long empresaId = empresa.getId();
        Usuario admin = usuarioService.listarPorEmpresa(empresaId).get(0);
        Proceso proceso = crearProceso(empresa, "Proceso mensaje interno");
        RolProceso rol = rolProcesoService.crear(empresaId, "Rol", "desc");

        Pool poolA = poolService.crear(empresaId, proceso.getId(), "Ventas", TipoParticipante.EMPRESA, false);
        Pool poolB = poolService.crear(empresaId, proceso.getId(), "Cliente", TipoParticipante.CLIENTE, false);
        Lane laneA = laneService.crear(empresaId, poolA.getId(), "Lane A", rol);
        Lane laneB = laneService.crear(empresaId, poolB.getId(), "Lane B", rol);

        EventoMensaje eventoThrow = eventoMensajeService.crear(empresaId, laneA.getId(), "Pedido",
                TipoEventoMensaje.THROW, "datos", "idPedido", 0, 0, false);
        EventoMensaje eventoCatch = eventoMensajeService.crear(empresaId, laneB.getId(), "Pedido",
                TipoEventoMensaje.CATCH_INTERMEDIO, "datos", "idPedido", 0, 0, false);

        Mensaje mensaje = mensajeService.crear(empresaId, proceso.getId(), "Pedido", "datos",
                poolA.getId(), poolB.getId(), eventoThrow.getId(), eventoCatch.getId(), "idPedido",
                null, null, null, null, null);

        Proceso publicado = procesoService.cambiarEstado(empresaId, proceso.getId(), admin.getId(),
                EstadoProceso.PUBLICADO);

        assertEquals(eventoCatch.getId(), mensaje.getEventoCatch().getId());
        assertEquals(EstadoProceso.PUBLICADO, publicado.getEstado());
    }

    @Test
    @DisplayName("Un segundo Throw con el mismo nombre sigue rechazandose")
    void segundo_throw_con_mismo_nombre_falla() {
        Empresa empresa = empresaOrquestadorService.registrarConAdministrador(
                "Acme Msg2", "900444002", "msg2@acme.com", "Admin", "admin-msg2@acme.com", "password123");
        Long empresaId = empresa.getId();
        Proceso proceso = crearProceso(empresa, "Proceso dos throw");
        RolProceso rol = rolProcesoService.crear(empresaId, "Rol", "desc");
        Pool poolA = poolService.crear(empresaId, proceso.getId(), "Ventas", TipoParticipante.EMPRESA, false);
        Pool poolB = poolService.crear(empresaId, proceso.getId(), "Cliente", TipoParticipante.CLIENTE, false);
        Lane laneA = laneService.crear(empresaId, poolA.getId(), "Lane A", rol);
        Lane laneB = laneService.crear(empresaId, poolB.getId(), "Lane B", rol);
        eventoMensajeService.crear(empresaId, laneA.getId(), "Pedido", TipoEventoMensaje.THROW,
                "datos", "idPedido", 0, 0, false);

        assertThrows(ReglaNegocioException.class, () -> eventoMensajeService.crear(empresaId, laneB.getId(),
                "Pedido", TipoEventoMensaje.THROW, "datos", "idPedido", 0, 0, false));
    }

    private Proceso crearProceso(Empresa empresa, String nombre) {
        Proceso proceso = new Proceso();
        proceso.setEmpresa(empresa);
        proceso.setNombre(nombre);
        proceso.setDescripcion("desc");
        proceso.setCategoria("cat");
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setActivo(true);
        proceso.setFechaCreacion(LocalDateTime.now());
        proceso.setFechaModificacion(LocalDateTime.now());
        return procesoRepository.save(proceso);
    }
}
