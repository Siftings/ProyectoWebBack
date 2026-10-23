package com.facimus.procesos.common;

/**
 * Se lanza cuando el usuario esta autenticado pero su rol no tiene permiso para la operacion segun
 * una politica configurable de la empresa (p. ej. permisos de estructura de HU-24). El manejador
 * global la traduce a 403, igual que un rechazo de Spring Security.
 */
public class PermisoDenegadoException extends RuntimeException {

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
