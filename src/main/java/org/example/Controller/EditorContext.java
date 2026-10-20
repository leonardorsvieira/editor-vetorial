package org.example.Controller;

import org.example.Models.FormaGeometrica;
import org.example.Models.Ponto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contexto do State pattern: guarda o documento (formas, seleção, configuração do canvas)
 * e delega os eventos do mouse à ferramenta ativa.
 */
public class EditorContext {

    public static final double LARGURA_PADRAO = 800;
    public static final double ALTURA_PADRAO = 600;
    public static final double GRADE_PADRAO = 20;
    private static final double DESLOCAMENTO_COPIA = 10;

    private Ferramenta ferramentaAtual;
    private final List<FormaGeometrica> formas = new ArrayList<>();
    private final List<FormaGeometrica> selecionadas = new ArrayList<>();
    private final HistoricoController historico = new HistoricoController();
    private final List<Runnable> ouvintes = new ArrayList<>();

    private FormaGeometrica formaEmConstrucao;
    private double largura = LARGURA_PADRAO;
    private double altura = ALTURA_PADRAO;
    private double tamanhoGrade = GRADE_PADRAO;
    private boolean snapAtivo;
    private boolean modoAdicionar;

    private String corPreenchimentoAtual = "#4A90E2";
    private String corBordaAtual = "#1F2937";
    private double espessuraAtual = 1.0;
    private boolean alterado;

    // ------------------------------------------------------------------ ferramentas

    public void setFerramenta(Ferramenta ferramenta) {
        this.ferramentaAtual = ferramenta;
        this.formaEmConstrucao = null;
        notificarOuvintes();
    }

    public Ferramenta getFerramenta() {
        return ferramentaAtual;
    }

    public void aoPressionarMouse(Ponto p) {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoPressionarMouse(this, ajustar(p));
        }
    }

    public void aoArrastarMouse(Ponto p) {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoArrastarMouse(this, ajustar(p));
        }
    }

    public void aoSoltarMouse(Ponto p) {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoSoltarMouse(this, ajustar(p));
        }
    }

    public void aoClicar(Ponto p) {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoClicar(this, ajustar(p));
        }
    }

    public void concluirAcao() {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoConcluir(this);
        }
    }

    public void cancelarAcao() {
        if (ferramentaAtual != null) {
            ferramentaAtual.aoCancelar(this);
        }
    }

    // ------------------------------------------------------------------ documento

    public List<FormaGeometrica> getFormas() {
        return Collections.unmodifiableList(formas);
    }

    /** Adiciona uma forma ao desenho, registrando o estado anterior no histórico. */
    public void adicionarForma(FormaGeometrica forma) {
        registrarHistorico();
        formas.add(forma);
        marcarAlterado();
    }

    /** Remove as formas selecionadas (com seus grupos). */
    public void removerSelecionadas() {
        if (selecionadas.isEmpty()) {
            return;
        }
        registrarHistorico();
        formas.removeAll(formasAfetadas());
        selecionadas.clear();
        marcarAlterado();
    }

    /** Salva o estado atual no histórico. Deve ser chamado antes de qualquer alteração. */
    public void registrarHistorico() {
        historico.registrarEstado(formas);
    }

    /** Substitui o documento inteiro (usado ao abrir um arquivo) sem gerar histórico. */
    public void carregarDocumento(double largura, double altura, double grade, boolean snap,
                                  List<FormaGeometrica> novasFormas) {
        this.largura = largura;
        this.altura = altura;
        this.tamanhoGrade = grade;
        this.snapAtivo = snap;
        formas.clear();
        formas.addAll(novasFormas);
        selecionadas.clear();
        formaEmConstrucao = null;
        historico.limpar();
        alterado = false;
        notificarOuvintes();
    }

    /** Novo documento vazio, mantendo a configuração do canvas. */
    public void novoDocumento() {
        carregarDocumento(LARGURA_PADRAO, ALTURA_PADRAO, GRADE_PADRAO, false, new ArrayList<>());
    }

    public void desfazer() {
        List<FormaGeometrica> anterior = historico.desfazer(formas);
        if (anterior != null) {
            restaurarFormas(anterior);
        }
    }

    public void refazer() {
        List<FormaGeometrica> proximo = historico.refazer(formas);
        if (proximo != null) {
            restaurarFormas(proximo);
        }
    }

    private void restaurarFormas(List<FormaGeometrica> novas) {
        formas.clear();
        formas.addAll(novas);
        selecionadas.clear();
        marcarAlterado();
    }

    public boolean podeDesfazer() {
        return historico.podeDesfazer();
    }

    public boolean podeRefazer() {
        return historico.podeRefazer();
    }

    /** Move a forma uma posição para frente (desenhada depois) ou para trás. */
    public void moverCamada(FormaGeometrica forma, boolean paraFrente) {
        int i = formas.indexOf(forma);
        int destino = paraFrente ? i + 1 : i - 1;
        if (i < 0 || destino < 0 || destino >= formas.size()) {
            return;
        }
        registrarHistorico();
        Collections.swap(formas, i, destino);
        marcarAlterado();
    }

    /** Forma mais à frente que contém o ponto, ou {@code null}. */
    public FormaGeometrica formaNoPonto(Ponto p) {
        for (int i = formas.size() - 1; i >= 0; i--) {
            FormaGeometrica f = formas.get(i);
            if (f.contemPonto(p.getX(), p.getY())) {
                return f;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ seleção e grupos

    public List<FormaGeometrica> getSelecionadas() {
        return Collections.unmodifiableList(selecionadas);
    }

    /**
     * Seleciona a forma; se ela pertencer a um grupo, todo o grupo é selecionado junto.
     * Com {@code adicionar=false} a seleção anterior é descartada.
     */
    public void selecionar(FormaGeometrica forma, boolean adicionar) {
        if (!adicionar) {
            selecionadas.clear();
        }
        for (FormaGeometrica f : formasDoGrupo(forma)) {
            if (!selecionadas.contains(f)) {
                selecionadas.add(f);
            }
        }
        notificarOuvintes();
    }

    public void limparSelecao() {
        selecionadas.clear();
        notificarOuvintes();
    }

    /** Formas afetadas por uma operação: a seleção expandida para os grupos completos. */
    public List<FormaGeometrica> formasAfetadas() {
        List<FormaGeometrica> afetadas = new ArrayList<>();
        for (FormaGeometrica f : selecionadas) {
            for (FormaGeometrica g : formasDoGrupo(f)) {
                if (!afetadas.contains(g)) {
                    afetadas.add(g);
                }
            }
        }
        return afetadas;
    }

    private List<FormaGeometrica> formasDoGrupo(FormaGeometrica forma) {
        List<FormaGeometrica> membros = new ArrayList<>();
        if (forma.getGroupId() == null) {
            membros.add(forma);
            return membros;
        }
        for (FormaGeometrica f : formas) {
            if (forma.getGroupId().equals(f.getGroupId())) {
                membros.add(f);
            }
        }
        return membros;
    }

    /** Agrupa as formas selecionadas sob um mesmo groupId (Composite). */
    public void agrupar() {
        if (selecionadas.size() < 2) {
            return;
        }
        registrarHistorico();
        String id = UUID.randomUUID().toString();
        for (FormaGeometrica f : selecionadas) {
            f.setGroupId(id);
        }
        marcarAlterado();
    }

    /**
     * Duplica as formas selecionadas (Prototype: usa {@code clonar()}) deslocando as cópias.
     * Grupos existentes são recriados com novos identificadores, para não se fundirem com o original.
     */
    public void duplicarSelecionadas() {
        if (selecionadas.isEmpty()) {
            return;
        }
        registrarHistorico();
        Map<String, String> novosGrupos = new HashMap<>();
        List<FormaGeometrica> copias = new ArrayList<>();
        for (FormaGeometrica original : selecionadas) {
            FormaGeometrica copia = original.clonar();
            copia.transladar(DESLOCAMENTO_COPIA, DESLOCAMENTO_COPIA);
            if (original.getGroupId() != null) {
                copia.setGroupId(novosGrupos.computeIfAbsent(original.getGroupId(), k -> UUID.randomUUID().toString()));
            }
            copias.add(copia);
        }
        formas.addAll(copias);
        selecionadas.clear();
        selecionadas.addAll(copias);
        marcarAlterado();
    }

    public void desagrupar() {
        List<FormaGeometrica> afetadas = formasAfetadas();
        if (afetadas.stream().noneMatch(f -> f.getGroupId() != null)) {
            return;
        }
        registrarHistorico();
        for (FormaGeometrica f : afetadas) {
            f.setGroupId(null);
        }
        marcarAlterado();
    }

    public boolean isModoAdicionar() {
        return modoAdicionar;
    }

    public void setModoAdicionar(boolean modoAdicionar) {
        this.modoAdicionar = modoAdicionar;
    }

    // ------------------------------------------------------------------ construção em andamento

    public FormaGeometrica getFormaEmConstrucao() {
        return formaEmConstrucao;
    }

    public void setFormaEmConstrucao(FormaGeometrica forma) {
        this.formaEmConstrucao = forma;
        notificarOuvintes();
    }

    // ------------------------------------------------------------------ canvas e grade

    /** Ajusta o ponto à grade quando o snap está ativo. */
    public Ponto ajustar(Ponto p) {
        if (!snapAtivo || tamanhoGrade <= 0) {
            return p.clonar();
        }
        return new Ponto(
                Math.round(p.getX() / tamanhoGrade) * tamanhoGrade,
                Math.round(p.getY() / tamanhoGrade) * tamanhoGrade);
    }

    public double getLargura() {
        return largura;
    }

    public double getAltura() {
        return altura;
    }

    public double getTamanhoGrade() {
        return tamanhoGrade;
    }

    public void setTamanhoGrade(double tamanhoGrade) {
        if (tamanhoGrade <= 0) {
            throw new IllegalArgumentException("O tamanho da grade deve ser positivo");
        }
        this.tamanhoGrade = tamanhoGrade;
        notificarOuvintes();
    }

    public boolean isSnapAtivo() {
        return snapAtivo;
    }

    public void setSnapAtivo(boolean snapAtivo) {
        this.snapAtivo = snapAtivo;
        notificarOuvintes();
    }

    // ------------------------------------------------------------------ estilo atual

    public String getCorPreenchimentoAtual() {
        return corPreenchimentoAtual;
    }

    public void setCorPreenchimentoAtual(String hex) {
        this.corPreenchimentoAtual = hex;
    }

    public String getCorBordaAtual() {
        return corBordaAtual;
    }

    public void setCorBordaAtual(String hex) {
        this.corBordaAtual = hex;
    }

    public double getEspessuraAtual() {
        return espessuraAtual;
    }

    public void setEspessuraAtual(double espessura) {
        this.espessuraAtual = espessura;
    }

    /** Altera a cor/espessura das formas selecionadas (com histórico). */
    public void aplicarEstiloSelecionadas(String corPreenchimento, String corBorda, double espessura) {
        List<FormaGeometrica> afetadas = formasAfetadas();
        if (afetadas.isEmpty()) {
            return;
        }
        registrarHistorico();
        for (FormaGeometrica f : afetadas) {
            f.setCorPreenchimentoHex(corPreenchimento);
            f.setCorBordaHex(corBorda);
            f.setEspessuraBorda(espessura);
        }
        marcarAlterado();
    }

    // ------------------------------------------------------------------ estado de arquivo

    public boolean isAlterado() {
        return alterado;
    }

    public void marcarSalvo() {
        alterado = false;
        notificarOuvintes();
    }

    private void marcarAlterado() {
        alterado = true;
        notificarOuvintes();
    }

    // ------------------------------------------------------------------ observadores

    public void adicionarOuvinte(Runnable ouvinte) {
        ouvintes.add(ouvinte);
    }

    public void notificarOuvintes() {
        for (Runnable r : ouvintes) {
            r.run();
        }
    }

    /** Repinta a tela após uma alteração direta nas formas (ex.: durante um arraste). */
    public void atualizarVisual() {
        notificarOuvintes();
    }
}
