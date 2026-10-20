package org.example.Controller;

import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Hexagono;
import org.example.Models.Poligono;
import org.example.Models.Ponto;
import org.example.Models.Quadrado;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Persistência em XML (API DOM da JDK, sem dependências externas).
 * Formato: {@code <desenho>} com as configurações do canvas e uma lista de {@code <forma>}.
 */
public final class GerenciadorPersistencia {

    private static final String RAIZ = "desenho";
    private static final String VERSAO = "1";

    private GerenciadorPersistencia() {
    }

    /** Conteúdo lido de um arquivo: configuração do canvas e formas. */
    public record Documento(double largura, double altura, double grade, boolean snap,
                            List<FormaGeometrica> formas) {
    }

    public static void salvar(Path arquivo, EditorContext ctx) throws PersistenciaException {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element raiz = doc.createElement(RAIZ);
            raiz.setAttribute("versao", VERSAO);
            raiz.setAttribute("largura", String.valueOf(ctx.getLargura()));
            raiz.setAttribute("altura", String.valueOf(ctx.getAltura()));
            raiz.setAttribute("grade", String.valueOf(ctx.getTamanhoGrade()));
            raiz.setAttribute("snap", String.valueOf(ctx.isSnapAtivo()));
            doc.appendChild(raiz);

            for (FormaGeometrica forma : ctx.getFormas()) {
                raiz.appendChild(paraElemento(doc, forma));
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
            try (var saida = Files.newOutputStream(arquivo)) {
                transformer.transform(new DOMSource(doc), new StreamResult(saida));
            }
            ctx.marcarSalvo();
        } catch (ParserConfigurationException | javax.xml.transform.TransformerException e) {
            throw new PersistenciaException("Não foi possível gerar o arquivo XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar o arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Lê um desenho. Arquivos ausentes, malformados ou com geometria inválida geram
     * {@link PersistenciaException} com mensagem explicativa, sem derrubar a aplicação.
     */
    public static Documento carregar(Path arquivo) throws PersistenciaException {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = fabrica.newDocumentBuilder();
            Document doc = builder.parse(Files.newInputStream(arquivo));

            Element raiz = doc.getDocumentElement();
            if (!RAIZ.equals(raiz.getTagName())) {
                throw new PersistenciaException("Arquivo não é um desenho válido (raiz inesperada).");
            }
            double largura = Double.parseDouble(raiz.getAttribute("largura"));
            double altura = Double.parseDouble(raiz.getAttribute("altura"));
            double grade = Double.parseDouble(raiz.getAttribute("grade"));
            boolean snap = Boolean.parseBoolean(raiz.getAttribute("snap"));

            List<FormaGeometrica> formas = new ArrayList<>();
            NodeList nos = raiz.getElementsByTagName("forma");
            for (int i = 0; i < nos.getLength(); i++) {
                formas.add(lerForma((Element) nos.item(i)));
            }
            return new Documento(largura, altura, grade, snap, formas);
        } catch (PersistenciaException e) {
            throw e;
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo: " + e.getMessage(), e);
        } catch (SAXException | ParserConfigurationException e) {
            throw new PersistenciaException("O arquivo está corrompido (XML inválido).", e);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
            // NumberFormatException também está incluída aqui (é subclasse de IllegalArgumentException).
            throw new PersistenciaException("O arquivo contém dados inválidos: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ escrita

    private static Element paraElemento(Document doc, FormaGeometrica forma) {
        Element el = doc.createElement("forma");
        el.setAttribute("tipo", forma.getTipo());
        el.setAttribute("corPreenchimento", forma.getCorPreenchimentoHex());
        el.setAttribute("corBorda", forma.getCorBordaHex());
        el.setAttribute("espessura", String.valueOf(forma.getEspessuraBorda()));
        if (forma.getGroupId() != null) {
            el.setAttribute("grupo", forma.getGroupId());
        }
        if (forma instanceof Circulo circulo) {
            el.setAttribute("raio", String.valueOf(circulo.getRaio()));
        }
        for (Ponto p : forma.getPontos()) {
            Element pe = doc.createElement("ponto");
            pe.setAttribute("x", String.valueOf(p.getX()));
            pe.setAttribute("y", String.valueOf(p.getY()));
            el.appendChild(pe);
        }
        return el;
    }

    // ------------------------------------------------------------------ leitura

    private static FormaGeometrica lerForma(Element el) throws PersistenciaException {
        String tipo = el.getAttribute("tipo");
        List<Ponto> pontos = lerPontos(el);

        FormaGeometrica forma = switch (tipo) {
            case "CIRCULO" -> {
                if (pontos.size() != 1) {
                    throw new PersistenciaException("Círculo deve ter exatamente um centro.");
                }
                yield new Circulo(pontos.get(0), Double.parseDouble(el.getAttribute("raio")));
            }
            case "POLIGONO" -> new Poligono(pontos);
            case "QUADRADO" -> {
                Quadrado q = new Quadrado();
                pontos.forEach(q::adicionarPonto);
                yield q;
            }
            case "HEXAGONO" -> {
                Hexagono h = new Hexagono();
                pontos.forEach(h::adicionarPonto);
                yield h;
            }
            default -> throw new PersistenciaException("Tipo de forma desconhecido: '" + tipo + "'.");
        };

        forma.setCorPreenchimentoHex(el.getAttribute("corPreenchimento"));
        forma.setCorBordaHex(el.getAttribute("corBorda"));
        forma.setEspessuraBorda(Double.parseDouble(el.getAttribute("espessura")));
        if (el.hasAttribute("grupo")) {
            forma.setGroupId(el.getAttribute("grupo"));
        }
        return forma;
    }

    private static List<Ponto> lerPontos(Element el) {
        List<Ponto> pontos = new ArrayList<>();
        NodeList nos = el.getElementsByTagName("ponto");
        for (int i = 0; i < nos.getLength(); i++) {
            Element pe = (Element) nos.item(i);
            pontos.add(new Ponto(
                    Double.parseDouble(pe.getAttribute("x")),
                    Double.parseDouble(pe.getAttribute("y"))));
        }
        return pontos;
    }
}
