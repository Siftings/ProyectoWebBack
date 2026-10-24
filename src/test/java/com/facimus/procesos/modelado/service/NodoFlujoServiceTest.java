package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.facimus.procesos.modelado.model.Actividad;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.Lane;
import com.facimus.procesos.modelado.model.NodoFlujo;
import com.facimus.procesos.modelado.model.Pool;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

@ExtendWith(MockitoExtension.class)
class NodoFlujoServiceTest {

    private static final Long EMPRESA_ID = 1L;
    private static final Long PROCESO_ID = 10L;
    private static final Long POOL_A = 100L;
    private static final Long POOL_B = 200L;

    @Mock
    private NodoFlujoRepository nodoFlujoRepository;

    @InjectMocks
    private NodoFlujoService nodoFlujoService;

    private void conNodos(NodoFlujo... nodos) {
        when(nodoFlujoRepository.findAllByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaIdAndActivoTrue(
                "Pedido", PROCESO_ID, EMPRESA_ID)).thenReturn(List.of(nodos));
    }

    private static Lane laneEnPool(Long poolId) {
        Pool pool = new Pool();
        pool.setId(poolId);
        Lane lane = new Lane();
        lane.setPool(pool);
        return lane;
    }

    private static EventoMensaje evento(Long id, TipoEventoMensaje tipo, Long poolId) {
        EventoMensaje evento = new EventoMensaje();
        evento.setId(id);
        evento.setNombre("Pedido");
        evento.setTipoEvento(tipo);
        evento.setLane(laneEnPool(poolId));
        return evento;
    }

    private boolean disponible(TipoEventoMensaje tipo, Long poolId, Long excluir) {
        return nodoFlujoService.nombreDisponibleParaEvento("Pedido", PROCESO_ID, EMPRESA_ID, tipo, poolId, excluir);
    }

    @Test
    @DisplayName("Nombre libre: disponible")
    void nombre_libre() {
        conNodos();
        assertTrue(disponible(TipoEventoMensaje.THROW, POOL_A, null));
    }

    @Test
    @DisplayName("Catch con el mismo nombre que un Throw de otro pool: disponible")
    void catch_complementa_throw_de_otro_pool() {
        conNodos(evento(5L, TipoEventoMensaje.THROW, POOL_A));
        assertTrue(disponible(TipoEventoMensaje.CATCH_INTERMEDIO, POOL_B, null));
        assertTrue(disponible(TipoEventoMensaje.CATCH_INICIO, POOL_B, null));
    }

    @Test
    @DisplayName("Throw con el mismo nombre que un Catch de otro pool: disponible")
    void throw_complementa_catch_de_otro_pool() {
        conNodos(evento(5L, TipoEventoMensaje.CATCH_INICIO, POOL_B));
        assertTrue(disponible(TipoEventoMensaje.THROW, POOL_A, null));
    }

    @Test
    @DisplayName("Dos Throw con el mismo nombre: rechazado")
    void dos_throw() {
        conNodos(evento(5L, TipoEventoMensaje.THROW, POOL_A));
        assertFalse(disponible(TipoEventoMensaje.THROW, POOL_B, null));
    }

    @Test
    @DisplayName("Dos Catch con el mismo nombre: rechazado")
    void dos_catch() {
        conNodos(evento(5L, TipoEventoMensaje.CATCH_INICIO, POOL_A));
        assertFalse(disponible(TipoEventoMensaje.CATCH_INTERMEDIO, POOL_B, null));
    }

    @Test
    @DisplayName("Throw y Catch en el mismo pool: rechazado")
    void mismo_pool() {
        conNodos(evento(5L, TipoEventoMensaje.THROW, POOL_A));
        assertFalse(disponible(TipoEventoMensaje.CATCH_INTERMEDIO, POOL_A, null));
    }

    @Test
    @DisplayName("Un tercer evento con el nombre de un par Throw/Catch: rechazado")
    void tercer_evento() {
        conNodos(evento(5L, TipoEventoMensaje.THROW, POOL_A), evento(6L, TipoEventoMensaje.CATCH_INICIO, POOL_B));
        assertFalse(disponible(TipoEventoMensaje.CATCH_INTERMEDIO, 300L, null));
    }

    @Test
    @DisplayName("Nombre usado por una actividad: rechazado")
    void nombre_de_actividad() {
        Actividad actividad = new Actividad();
        actividad.setId(7L);
        actividad.setNombre("Pedido");
        actividad.setLane(laneEnPool(POOL_A));
        conNodos(actividad);
        assertFalse(disponible(TipoEventoMensaje.THROW, POOL_B, null));
    }

    @Test
    @DisplayName("Al editar, el propio evento no cuenta como conflicto")
    void excluye_al_propio_evento() {
        conNodos(evento(5L, TipoEventoMensaje.THROW, POOL_A), evento(6L, TipoEventoMensaje.CATCH_INICIO, POOL_B));
        assertTrue(disponible(TipoEventoMensaje.CATCH_INTERMEDIO, POOL_B, 6L));
    }
}
