package com.authservice.security;

import com.authservice.enums.TipoPerfil;
import com.authservice.model.Perfil;
import com.authservice.model.Usuario;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Lê o header "Authorization: Bearer <token>", valida o access token (RN-005)
 * e autentica o usuário no SecurityContext.
 *
 * O perfil é carregado do banco a cada requisição, assim usuários desativados
 * ou com perfil alterado perdem o acesso imediatamente (RN-004).
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length()).trim();

            jwtTokenProvider.validarAccessToken(token)
                    .map(jwtTokenProvider::obterUsuarioId)
                    .flatMap(usuarioRepository::findById)
                    .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                    .ifPresent(usuario -> autenticar(usuario, request));
        }

        chain.doFilter(request, response);
    }

    private void autenticar(Usuario usuario, HttpServletRequest request) {
        TipoPerfil tipoPerfil = usuario.getPerfilId() == null ? null
                : perfilRepository.findById(usuario.getPerfilId())
                        .filter(perfil -> Boolean.TRUE.equals(perfil.getAtivo()))
                        .map(Perfil::getTipo)
                        .orElse(null);

        UsuarioAutenticado principal = new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), tipoPerfil);
        List<SimpleGrantedAuthority> authorities = tipoPerfil == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + tipoPerfil.name()));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
