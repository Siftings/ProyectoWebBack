package com.facimus.procesos.transaccionalidad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;
import com.facimus.procesos.gestion.model.RolProceso;
import com.facimus.procesos.gestion.model.Usuario;
import com.facimus.procesos.gestion.service.EmpresaOrquestadorService;
import com.facimus.procesos.gestion.service.ProcesoService;
import com.facimus.procesos.gestion.service.RolProcesoService;
import com.facimus.procesos.gestion.service.UsuarioService;
import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.Mensaje;
import com.facimus.procesos.modelado.model.PoliticaFalloNotificacion;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoDestinoExterno;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.model.TipoGateway;
import com.facimus.procesos.modelado.model.TipoParticipante;
import com.facimus.procesos.modelado.service.ActividadService;
import com.facimus.procesos.modelado.service.CorrelacionOrquestadorService;
import com.facimus.procesos.modelado.service.EventoMensajeService;
import com.facimus.procesos.modelado.service.GatewayService;
import com.facimus.procesos.modelado.service.LaneService;
import com.facimus.procesos.modelado.service.MensajeService;
import com.facimus.procesos.modelado.service.PoolOrquestadorService;
import com.facimus.procesos.modelado.service.PoolService;

/** Un proceso PUBLICADO congela su diagrama; sus datos generales siguen editables. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:proceso-publicado-it;DB_CLOSE_DELAY=-1")
class ProcesoPublicadoIntegracionTest {

    private static final String MENSAJE_BLOQUEO = "No se permite modificar el diagrama de un proceso publicado.";
    private static int secuencia = 0;

    @Autowired private EmpresaOrquestadorService empresaOrquestadorService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ProcesoService procesoService;
    @Autowired private RolProcesoService rolProcesoService;
    @Autowired private PoolService poolService;
    @Autowired private PoolOrquestadorService poolOrquestadorService;
    @Autowired private LaneService laneService;
    @Autowired private ActividadService actividadService;
    @Autowired private GatewayService gatewayService;
    @Autowired private EventoMensajeService eventoMensajeService;
    @Autowired private MensajeService mensajeService;
    @Autowired private CorrelacionOrquestadorService correlacionOrquestadorService;

    private Long empresaId;
    private Long usuarioId;
    private Proceso proceso;
    private RolProceso rol;
    private Pool poolA;
    private Pool poolExterno;
    private Lane lane;
    private Actividad actividad;
    private Mensaje mensaje;

    @BeforeEach
    void modeloPublicado() {
        int n = ++secuencia;
        Empresa empresa = empresaOrquestadorService.registrarConAdministrador("Acme Pub " + n, "900555" + n,
                "pub" + n + "@acme.com", "Admin", "admin-pub" + n + "@acme.com", "password123");
        empresaId = empresa.getId();
        Usuario admin = usuarioService.listarPorEmpresa(empresaId).get(0);
        usuarioId = admin.getId();
        proceso = procesoService.crear(empresaId, usuarioId, "Proceso publicado", "desc", "cat");
        rol = rolProcesoService.crear(empresaId, "Rol", "desc");
        poolA = poolService.crear(empresaId, proceso.getId(), "Ventas", TipoParticipante.EMPRESA, false);
        poolExterno = poolService.crear(empresaId, proceso.getId(), "ERP", TipoParticipante.SISTEMA_EXTERNO, true);
        lane = laneService.crear(empresaId, poolA.getId(), "Lane", rol);
        actividad = actividadService.crear(empresaId, lane.getId(), "Tarea", "desc", 0, 0);
        EventoMensaje eventoThrow = eventoMensajeService.crear(empresaId, lane.getId(), "Factura",
                TipoEventoMensaje.THROW, "datos", "idFactura", 0, 0, false);
        mensaje = mensajeService.crear(empresaId, proceso.getId(), "Factura", "datos", poolA.getId(),
                poolExterno.getId(), eventoThrow.getId(), null, "idFactura",
                TipoDestinoExterno.SERVICIO_WEB, "https://erp.example.com",
                PoliticaFalloNotificacion.CONTINUAR_FLUJO, null, null);
        procesoService.cambiarEstado(empresaId, proceso.getId(), usuarioId, EstadoProceso.PUBLICADO);
    }

    @Test
    @DisplayName("Toda modificacion del diagrama de un proceso publicado responde regla de negocio")
    void diagrama_bloqueado() {
        List<Executable> operaciones = List.of(
                () -> poolService.crear(empresaId, proceso.getId(), "Nuevo", TipoParticipante.CLIENTE, false),
                () -> poolService.editar(empresaId, poolA.getId(), "Ventas 2", TipoParticipante.EMPRESA),
                () -> poolOrquestadorService.eliminar(empresaId, poolA.getId()),
                () -> laneService.crear(empresaId, poolA.getId(), "Lane 2", rol),
                () -> laneService.editar(empresaId, lane.getId(), "Lane renombrada", rol),
                () -> laneService.reordenar(empresaId, poolA.getId(), List.of(lane.getId())),
                () -> laneService.eliminar(empresaId, lane.getId()),
                () -> actividadService.crear(empresaId, lane.getId(), "Otra", "desc", 0, 0),
                () -> actividadService.editar(empresaId, actividad.getId(), "Tarea 2", "desc", 0, 0, null),
                () -> actividadService.eliminar(empresaId, actividad.getId()),
                () -> gatewayService.crear(empresaId, lane.getId(), "Decision", TipoGateway.EXCLUSIVO, 0, 0),
                () -> eventoMensajeService.crear(empresaId, lane.getId(), "Aviso", TipoEventoMensaje.THROW,
                        "datos", "id", 0, 0, false),
                () -> mensajeService.editar(empresaId, mensaje.getId(), "Factura", "nuevo contenido"),
                () -> mensajeService.eliminar(empresaId, mensaje.getId()),
                () -> correlacionOrquestadorService.definir(empresaId, mensaje.getId(), "idFactura"),
                () -> correlacionOrquestadorService.eliminar(empresaId, mensaje.getId()));

        for (Executable operacion : operaciones) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, operacion);
            assertEquals(MENSAJE_BLOQUEO, ex.getMessage());
        }
    }

    @Test
    @DisplayName("Los datos generales de un proceso publicado siguen editables")
    void datos_editables() {
        Proceso editado = procesoService.editarDatos(empresaId, proceso.getId(), usuarioId, "Proceso renombrado",
                "otra desc", "otra cat");

        assertEquals("Proceso renombrado", editado.getNombre());
        assertEquals(EstadoProceso.PUBLICADO, editado.getEstado());
    }
}
