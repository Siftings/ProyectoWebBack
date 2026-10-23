package com.facimus.procesos.modelado.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;

class ReglasModeladoTest {

    private static Proceso proceso(boolean activo, EstadoProceso estado) {
        Proceso proceso = new Proceso();
        proceso.setActivo(activo);
        proceso.setEstado(estado);
        return proceso;
    }

    @Test
    @DisplayName("Proceso activo en borrador: se puede modelar")
    void borrador_activo_permite_modelar() {
        assertDoesNotThrow(() -> ReglasModelado.validarProcesoModelable(proceso(true, EstadoProceso.BORRADOR)));
    }

    @Test
    @DisplayName("Proceso inactivo: no se puede modelar")
    void inactivo_bloquea() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ReglasModelado.validarProcesoModelable(proceso(false, EstadoProceso.BORRADOR)));
        assertEquals("No se permite modelar en un proceso inactivo.", ex.getMessage());
    }

    @Test
    @DisplayName("Proceso publicado: el diagrama queda bloqueado")
    void publicado_bloquea() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ReglasModelado.validarProcesoModelable(proceso(true, EstadoProceso.PUBLICADO)));
        assertEquals("No se permite modificar el diagrama de un proceso publicado.", ex.getMessage());
    }
}
