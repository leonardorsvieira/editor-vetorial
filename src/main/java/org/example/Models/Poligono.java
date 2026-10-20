package org.example.Models;

import java.util.List;

/**
 * Polígono fechado. Regra: deve possuir no mínimo 3 pontos.
 * A área usa a fórmula de Gauss (shoelace) e o perímetro é a soma das distâncias entre vértices consecutivos.
 */
public class Poligono extends FormaBase {

    public static final int MIN_PONTOS = 3;

    public Poligono() {
    }

    public Poligono(List<Ponto> vertices) {
        if (vertices == null || vertices.size() < MIN_PONTOS) {
            throw new IllegalArgumentException("Um polígono deve ter no mínimo " + MIN_PONTOS + " pontos");
        }
        vertices.forEach(this::adicionarPonto);
    }

    public boolean isValido() {
        return getQuantidadePontos() >= MIN_PONTOS;
    }

    /** Move um vértice para uma nova posição (usado na edição de vértices). */
    public void moverVertice(int indice, double x, double y) {
        Ponto p = pontosInternos().get(indice);
        p.setX(x);
        p.setY(y);
    }

    @Override
    public double calcularArea() {
        validar();
        List<Ponto> pts = pontosInternos();
        int n = pts.size();
        double soma = 0;
        for (int i = 0; i < n; i++) {
            Ponto a = pts.get(i);
            Ponto b = pts.get((i + 1) % n);
            soma += a.getX() * b.getY() - b.getX() * a.getY();
        }
        return Math.abs(soma) / 2.0;
    }

    @Override
    public double calcularPerimetro() {
        validar();
        List<Ponto> pts = pontosInternos();
        int n = pts.size();
        double total = 0;
        for (int i = 0; i < n; i++) {
            total += pts.get(i).distancia(pts.get((i + 1) % n));
        }
        return total;
    }

    protected void validar() {
        if (!isValido()) {
            throw new IllegalStateException("Um polígono deve ter no mínimo " + MIN_PONTOS + " pontos");
        }
    }

    @Override
    public String getTipo() {
        return "POLIGONO";
    }

    @Override
    public Poligono clonar() {
        Poligono copia = new Poligono();
        copia.copiarEstadoDe(this);
        return copia;
    }
}
