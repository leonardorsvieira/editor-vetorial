package org.example.Models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FormasTest {

    @Test
    void areaDoQuadradoPelaFormulaDeGauss() {
        Poligono p = new Poligono(List.of(
                new Ponto(0, 0), new Ponto(10, 0), new Ponto(10, 10), new Ponto(0, 10)));
        assertEquals(100.0, p.calcularArea(), 1e-9);
        assertEquals(40.0, p.calcularPerimetro(), 1e-9);
    }

    @Test
    void poligonoExigeTresPontos() {
        assertThrows(IllegalArgumentException.class,
                () -> new Poligono(List.of(new Ponto(0, 0), new Ponto(1, 1))));
        Poligono p = new Poligono();
        p.adicionarPonto(new Ponto(0, 0));
        p.adicionarPonto(new Ponto(1, 0));
        assertFalse(p.isValido());
        assertThrows(IllegalStateException.class, p::calcularArea);
    }

    @Test
    void circuloExigeRaioPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new Circulo(new Ponto(0, 0), 0));
        assertThrows(IllegalArgumentException.class, () -> new Circulo(new Ponto(0, 0), -3));
        Circulo c = new Circulo(new Ponto(0, 0), 2);
        assertEquals(Math.PI * 4, c.calcularArea(), 1e-9);
        assertEquals(4 * Math.PI, c.calcularPerimetro(), 1e-9);
    }

    @Test
    void quadradoEHexagonoCalculamVertices() {
        Quadrado q = new Quadrado(new Ponto(5, 5), 4);
        assertEquals(4, q.getQuantidadePontos());
        assertEquals(16.0, q.calcularArea(), 1e-9);

        Hexagono h = new Hexagono(new Ponto(0, 0), 10);
        assertEquals(6, h.getQuantidadePontos());
        assertEquals(10.0, h.getPontos().get(0).distancia(new Ponto(0, 0)), 1e-9);
    }

    @Test
    void contemPontoNoPoligonoEnoCirculo() {
        Poligono p = new Poligono(List.of(
                new Ponto(0, 0), new Ponto(10, 0), new Ponto(10, 10), new Ponto(0, 10)));
        assertTrue(p.contemPonto(5, 5));
        assertFalse(p.contemPonto(15, 5));

        Circulo c = new Circulo(new Ponto(0, 0), 5);
        assertTrue(c.contemPonto(3, 3));
        assertFalse(c.contemPonto(4, 4));
    }

    @Test
    void transformacoesPreservamCentroide() {
        Poligono p = new Poligono(List.of(
                new Ponto(0, 0), new Ponto(10, 0), new Ponto(10, 10), new Ponto(0, 10)));
        p.rotacionar(Math.PI / 4);
        assertEquals(5.0, p.getCentroide().getX(), 1e-9);
        assertEquals(5.0, p.getCentroide().getY(), 1e-9);

        p.escalar(2);
        assertEquals(400.0, p.calcularArea(), 1e-6);

        p.transladar(3, -2);
        assertEquals(8.0, p.getCentroide().getX(), 1e-9);
        assertEquals(3.0, p.getCentroide().getY(), 1e-9);
    }

    @Test
    void escalaDoCirculoAlteraRaio() {
        Circulo c = new Circulo(new Ponto(0, 0), 5);
        c.escalar(3);
        assertEquals(15.0, c.getRaio(), 1e-9);
    }

    @Test
    void clonarGeraCopiaProfunda() {
        Poligono original = new Poligono(List.of(
                new Ponto(0, 0), new Ponto(10, 0), new Ponto(10, 10)));
        original.setCorBordaHex("#FF0000");
        FormaGeometrica copia = original.clonar();

        original.transladar(100, 100);
        assertEquals(0.0, copia.getPontos().get(0).getX(), 1e-9);
        assertEquals("#FF0000", copia.getCorBordaHex());
        assertTrue(copia instanceof Poligono);
    }

    @Test
    void circuloClonadoMantemRaio() {
        Circulo c = new Circulo(new Ponto(1, 2), 7);
        Circulo copia = (Circulo) c.clonar();
        assertEquals(7.0, copia.getRaio(), 1e-9);
        assertEquals(1.0, copia.getCentro().getX(), 1e-9);
    }
}
