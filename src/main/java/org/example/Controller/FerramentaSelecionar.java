package org.example.Controller;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

/**
 * Estado de seleção: clique seleciona a forma (ou o grupo inteiro) sob o cursor.
 * Com Shift pressionado a seleção é acumulada; clicar no vazio limpa a seleção.
 */
public class FerramentaSelecionar implements Ferramenta {

    @Override
    public String getNome() {
        return "Selecionar";
    }

    @Override
    public void aoPressionarMouse(EditorContext ctx, Ponto p) {
        FormaGeometrica alvo = ctx.formaNoPonto(p);
        if (alvo == null) {
            if (!ctx.isModoAdicionar()) {
                ctx.limparSelecao();
            }
        } else {
            ctx.selecionar(alvo, ctx.isModoAdicionar());
        }
    }

    @Override
    public void aoArrastarMouse(EditorContext ctx, Ponto p) {
        // A seleção não move formas: a movimentação é feita pela ferramenta Transformar.
    }

    @Override
    public void aoClicar(EditorContext ctx, Ponto p) {
        // Tratado em aoPressionarMouse.
    }
}
