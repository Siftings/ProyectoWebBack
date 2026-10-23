package com.facimus.procesos.gestion.controller.dto;

/** Datos minimos de otra empresa, para compartirle un proceso (HU-23) sin exponer el resto de su registro. */
public record EmpresaResumenResponse(Long id, String nombre) {
}
