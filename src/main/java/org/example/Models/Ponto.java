package org.example.Models;

/**
 * Coordenada bidimensional usada por todas as formas do editor.
 */
public class Ponto {

    private double x;
    private double y;

    public Ponto(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double distancia(Ponto outro) {
        return Math.hypot(x - outro.x, y - outro.y);
    }

    public Ponto clonar() {
        return new Ponto(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.1f, %.1f)", x, y);
    }
}
