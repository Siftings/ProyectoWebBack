package com.facimus.procesos.modelado.service;

import com.facimus.procesos.common.ReglaNegocioException;
import com.facimus.procesos.gestion.model.EstadoProceso;
import com.facimus.procesos.gestion.model.Proceso;

/** Reglas compartidas por todos los servicios que modifican el diagrama de un proceso. */
final class ReglasModelado {

    private ReglasModelado() {
    }

    /**
     * El diagrama solo se modifica en procesos activos y en BORRADOR: un proceso PUBLICADO ya paso la
     * validacion del modelo, y cambiarlo despues dejaria publicado un diagrama que nadie valido.
     */
    static void validarProcesoModelable(Proceso proceso) {
        if (!proceso.isActivo()) {
            throw new ReglaNegocioException("No se permite modelar en un proceso inactivo.");
        }
        if (proceso.getEstado() == EstadoProceso.PUBLICADO) {
            throw new ReglaNegocioException("No se permite modificar el diagrama de un proceso publicado.");
        }
    }
}
