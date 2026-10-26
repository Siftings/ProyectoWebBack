package com.facimus.procesos.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.facimus.procesos.gestion.service.UsuarioService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Solo desarrollo (seguridad.jwt.habilitado=false): autentica toda peticion como el usuario fijo, sin token.
 * Si el usuario no existe o esta inactivo, deja pasar sin autenticar: decide SecurityConfig.
 */
public class UsuarioFijoFilter extends OncePerRequestFilter {

    private final String email;
    private final UsuarioService usuarioService;

    public UsuarioFijoFilter(String email, UsuarioService usuarioService) {
        this.email = email;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        usuarioService.buscarActivoPorEmail(email)
                .map(ApiPrincipal::of)
                .ifPresent(principal -> autenticar(principal, request));
        filterChain.doFilter(request, response);
    }

    private void autenticar(ApiPrincipal principal, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
