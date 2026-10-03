package com.authservice.service;

import com.authservice.dto.CriarUsuarioDTO;
import com.authservice.enums.TipoPerfil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro usuário ADMIN na inicialização, caso ainda não exista.
 * Resolve o "ovo e a galinha": criar usuários ADMIN exige um ADMIN autenticado.
 * Configure via ADMIN_EMAIL / ADMIN_SENHA / ADMIN_NOME (ADMIN_EMAIL vazio desativa).
 */
@Slf4j
@Component
@Order(3)
public class AdminInitializer implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInitializer(UsuarioService usuarioService,
                            @Value("${bootstrap.admin.email:}") String email,
                            @Value("${bootstrap.admin.senha:}") String senha,
                            @Value("${bootstrap.admin.nome:Administrador}") String nome) {
        this.usuarioService = usuarioService;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(String... args) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (usuarioService.existePorEmail(email)) {
            log.debug("ADMIN inicial '{}' já existe", email);
            return;
        }
        usuarioService.criarUsuario(new CriarUsuarioDTO(nome, email, senha, true, TipoPerfil.ADMIN));
        log.warn("ADMIN inicial criado: {} (altere a senha padrão em produção via ADMIN_SENHA)", email);
    }
}
