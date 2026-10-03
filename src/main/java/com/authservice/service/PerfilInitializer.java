package com.authservice.service;

import com.authservice.enums.TipoPerfil;
import com.authservice.model.Perfil;
import com.authservice.repository.PerfilRepository;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(2)
@RequiredArgsConstructor
public class PerfilInitializer implements CommandLineRunner {

    private final PerfilRepository perfilRepository;

    @Override
    public void run(String... args) {
        // Inicializar perfis se não existirem
        if (!perfilRepository.existsByTipo(TipoPerfil.ADMIN)) {
            Perfil adminPerfil = new Perfil();
            adminPerfil.setId(UUID.randomUUID().toString());
            adminPerfil.setTipo(TipoPerfil.ADMIN);
            adminPerfil.setDescricao("Administrador - Pode criar contas, gerenciar usuários e acessar todas as funcionalidades");
            adminPerfil.setAtivo(true);
            perfilRepository.save(adminPerfil);
        }

        if (!perfilRepository.existsByTipo(TipoPerfil.USER)) {
            Perfil userPerfil = new Perfil();
            userPerfil.setId(UUID.randomUUID().toString());
            userPerfil.setTipo(TipoPerfil.USER);
            userPerfil.setDescricao("Usuário - Pode autenticar e buscar seu próprio perfil");
            userPerfil.setAtivo(true);
            perfilRepository.save(userPerfil);
        }
    }
}

