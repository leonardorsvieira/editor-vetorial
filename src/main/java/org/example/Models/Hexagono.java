package org.example.Models;

/**
 * Hexágono regular inscrito em um círculo de raio {@code raio}, centrado em {@code centro}.
 * Os seis vértices são calculados automaticamente.
 */
public class Hexagono extends Poligono {

    private static final int LADOS = 6;

    public Hexagono() {
    }

    public Hexagono(Ponto centro, double raio) {
        if (raio <= 0) {
            throw new IllegalArgumentException("O raio do hexágono deve ser positivo");
        }
        for (int k = 0; k < LADOS; k++) {
            double angulo = Math.PI / 3 * k;
            adicionarPonto(new Ponto(
                    centro.getX() + raio * Math.cos(angulo),
                    centro.getY() + raio * Math.sin(angulo)));
        }
    }

    @Override
    public String getTipo() {
        return "HEXAGONO";
    }

    @Override
    public Hexagono clonar() {
        Hexagono copia = new Hexagono();
        copia.copiarEstadoDe(this);
        return copia;
    }
}
