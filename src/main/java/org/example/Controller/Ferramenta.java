package org.example.Controller;

import org.example.Models.Ponto;

/**
 * Interface de Estado (State pattern). Cada ferramenta define como o editor reage
 * aos eventos do mouse enquanto estiver ativa. Os pontos recebidos já estão ajustados à grade.
 */
public interface Ferramenta {

    String getNome();

    void aoPressionarMouse(EditorContext ctx, Ponto p);

    void aoArrastarMouse(EditorContext ctx, Ponto p);

    default void aoSoltarMouse(EditorContext ctx, Ponto p) {
    }

    void aoClicar(EditorContext ctx, Ponto p);

    /** Chamado ao concluir a ação atual (Enter, clique direito ou duplo clique). */
    default void aoConcluir(EditorContext ctx) {
    }

    /** Chamado ao cancelar a ação atual (Esc). */
    default void aoCancelar(EditorContext ctx) {
    }
}
