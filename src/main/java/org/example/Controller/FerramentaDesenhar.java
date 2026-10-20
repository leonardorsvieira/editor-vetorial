package org.example.Controller;

import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Hexagono;
import org.example.Models.Poligono;
import org.example.Models.Ponto;
import org.example.Models.Quadrado;

/**
 * Estado de criação de formas. Polígonos são criados por cliques (um vértice por clique,
 * concluídos com Enter, clique direito ou duplo clique). Círculo, quadrado e hexágono são
 * criados arrastando o mouse do centro/canto até a posição desejada.
 */
public class FerramentaDesenhar implements Ferramenta {

    public enum Tipo {
        POLIGONO("Polígono"),
        CIRCULO("Círculo"),
        QUADRADO("Quadrado"),
        HEXAGONO("Hexágono");

        private final String rotulo;

        Tipo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    private final Tipo tipo;
    private Ponto inicio;

    public FerramentaDesenhar(Tipo tipo) {
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    @Override
    public String getNome() {
        return "Desenhar " + tipo.getRotulo();
    }

    @Override
    public void aoPressionarMouse(EditorContext ctx, Ponto p) {
        if (tipo != Tipo.POLIGONO) {
            inicio = p;
        }
    }

    @Override
    public void aoArrastarMouse(EditorContext ctx, Ponto p) {
        if (inicio != null) {
            ctx.setFormaEmConstrucao(criarForma(inicio, p));
        }
    }

    @Override
    public void aoSoltarMouse(EditorContext ctx, Ponto p) {
        if (inicio == null) {
            return;
        }
        FormaGeometrica forma = criarForma(inicio, p);
        inicio = null;
        ctx.setFormaEmConstrucao(null);
        if (forma != null) {
            aplicarEstilo(ctx, forma);
            ctx.adicionarForma(forma);
        }
    }

    @Override
    public void aoClicar(EditorContext ctx, Ponto p) {
        if (tipo != Tipo.POLIGONO) {
            return;
        }
        Poligono poligono = ctx.getFormaEmConstrucao() instanceof Poligono
                ? (Poligono) ctx.getFormaEmConstrucao()
                : new Poligono();
        // Ignora cliques repetidos no mesmo vértice (ex.: primeiro clique de um duplo clique).
        if (poligono.getQuantidadePontos() > 0) {
            Ponto ultimo = poligono.getPontos().get(poligono.getQuantidadePontos() - 1);
            if (ultimo.distancia(p) < 1e-9) {
                return;
            }
        }
        poligono.adicionarPonto(p);
        ctx.setFormaEmConstrucao(poligono);
    }

    @Override
    public void aoConcluir(EditorContext ctx) {
        if (tipo == Tipo.POLIGONO && ctx.getFormaEmConstrucao() instanceof Poligono) {
            Poligono poligono = (Poligono) ctx.getFormaEmConstrucao();
            ctx.setFormaEmConstrucao(null);
            if (poligono.isValido()) {
                aplicarEstilo(ctx, poligono);
                ctx.adicionarForma(poligono);
            }
        }
    }

    @Override
    public void aoCancelar(EditorContext ctx) {
        inicio = null;
        ctx.setFormaEmConstrucao(null);
    }

    /** Cria a forma a partir do arraste; retorna {@code null} se o tamanho for zero. */
    private FormaGeometrica criarForma(Ponto inicio, Ponto fim) {
        double dx = fim.getX() - inicio.getX();
        double dy = fim.getY() - inicio.getY();
        switch (tipo) {
            case CIRCULO: {
                double raio = inicio.distancia(fim);
                return raio > 0 ? new Circulo(inicio.clonar(), raio) : null;
            }
            case HEXAGONO: {
                double raio = inicio.distancia(fim);
                return raio > 0 ? new Hexagono(inicio.clonar(), raio) : null;
            }
            case QUADRADO: {
                double lado = Math.max(Math.abs(dx), Math.abs(dy));
                if (lado <= 0) {
                    return null;
                }
                // O quadrado cresce na direção do arraste, a partir do ponto inicial.
                double x0 = dx < 0 ? inicio.getX() - lado : inicio.getX();
                double y0 = dy < 0 ? inicio.getY() - lado : inicio.getY();
                return new Quadrado(new Ponto(x0, y0), lado);
            }
            case POLIGONO:
            default:
                return null;
        }
    }

    private void aplicarEstilo(EditorContext ctx, FormaGeometrica forma) {
        forma.setCorPreenchimentoHex(ctx.getCorPreenchimentoAtual());
        forma.setCorBordaHex(ctx.getCorBordaAtual());
        forma.setEspessuraBorda(ctx.getEspessuraAtual());
    }
}
