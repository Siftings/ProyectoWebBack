package com.facimus.procesos.modelado.service;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.facimus.procesos.common.RecursoNoEncontradoException;
import com.facimus.procesos.modelado.model.EventoMensaje;
import com.facimus.procesos.modelado.model.NodoFlujo;
import com.facimus.procesos.modelado.model.TipoEventoMensaje;
import com.facimus.procesos.modelado.repository.NodoFlujoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NodoFlujoService {

    private final NodoFlujoRepository nodoFlujoRepository;

    @Transactional(readOnly = true)
    public NodoFlujo obtener(Long empresaId, Long nodoId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(nodoId, empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Nodo no encontrado."));
    }

    @Transactional(readOnly = true)
    public Optional<NodoFlujo> buscar(Long empresaId, Long nodoId) {
        return nodoFlujoRepository.findByIdAndEmpresaId(nodoId, empresaId);
    }

    @Transactional(readOnly = true)
    public List<NodoFlujo> listarActivosPorProceso(Long empresaId, Long procesoId) {
        return nodoFlujoRepository.findAllByLane_Pool_ProcesoIdAndEmpresaId(procesoId, empresaId).stream()
                .filter(NodoFlujo::isActivo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NodoFlujo> listarPorLane(Long empresaId, Long laneId) {
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId);
    }

    @Transactional(readOnly = true)
    public boolean tieneNodosActivos(Long empresaId, Long laneId) {
        return nodoFlujoRepository.findAllByLaneIdAndEmpresaId(laneId, empresaId).stream()
                .anyMatch(NodoFlujo::isActivo);
    }

    @Transactional(readOnly = true)
    public boolean tieneNodosActivosEnPool(Long empresaId, Long poolId) {
        return nodoFlujoRepository.existsByLane_PoolIdAndEmpresaIdAndActivoTrue(poolId, empresaId);
    }

    @Transactional(readOnly = true)
    public boolean existeNombreEnProceso(String nombre, Long procesoId, Long empresaId) {
        return nodoFlujoRepository.existsByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaIdAndActivoTrue(
                nombre, procesoId, empresaId);
    }

    /**
     * HU-25/HU-27: un Message Throw y su Message Catch deben llamarse igual que el mensaje, asi que el
     * nombre de un evento puede repetirse una sola vez, con un evento del tipo complementario
     * (THROW <-> CATCH_*) ubicado en otro pool. Fuera de ese par, el nombre sigue siendo unico en el proceso.
     */
    @Transactional(readOnly = true)
    public boolean nombreDisponibleParaEvento(String nombre, Long procesoId, Long empresaId,
            TipoEventoMensaje tipoEvento, Long poolId, Long eventoIdExcluir) {
        List<NodoFlujo> conflictos = nodoFlujoRepository
                .findAllByNombreIgnoreCaseAndLane_Pool_ProcesoIdAndEmpresaIdAndActivoTrue(nombre, procesoId, empresaId)
                .stream()
                .filter(nodo -> !nodo.getId().equals(eventoIdExcluir))
                .toList();
        if (conflictos.isEmpty()) {
            return true;
        }
        return conflictos.size() == 1
                && conflictos.get(0) instanceof EventoMensaje otro
                && esThrow(otro.getTipoEvento()) != esThrow(tipoEvento)
                && !otro.getLane().getPool().getId().equals(poolId);
    }

    private static boolean esThrow(TipoEventoMensaje tipo) {
        return tipo == TipoEventoMensaje.THROW;
    }

    public NodoFlujo guardar(NodoFlujo nodo) {
        return nodoFlujoRepository.save(nodo);
    }
}
