package org.example.Models;

/**
 * Quadrado regular alinhado aos eixos. Os quatro vértices são calculados automaticamente
 * a partir do canto de origem (superior esquerdo) e do lado.
 */
public class Quadrado extends Poligono {

    public Quadrado() {
    }

    public Quadrado(Ponto origem, double lado) {
        if (lado <= 0) {
            throw new IllegalArgumentException("O lado do quadrado deve ser positivo");
        }
        adicionarPonto(new Ponto(origem.getX(), origem.getY()));
        adicionarPonto(new Ponto(origem.getX() + lado, origem.getY()));
        adicionarPonto(new Ponto(origem.getX() + lado, origem.getY() + lado));
        adicionarPonto(new Ponto(origem.getX(), origem.getY() + lado));
    }

    @Override
    public String getTipo() {
        return "QUADRADO";
    }

    @Override
    public Quadrado clonar() {
        Quadrado copia = new Quadrado();
        copia.copiarEstadoDe(this);
        return copia;
    }
}
