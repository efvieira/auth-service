package com.authservice.validator;

import java.util.regex.Pattern;

public class SenhaValidator {

    private static final int MINIMO_CARACTERES = 8;
    private static final Pattern LETRA_MAIUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern NUMERO = Pattern.compile("[0-9]");

    /**
     * RN-003: Senha deve conter:
     * - mínimo 8 caracteres
     * - 1 letra maiúscula
     * - 1 número
     */
    public static boolean isValida(String senha) {
        if (senha == null || senha.length() < MINIMO_CARACTERES) {
            return false;
        }

        if (!LETRA_MAIUSCULA.matcher(senha).find()) {
            return false;
        }

        if (!NUMERO.matcher(senha).find()) {
            return false;
        }

        return true;
    }

    public static String obterMensagemErro(String senha) {
        if (senha == null) {
            return "Senha não pode ser nula";
        }

        if (senha.length() < MINIMO_CARACTERES) {
            return "Senha deve ter no mínimo " + MINIMO_CARACTERES + " caracteres";
        }

        if (!LETRA_MAIUSCULA.matcher(senha).find()) {
            return "Senha deve conter ao menos uma letra maiúscula";
        }

        if (!NUMERO.matcher(senha).find()) {
            return "Senha deve conter ao menos um número";
        }

        return "Senha válida";
    }
}

