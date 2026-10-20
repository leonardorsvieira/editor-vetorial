package org.example.Views;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.example.Controller.EditorContext;
import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

import java.util.List;
import java.util.function.Consumer;

/**
 * Área central de desenho. Renderiza o documento, a seleção e a forma em construção,
 * e encaminha os eventos do mouse ao {@link EditorContext}.
 */
public class PainelDesenho extends StackPane {

    private static final Color COR_GRADE = Color.web("#E5E7EB");
    private static final Color COR_SELECAO = Color.web("#F97316");
    private static final Color COR_CONSTRUCAO = Color.web("#6B7280");
    private static final double TAMANHO_ALCA = 6;
    private static final double MARGEM_QUADRO = 100;

    private final EditorContext ctx;
    private final Canvas canvas = new Canvas();
    private Consumer<Ponto> aoMoverCursor = p -> { };

    public PainelDesenho(EditorContext ctx) {
        this.ctx = ctx;
        getChildren().add(canvas);
        configurarMouse();
        ctx.adicionarOuvinte(this::redesenhar);
        redesenhar();
    }

    public void setAoMoverCursor(Consumer<Ponto> aoMoverCursor) {
        this.aoMoverCursor = aoMoverCursor;
    }

    private void configurarMouse() {
        canvas.setOnMousePressed(e -> {
            Ponto p = new Ponto(e.getX(), e.getY());
            if (e.getButton() == MouseButton.SECONDARY) {
                ctx.concluirAcao();
            } else if (e.getButton() == MouseButton.PRIMARY) {
                ctx.setModoAdicionar(e.isShiftDown());
                ctx.aoPressionarMouse(p);
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                ctx.aoArrastarMouse(new Ponto(e.getX(), e.getY()));
            }
            aoMoverCursor.accept(new Ponto(e.getX(), e.getY()));
        });
        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                ctx.aoSoltarMouse(new Ponto(e.getX(), e.getY()));
            }
        });
        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Ponto p = new Ponto(e.getX(), e.getY());
            if (e.getClickCount() >= 2) {
                ctx.concluirAcao();
            } else {
                ctx.aoClicar(p);
            }
        });
        canvas.setOnMouseMoved(e -> aoMoverCursor.accept(new Ponto(e.getX(), e.getY())));
    }

    public void redesenhar() {
        // O quadro cresce para acompanhar as formas, nunca fica menor que o tamanho configurado.
        double[] tamanho = tamanhoNecessario();
        canvas.setWidth(tamanho[0]);
        canvas.setHeight(tamanho[1]);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        desenharGrade(gc);

        for (FormaGeometrica f : ctx.getFormas()) {
            desenharForma(gc, f);
        }

        gc.setLineDashes(6, 4);
        gc.setStroke(COR_SELECAO);
        gc.setLineWidth(2);
        for (FormaGeometrica f : ctx.getSelecionadas()) {
            desenharContorno(gc, f);
            desenharAlcas(gc, f);
        }
        gc.setLineDashes(0);

        FormaGeometrica construcao = ctx.getFormaEmConstrucao();
        if (construcao != null) {
            gc.setLineDashes(6, 4);
            gc.setStroke(COR_CONSTRUCAO);
            gc.setLineWidth(1.5);
            if (construcao instanceof Poligono && !((Poligono) construcao).isValido()) {
                desenharPoliLinha(gc, (Poligono) construcao);
            } else {
                desenharContorno(gc, construcao);
            }
            gc.setLineDashes(0);
        }
    }

    private void desenharGrade(GraphicsContext gc) {
        double g = ctx.getTamanhoGrade();
        if (g <= 0) {
            return;
        }
        gc.setStroke(COR_GRADE);
        gc.setLineWidth(1);
        for (double x = g; x < canvas.getWidth(); x += g) {
            gc.strokeLine(x + 0.5, 0, x + 0.5, canvas.getHeight());
        }
        for (double y = g; y < canvas.getHeight(); y += g) {
            gc.strokeLine(0, y + 0.5, canvas.getWidth(), y + 0.5);
        }
    }

    /** Largura e altura do quadro: o maior entre a configuração e a área ocupada pelas formas. */
    private double[] tamanhoNecessario() {
        double largura = ctx.getLargura();
        double altura = ctx.getAltura();
        List<FormaGeometrica> todas = new java.util.ArrayList<>(ctx.getFormas());
        if (ctx.getFormaEmConstrucao() != null) {
            todas.add(ctx.getFormaEmConstrucao());
        }
        for (FormaGeometrica f : todas) {
            for (Ponto p : f.getPontos()) {
                largura = Math.max(largura, p.getX() + MARGEM_QUADRO);
                altura = Math.max(altura, p.getY() + MARGEM_QUADRO);
            }
            if (f instanceof Circulo) {
                Circulo c = (Circulo) f;
                largura = Math.max(largura, c.getCentro().getX() + c.getRaio() + MARGEM_QUADRO);
                altura = Math.max(altura, c.getCentro().getY() + c.getRaio() + MARGEM_QUADRO);
            }
        }
        return new double[]{largura, altura};
    }

    private void desenharForma(GraphicsContext gc, FormaGeometrica f) {
        gc.setFill(Color.web(f.getCorPreenchimentoHex()));
        gc.setStroke(Color.web(f.getCorBordaHex()));
        gc.setLineWidth(Math.max(f.getEspessuraBorda(), 0.01));
        if (f instanceof Circulo) {
            Circulo c = (Circulo) f;
            double r = c.getRaio();
            gc.fillOval(c.getCentro().getX() - r, c.getCentro().getY() - r, 2 * r, 2 * r);
            gc.strokeOval(c.getCentro().getX() - r, c.getCentro().getY() - r, 2 * r, 2 * r);
        } else if (f.getQuantidadePontos() >= 3) {
            double[][] xy = coordenadas(f.getPontos());
            gc.fillPolygon(xy[0], xy[1], xy[0].length);
            gc.strokePolygon(xy[0], xy[1], xy[0].length);
        }
    }

    private void desenharContorno(GraphicsContext gc, FormaGeometrica f) {
        if (f instanceof Circulo) {
            Circulo c = (Circulo) f;
            double r = c.getRaio();
            gc.strokeOval(c.getCentro().getX() - r, c.getCentro().getY() - r, 2 * r, 2 * r);
        } else if (f.getQuantidadePontos() >= 3) {
            double[][] xy = coordenadas(f.getPontos());
            gc.strokePolygon(xy[0], xy[1], xy[0].length);
        }
    }

    private void desenharPoliLinha(GraphicsContext gc, Poligono p) {
        double[][] xy = coordenadas(p.getPontos());
        gc.strokePolyline(xy[0], xy[1], xy[0].length);
    }

    /** Alças nos vértices dos polígonos selecionados (uso da ferramenta Editar vértices). */
    private void desenharAlcas(GraphicsContext gc, FormaGeometrica f) {
        gc.setLineDashes(0);
        gc.setFill(COR_SELECAO);
        if (f instanceof Poligono) {
            for (Ponto p : f.getPontos()) {
                gc.fillRect(p.getX() - TAMANHO_ALCA / 2, p.getY() - TAMANHO_ALCA / 2, TAMANHO_ALCA, TAMANHO_ALCA);
            }
        }
        gc.setLineDashes(6, 4);
    }

    private static double[][] coordenadas(List<Ponto> pontos) {
        double[] xs = new double[pontos.size()];
        double[] ys = new double[pontos.size()];
        for (int i = 0; i < pontos.size(); i++) {
            xs[i] = pontos.get(i).getX();
            ys[i] = pontos.get(i).getY();
        }
        return new double[][]{xs, ys};
    }
}
