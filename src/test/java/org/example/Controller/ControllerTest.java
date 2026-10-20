package org.example.Controller;

import org.example.Models.Circulo;
import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    @Test
    void desfazerERefazerRestauramEstados() {
        EditorContext ctx = new EditorContext();
        ctx.adicionarForma(new Circulo(new Ponto(0, 0), 5));
        ctx.adicionarForma(new Circulo(new Ponto(10, 10), 5));
        assertEquals(2, ctx.getFormas().size());

        ctx.desfazer();
        assertEquals(1, ctx.getFormas().size());
        ctx.desfazer();
        assertEquals(0, ctx.getFormas().size());
        assertFalse(ctx.podeDesfazer());

        ctx.refazer();
        assertEquals(1, ctx.getFormas().size());
    }

    @Test
    void novaAcaoLimpaPilhaDeRefazer() {
        EditorContext ctx = new EditorContext();
        ctx.adicionarForma(new Circulo(new Ponto(0, 0), 5));
        ctx.desfazer();
        assertTrue(ctx.podeRefazer());
        ctx.adicionarForma(new Circulo(new Ponto(1, 1), 2));
        assertFalse(ctx.podeRefazer());
    }

    @Test
    void historicoGuardaCopiasProfundas() {
        EditorContext ctx = new EditorContext();
        FormaGeometrica c = new Circulo(new Ponto(0, 0), 5);
        ctx.adicionarForma(c);
        ctx.registrarHistorico(); // snapshot com a forma em x = 0
        c.transladar(50, 50);     // altera a forma original depois de registrada
        ctx.desfazer();
        assertEquals(0.0, ctx.getFormas().get(0).getCentroide().getX(), 1e-9,
                "o snapshot não pode ser afetado pela alteração posterior");
    }

    @Test
    void agrupamentoTransformaOGrupoComoUmaForma() {
        EditorContext ctx = new EditorContext();
        FormaGeometrica a = new Circulo(new Ponto(0, 0), 5);
        FormaGeometrica b = new Circulo(new Ponto(100, 0), 5);
        ctx.adicionarForma(a);
        ctx.adicionarForma(b);
        ctx.selecionar(a, false);
        ctx.selecionar(b, true);
        ctx.agrupar();

        ctx.selecionar(a, false);
        assertEquals(2, ctx.getSelecionadas().size(), "selecionar um membro seleciona o grupo");
        ctx.formasAfetadas();
        ctx.desagrupar();
        assertNull(a.getGroupId());
        assertNull(b.getGroupId());
    }

    @Test
    void duplicarUsaPrototypeEAgrupaCopias() {
        EditorContext ctx = new EditorContext();
        Poligono p = new Poligono(List.of(new Ponto(0, 0), new Ponto(5, 0), new Ponto(0, 5)));
        ctx.adicionarForma(p);
        ctx.selecionar(p, false);
        ctx.duplicarSelecionadas();
        assertEquals(2, ctx.getFormas().size());
        assertNotSame(p, ctx.getFormas().get(1));
        assertEquals(10.0, ctx.getFormas().get(1).getPontos().get(0).getX(), 1e-9);
    }

    @Test
    void snapAjustaPontosAGrade() {
        EditorContext ctx = new EditorContext();
        ctx.setTamanhoGrade(20);
        ctx.setSnapAtivo(true);
        Ponto p = ctx.ajustar(new Ponto(31, 49));
        assertEquals(40.0, p.getX(), 1e-9);
        assertEquals(40.0, p.getY(), 1e-9);
    }

    @Test
    void persistenciaIdaEVolta(@TempDir Path dir) throws Exception {
        EditorContext ctx = new EditorContext();
        ctx.setTamanhoGrade(10);
        ctx.setSnapAtivo(true);
        Poligono p = new Poligono(List.of(new Ponto(0, 0), new Ponto(10, 0), new Ponto(0, 10)));
        p.setCorPreenchimentoHex("#112233");
        ctx.adicionarForma(p);
        ctx.adicionarForma(new Circulo(new Ponto(50, 50), 12.5));

        Path arquivo = dir.resolve("desenho.xml");
        GerenciadorPersistencia.salvar(arquivo, ctx);
        assertFalse(ctx.isAlterado());

        GerenciadorPersistencia.Documento doc = GerenciadorPersistencia.carregar(arquivo);
        assertEquals(10.0, doc.grade(), 1e-9);
        assertTrue(doc.snap());
        assertEquals(2, doc.formas().size());
        assertEquals("#112233", doc.formas().get(0).getCorPreenchimentoHex());
        assertEquals(12.5, ((Circulo) doc.formas().get(1)).getRaio(), 1e-9);
    }

    @Test
    void arquivoCorrompidoGeraExcecaoExplicativa(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("ruim.xml");
        Files.writeString(arquivo, "<desenho><forma tipo=\"CIRCULO\">");
        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> GerenciadorPersistencia.carregar(arquivo));
        assertTrue(e.getMessage().contains("corrompido"));
    }

    @Test
    void geometriaInvalidaNoArquivoGeraExcecao(@TempDir Path dir) throws Exception {
        Path arquivo = dir.resolve("invalido.xml");
        Files.writeString(arquivo, """
                <?xml version="1.0" encoding="UTF-8"?>
                <desenho versao="1" largura="800" altura="600" grade="20" snap="false">
                  <forma tipo="CIRCULO" corPreenchimento="#000000" corBorda="#000000" espessura="1.0" raio="-1">
                    <ponto x="0" y="0"/>
                  </forma>
                </desenho>
                """);
        assertThrows(PersistenciaException.class, () -> GerenciadorPersistencia.carregar(arquivo));
    }

    @Test
    void arquivoInexistenteGeraExcecao(@TempDir Path dir) {
        assertThrows(PersistenciaException.class,
                () -> GerenciadorPersistencia.carregar(dir.resolve("nao_existe.xml")));
    }
}
