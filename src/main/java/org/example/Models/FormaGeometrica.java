package org.example.Models;

import java.util.List;

/**
 * Contrato de todas as formas do desenho.
 * Os métodos {@code escalarEm}, {@code rotacionarEm} e {@code cisalharEm} permitem
 * transformar uma forma (ou um grupo) em torno de um centro comum, e {@code getTipo}
 * identifica o tipo da forma na persistência.
 */
public interface FormaGeometrica {

    void adicionarPonto(Ponto p);

    List<Ponto> getPontos();

    int getQuantidadePontos();

    String getTipo();

    String getCorPreenchimentoHex();

    void setCorPreenchimentoHex(String hex);

    String getCorBordaHex();

    void setCorBordaHex(String hex);

    double getEspessuraBorda();

    void setEspessuraBorda(double espessura);

    String getGroupId();

    void setGroupId(String groupId);

    double calcularArea();

    double calcularPerimetro();

    Ponto getCentroide();

    boolean contemPonto(double px, double py);

    void transladar(double dx, double dy);

    void escalar(double fator);

    void escalarEm(Ponto centro, double fator);

    void rotacionar(double anguloRadianos);

    void rotacionarEm(Ponto centro, double anguloRadianos);

    void cisalhar(double shx, double shy);

    void cisalharEm(Ponto centro, double shx, double shy);

    FormaGeometrica clonar();
}
