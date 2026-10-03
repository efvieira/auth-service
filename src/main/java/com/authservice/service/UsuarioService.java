package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.dto.PerfilDTO;
import com.authservice.dto.UsuarioResponseDTO;
import com.authservice.enums.TipoPerfil;
import com.authservice.exception.EmailJaExisteException;
import com.authservice.exception.SenhaInvalidaException;
import com.authservice.exception.UsuarioNaoEncontradoException;
import com.authservice.model.Perfil;
import com.authservice.model.Usuario;
import com.authservice.repository.PerfilRepository;
import com.authservice.repository.UsuarioRepository;
import com.authservice.validator.SenhaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<UsuarioResponseDTO> obterPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(this::converterParaDTO);
    }

    public Optional<UsuarioResponseDTO> obterPorId(String id) {
        return usuarioRepository.findById(id).map(this::converterParaDTO);
    }

    /**
     * Cria um novo usuário. Usado no registro público (perfil USER),
     * na criação administrativa e no bootstrap do ADMIN inicial.
     * Validações:
     * - RN-001: Email único
     * - RN-002: Senha com BCrypt
     * - RN-003: Validação de senha
     *
     * O perfil pode ser especificado; se não informado, usa USER como padrão
     */
    public UsuarioResponseDTO criarUsuario(CriarUsuarioDTO criarDTO) {
        String email = normalizarEmail(criarDTO.getEmail());

        // RN-001: Verificar email único
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaExisteException("Email já existe: " + email);
        }

        // RN-003: Validar formato da senha
        if (!SenhaValidator.isValida(criarDTO.getSenha())) {
            throw new SenhaInvalidaException(SenhaValidator.obterMensagemErro(criarDTO.getSenha()));
        }

        // Atribuir perfil: se não informado, usa USER como padrão
        TipoPerfil tipoPerfil = criarDTO.getPerfil() != null ? criarDTO.getPerfil() : TipoPerfil.USER;
        Perfil perfil = perfilRepository.findByTipo(tipoPerfil)
                .orElseThrow(() -> new IllegalStateException("Perfil não encontrado: " + tipoPerfil));

        LocalDateTime agora = LocalDateTime.now();
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID().toString());
        usuario.setEmail(email);
        usuario.setNome(criarDTO.getNome());
        // RN-002: Armazenar senha com BCrypt
        usuario.setSenha(passwordEncoder.encode(criarDTO.getSenha()));
        usuario.setAtivo(criarDTO.getAtivo() != null ? criarDTO.getAtivo() : true);
        usuario.setPerfilId(perfil.getId());
        usuario.setDataCriacao(agora);
        usuario.setDataAtualizacao(agora);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return converterParaDTO(usuarioSalvo, perfil);
    }

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(normalizarEmail(email));
    }

    /**
     * Emails são comparados sem diferenciar maiúsculas/minúsculas (RN-001)
     */
    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        Perfil perfil = usuario.getPerfilId() == null ? null
                : perfilRepository.findById(usuario.getPerfilId()).orElse(null);
        return converterParaDTO(usuario, perfil);
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario, Perfil perfil) {
        PerfilDTO perfilDTO = perfil == null ? null
                : new PerfilDTO(perfil.getId(), perfil.getTipo(), perfil.getDescricao(), perfil.getAtivo());
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getAtivo(),
                perfilDTO
        );
    }
}
