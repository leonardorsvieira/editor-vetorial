package org.example.Controller;

import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

/**
 * Estado de edição de vértices: ao pressionar perto de um vértice de um polígono selecionado,
 * arrasta-o para outra posição. Se não houver vértice por perto, seleciona a forma sob o cursor.
 */
public class FerramentaEditarVertice implements Ferramenta {

    /** Distância máxima (em unidades do canvas) para agarrar um vértice. */
    private static final double TOLERANCIA = 8.0;

    private Poligono alvo;
    private int indiceVertice = -1;

    @Override
    public String getNome() {
        return "Editar vértices";
    }

    @Override
    public void aoPressionarMouse(EditorContext ctx, Ponto p) {
        indiceVertice = -1;
        alvo = null;
        for (FormaGeometrica f : ctx.getSelecionadas()) {
            if (!(f instanceof Poligono)) {
                continue;
            }
            Poligono poligono = (Poligono) f;
            for (int i = 0; i < poligono.getQuantidadePontos(); i++) {
                if (poligono.getPontos().get(i).distancia(p) <= TOLERANCIA) {
                    ctx.registrarHistorico();
                    alvo = poligono;
                    indiceVertice = i;
                    return;
                }
            }
        }
        FormaGeometrica forma = ctx.formaNoPonto(p);
        if (forma != null) {
            ctx.selecionar(forma, ctx.isModoAdicionar());
        } else if (!ctx.isModoAdicionar()) {
            ctx.limparSelecao();
        }
    }

    @Override
    public void aoArrastarMouse(EditorContext ctx, Ponto p) {
        if (alvo != null && indiceVertice >= 0) {
            alvo.moverVertice(indiceVertice, p.getX(), p.getY());
            ctx.atualizarVisual();
        }
    }

    @Override
    public void aoSoltarMouse(EditorContext ctx, Ponto p) {
        alvo = null;
        indiceVertice = -1;
    }

    @Override
    public void aoClicar(EditorContext ctx, Ponto p) {
        // Tratado em aoPressionarMouse.
    }

    @Override
    public void aoCancelar(EditorContext ctx) {
        alvo = null;
        indiceVertice = -1;
    }
}
