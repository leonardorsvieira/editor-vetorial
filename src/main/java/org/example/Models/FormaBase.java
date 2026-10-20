package org.example.Models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementação comum às formas: lista de pontos, estilo visual e identificador de grupo.
 * Os atributos são privados e acessados por getters/setters.
 */
public abstract class FormaBase implements FormaGeometrica {

    private final List<Ponto> pontos = new ArrayList<>();
    private String corPreenchimentoHex = "#4A90E2";
    private String corBordaHex = "#1F2937";
    private double espessuraBorda = 1.0;
    private String groupId;

    @Override
    public void adicionarPonto(Ponto p) {
        pontos.add(Objects.requireNonNull(p, "ponto não pode ser nulo"));
    }

    @Override
    public List<Ponto> getPontos() {
        return Collections.unmodifiableList(pontos);
    }

    /** Acesso interno para subclasses que precisam alterar os pontos. */
    protected List<Ponto> pontosInternos() {
        return pontos;
    }

    @Override
    public int getQuantidadePontos() {
        return pontos.size();
    }

    @Override
    public String getCorPreenchimentoHex() {
        return corPreenchimentoHex;
    }

    @Override
    public void setCorPreenchimentoHex(String hex) {
        this.corPreenchimentoHex = Objects.requireNonNull(hex);
    }

    @Override
    public String getCorBordaHex() {
        return corBordaHex;
    }

    @Override
    public void setCorBordaHex(String hex) {
        this.corBordaHex = Objects.requireNonNull(hex);
    }

    @Override
    public double getEspessuraBorda() {
        return espessuraBorda;
    }

    @Override
    public void setEspessuraBorda(double espessura) {
        if (espessura < 0) {
            throw new IllegalArgumentException("A espessura da borda não pode ser negativa");
        }
        this.espessuraBorda = espessura;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /** Centro geométrico: média dos vértices. */
    @Override
    public Ponto getCentroide() {
        if (pontos.isEmpty()) {
            throw new IllegalStateException("Forma sem pontos não possui centroide");
        }
        double somaX = 0;
        double somaY = 0;
        for (Ponto p : pontos) {
            somaX += p.getX();
            somaY += p.getY();
        }
        return new Ponto(somaX / pontos.size(), somaY / pontos.size());
    }

    /** Teste de ponto-em-polígono pelo algoritmo de raio (ray casting). */
    @Override
    public boolean contemPonto(double px, double py) {
        int n = pontos.size();
        if (n < 3) {
            return false;
        }
        boolean dentro = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Ponto a = pontos.get(i);
            Ponto b = pontos.get(j);
            boolean cruza = (a.getY() > py) != (b.getY() > py);
            if (cruza && px < (b.getX() - a.getX()) * (py - a.getY()) / (b.getY() - a.getY()) + a.getX()) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    @Override
    public void transladar(double dx, double dy) {
        for (Ponto p : pontos) {
            p.setX(p.getX() + dx);
            p.setY(p.getY() + dy);
        }
    }

    @Override
    public void escalar(double fator) {
        escalarEm(getCentroide(), fator);
    }

    @Override
    public void escalarEm(Ponto centro, double fator) {
        if (fator <= 0) {
            throw new IllegalArgumentException("O fator de escala deve ser positivo");
        }
        for (Ponto p : pontos) {
            p.setX(centro.getX() + fator * (p.getX() - centro.getX()));
            p.setY(centro.getY() + fator * (p.getY() - centro.getY()));
        }
    }

    @Override
    public void rotacionar(double anguloRadianos) {
        rotacionarEm(getCentroide(), anguloRadianos);
    }

    @Override
    public void rotacionarEm(Ponto centro, double anguloRadianos) {
        double cos = Math.cos(anguloRadianos);
        double sen = Math.sin(anguloRadianos);
        for (Ponto p : pontos) {
            double dx = p.getX() - centro.getX();
            double dy = p.getY() - centro.getY();
            p.setX(centro.getX() + dx * cos - dy * sen);
            p.setY(centro.getY() + dx * sen + dy * cos);
        }
    }

    @Override
    public void cisalhar(double shx, double shy) {
        cisalharEm(getCentroide(), shx, shy);
    }

    @Override
    public void cisalharEm(Ponto centro, double shx, double shy) {
        for (Ponto p : pontos) {
            double dx = p.getX() - centro.getX();
            double dy = p.getY() - centro.getY();
            p.setX(centro.getX() + dx + shx * dy);
            p.setY(centro.getY() + dy + shy * dx);
        }
    }

    /** Copia estilo, grupo e pontos (em cópia profunda) da forma de origem. */
    protected void copiarEstadoDe(FormaBase origem) {
        pontos.clear();
        for (Ponto p : origem.pontos) {
            pontos.add(p.clonar());
        }
        this.corPreenchimentoHex = origem.corPreenchimentoHex;
        this.corBordaHex = origem.corBordaHex;
        this.espessuraBorda = origem.espessuraBorda;
        this.groupId = origem.groupId;
    }

    @Override
    public abstract FormaGeometrica clonar();

    @Override
    public String toString() {
        return getTipo() + " " + pontos;
    }
}
