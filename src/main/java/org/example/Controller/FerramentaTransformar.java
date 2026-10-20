package org.example.Controller;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

import java.util.List;

/**
 * Estado de transformação das formas selecionadas (ou de seus grupos).
 * Ao arrastar, aplica a transformação incremental do modo escolhido:
 * translação, escala, rotação ou cisalhamento em torno do centroide do conjunto.
 * O estado é salvo no histórico uma única vez por gesto (no pressionar do mouse).
 */
public class FerramentaTransformar implements Ferramenta {

    public enum Modo {
        TRANSLADAR("Transladar"),
        ESCALAR("Escalar"),
        ROTACIONAR("Rotacionar"),
        CISALHAR("Cisalhar");

        private final String rotulo;

        Modo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private static final double MIN_DISTANCIA = 1e-6;
    private static final double FATOR_CISALHAMENTO = 100.0;

    private final Modo modo;
    private Ponto anterior;
    private Ponto pivo;

    public FerramentaTransformar(Modo modo) {
        this.modo = modo;
    }

    public Modo getModo() {
        return modo;
    }

    @Override
    public String getNome() {
        return "Transformar: " + modo.getRotulo();
    }

    @Override
    public void aoPressionarMouse(EditorContext ctx, Ponto p) {
        List<FormaGeometrica> afetadas = ctx.formasAfetadas();
        if (afetadas.isEmpty()) {
            anterior = null;
            return;
        }
        ctx.registrarHistorico();
        anterior = p;
        pivo = centroideDoConjunto(afetadas);
    }

    @Override
    public void aoArrastarMouse(EditorContext ctx, Ponto p) {
        if (anterior == null) {
            return;
        }
        List<FormaGeometrica> afetadas = ctx.formasAfetadas();
        switch (modo) {
            case TRANSLADAR:
                for (FormaGeometrica f : afetadas) {
                    f.transladar(p.getX() - anterior.getX(), p.getY() - anterior.getY());
                }
                break;
            case ESCALAR: {
                double distAnterior = anterior.distancia(pivo);
                double distAtual = p.distancia(pivo);
                if (distAnterior > MIN_DISTANCIA && distAtual > MIN_DISTANCIA) {
                    double fator = distAtual / distAnterior;
                    for (FormaGeometrica f : afetadas) {
                        f.escalarEm(pivo, fator);
                    }
                }
                break;
            }
            case ROTACIONAR: {
                if (anterior.distancia(pivo) > MIN_DISTANCIA && p.distancia(pivo) > MIN_DISTANCIA) {
                    double angulo = Math.atan2(p.getY() - pivo.getY(), p.getX() - pivo.getX())
                            - Math.atan2(anterior.getY() - pivo.getY(), anterior.getX() - pivo.getX());
                    for (FormaGeometrica f : afetadas) {
                        f.rotacionarEm(pivo, angulo);
                    }
                }
                break;
            }
            case CISALHAR:
                for (FormaGeometrica f : afetadas) {
                    f.cisalharEm(pivo,
                            (p.getX() - anterior.getX()) / FATOR_CISALHAMENTO,
                            (p.getY() - anterior.getY()) / FATOR_CISALHAMENTO);
                }
                break;
            default:
                break;
        }
        anterior = p;
        ctx.atualizarVisual();
    }

    @Override
    public void aoSoltarMouse(EditorContext ctx, Ponto p) {
        anterior = null;
        pivo = null;
    }

    @Override
    public void aoClicar(EditorContext ctx, Ponto p) {
        // Transformações acontecem durante o arraste.
    }

    @Override
    public void aoCancelar(EditorContext ctx) {
        anterior = null;
        pivo = null;
    }

    /** Média dos centroides: pivô comum para escala, rotação e cisalhamento de um grupo. */
    private Ponto centroideDoConjunto(List<FormaGeometrica> formas) {
        double somaX = 0;
        double somaY = 0;
        for (FormaGeometrica f : formas) {
            Ponto c = f.getCentroide();
            somaX += c.getX();
            somaY += c.getY();
        }
        return new Ponto(somaX / formas.size(), somaY / formas.size());
    }
}
