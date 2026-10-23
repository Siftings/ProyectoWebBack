package com.facimus.procesos.gestion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import org.modelmapper.ModelMapper;

import com.facimus.procesos.gestion.controller.dto.EmpresaResponse;
import com.facimus.procesos.gestion.controller.dto.EmpresaResumenResponse;
import com.facimus.procesos.gestion.controller.dto.RegistroEmpresaRequest;
import com.facimus.procesos.gestion.model.Empresa;
import com.facimus.procesos.gestion.service.EmpresaOrquestadorService;
import com.facimus.procesos.gestion.service.EmpresaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

/** HU-01: registro de una nueva empresa y su administrador inicial. HU-23: ubicar otra empresa por NIT. */
@Tag(name = "Empresas", description = "Registro de empresas nuevas (HU-01) y busqueda por NIT para compartir procesos (HU-23).")
@RestController
@RequestMapping("/api/v1/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaOrquestadorService empresaOrquestadorService;
    private final EmpresaService empresaService;
    private final ModelMapper modelMapper;

    @SecurityRequirements()
    @PostMapping
    @Operation(summary = "Registrar una nueva empresa", description = "Crea la empresa y su usuario administrador inicial. Publico, no requiere autenticacion.")
    @ApiResponse(responseCode = "201", description = "Recurso creado.",
            content = @Content(schema = @Schema(implementation = EmpresaResponse.class)))
    @ApiResponse(responseCode = "400", description = "Solicitud invalida (errores de validacion o JSON malformado).",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto: la operacion viola una regla de negocio.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EmpresaResponse> registrar(@Validated @RequestBody RegistroEmpresaRequest request) {
        Empresa empresa = empresaOrquestadorService.registrarConAdministrador(request.nombreEmpresa(),
                request.nit(), request.correoContacto(), request.nombreAdmin(), request.emailAdmin(),
                request.passwordAdmin());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/empresas/{id}").buildAndExpand(empresa.getId()).toUri())
        .body(modelMapper.map(empresa, EmpresaResponse.class));
    }

    @GetMapping
    @Operation(summary = "Buscar una empresa por NIT", description = "Solo administrador. Coincidencia exacta; devuelve el id necesario para compartir un proceso (HU-23). No lista empresas.")
    @ApiResponse(responseCode = "200", description = "Operacion exitosa.",
            content = @Content(schema = @Schema(implementation = EmpresaResumenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Falta el parametro nit.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "No autenticado: token ausente o invalido.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Sin permisos: solo administrador.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No existe una empresa con ese NIT.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<EmpresaResumenResponse> buscarPorNit(@RequestParam String nit) {
        Empresa empresa = empresaService.obtenerPorNit(nit);
        return ResponseEntity.ok(new EmpresaResumenResponse(empresa.getId(), empresa.getNombre()));
    }
}
