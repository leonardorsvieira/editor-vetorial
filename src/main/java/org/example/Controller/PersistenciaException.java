package org.example.Controller;

/**
 * Falha ao salvar ou carregar um desenho (arquivo ausente, corrompido ou com dados inválidos).
 */
public class PersistenciaException extends Exception {

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
