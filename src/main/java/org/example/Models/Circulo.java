package org.example.Models;

/**
 * Círculo definido pelo centro (armazenado como único ponto da forma) e raio estritamente positivo.
 */
public class Circulo extends FormaBase {

    private double raio;

    public Circulo() {
    }

    public Circulo(Ponto centro, double raio) {
        setRaio(raio);
        adicionarPonto(centro);
    }

    public Ponto getCentro() {
        return getPontos().get(0);
    }

    public double getRaio() {
        return raio;
    }

    public void setRaio(double raio) {
        if (raio <= 0) {
            throw new IllegalArgumentException("O raio do círculo deve ser estritamente positivo (r > 0)");
        }
        this.raio = raio;
    }

    @Override
    public double calcularArea() {
        return Math.PI * raio * raio;
    }

    @Override
    public double calcularPerimetro() {
        return 2 * Math.PI * raio;
    }

    @Override
    public Ponto getCentroide() {
        return getCentro().clonar();
    }

    @Override
    public boolean contemPonto(double px, double py) {
        return getCentro().distancia(new Ponto(px, py)) <= raio;
    }

    @Override
    public void escalarEm(Ponto centro, double fator) {
        super.escalarEm(centro, fator);
        this.raio *= fator;
    }

    @Override
    public void cisalharEm(Ponto centro, double shx, double shy) {
        // Cisalhamento não deforma um círculo em elipse neste modelo: apenas desloca o centro.
        super.cisalharEm(centro, 0, 0);
    }

    @Override
    public String getTipo() {
        return "CIRCULO";
    }

    @Override
    public Circulo clonar() {
        Circulo copia = new Circulo();
        copia.copiarEstadoDe(this);
        copia.raio = this.raio;
        return copia;
    }
}
