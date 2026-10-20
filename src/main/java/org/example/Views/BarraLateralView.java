package org.example.Views;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.Controller.EditorContext;
import org.example.Controller.FerramentaDesenhar;
import org.example.Controller.FerramentaEditarVertice;
import org.example.Controller.FerramentaSelecionar;
import org.example.Controller.FerramentaTransformar;
import org.example.Models.FormaGeometrica;

import java.util.List;

/**
 * Painel lateral: troca de ferramentas, cores/espessura, grade, camadas e comandos de edição.
 */
public class BarraLateralView extends VBox {

    private final EditorContext ctx;
    private final ToggleGroup grupoFerramentas = new ToggleGroup();
    private final ComboBox<FerramentaTransformar.Modo> modoTransformacao = new ComboBox<>(
            FXCollections.observableArrayList(FerramentaTransformar.Modo.values()));
    private final ColorPicker corPreenchimento = new ColorPicker(Color.web("#4A90E2"));
    private final ColorPicker corBorda = new ColorPicker(Color.web("#1F2937"));
    private final Spinner<Double> espessura = new Spinner<>(0.5, 20.0, 1.0, 0.5);
    private final CheckBox snap = new CheckBox("Snap to grid");
    private final Spinner<Double> grade = new Spinner<>(5.0, 200.0, EditorContext.GRADE_PADRAO, 5.0);
    private final ListView<FormaGeometrica> camadas = new ListView<>();
    private final ObservableList<FormaGeometrica> itensCamadas = FXCollections.observableArrayList();
    private boolean sincronizando;

    public BarraLateralView(EditorContext ctx) {
        this.ctx = ctx;
        setSpacing(12);
        setPadding(new Insets(12));
        setPrefWidth(230);
        getChildren().addAll(
                titulo("Ferramentas"), criarFerramentas(),
                titulo("Estilo"), criarEstilo(),
                titulo("Grade"), criarGrade(),
                titulo("Camadas"), criarCamadas(),
                titulo("Edição"), criarComandos());

        ctx.adicionarOuvinte(this::sincronizar);
        sincronizar();
    }

    // ------------------------------------------------------------------ ferramentas

    private VBox criarFerramentas() {
        VBox caixa = new VBox(6);
        ToggleButton selecionar = ferramenta("Selecionar", new FerramentaSelecionar());
        ToggleButton editarVertices = ferramenta("Editar vértices", new FerramentaEditarVertice());

        modoTransformacao.setValue(FerramentaTransformar.Modo.TRANSLADAR);
        modoTransformacao.setMaxWidth(Double.MAX_VALUE);
        ToggleButton transformar = new ToggleButton("Transformar");
        transformar.setToggleGroup(grupoFerramentas);
        transformar.setMaxWidth(Double.MAX_VALUE);
        transformar.setOnAction(e -> ativar(transformar,
                () -> new FerramentaTransformar(modoTransformacao.getValue())));
        // Reage a cada mudança de valor (inclusive ao passar o mouse sobre as opções abertas).
        modoTransformacao.valueProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && transformar.isSelected()) {
                ctx.setFerramenta(new FerramentaTransformar(novo));
            }
        });
        modoTransformacao.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FerramentaTransformar.Modo modo, boolean vazio) {
                super.updateItem(modo, vazio);
                setText(vazio || modo == null ? null : modo.getRotulo());
                setOnMouseEntered(e -> {
                    if (!vazio && modo != null) {
                        modoTransformacao.setValue(modo);
                    }
                });
            }
        });
        modoTransformacao.setButtonCell(modoTransformacao.getCellFactory().call(null));

        caixa.getChildren().addAll(
                selecionar,
                transformar, modoTransformacao,
                editarVertices,
                ferramenta("Polígono", new FerramentaDesenhar(FerramentaDesenhar.Tipo.POLIGONO)),
                ferramenta("Círculo", new FerramentaDesenhar(FerramentaDesenhar.Tipo.CIRCULO)),
                ferramenta("Quadrado", new FerramentaDesenhar(FerramentaDesenhar.Tipo.QUADRADO)),
                ferramenta("Hexágono", new FerramentaDesenhar(FerramentaDesenhar.Tipo.HEXAGONO)));
        selecionar.setSelected(true);
        ctx.setFerramenta(new FerramentaSelecionar());
        return caixa;
    }

    private ToggleButton ferramenta(String rotulo, org.example.Controller.Ferramenta instancia) {
        ToggleButton botao = new ToggleButton(rotulo);
        botao.setToggleGroup(grupoFerramentas);
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setOnAction(e -> ativar(botao, () -> instancia));
        return botao;
    }

    /** Impede que nenhuma ferramenta fique selecionada e troca a ferramenta do contexto. */
    private void ativar(ToggleButton botao, java.util.function.Supplier<org.example.Controller.Ferramenta> fabrica) {
        if (!botao.isSelected()) {
            botao.setSelected(true);
            return;
        }
        ctx.setFerramenta(fabrica.get());
    }

    // ------------------------------------------------------------------ estilo

    private VBox criarEstilo() {
        espessura.setEditable(true);
        espessura.setMaxWidth(Double.MAX_VALUE);
        corPreenchimento.valueProperty().addListener((obs, antigo, novo) -> aoMudarEstilo());
        corBorda.valueProperty().addListener((obs, antigo, novo) -> aoMudarEstilo());
        espessura.valueProperty().addListener((obs, antigo, novo) -> aoMudarEstilo());
        // Confirma a espessura digitada a cada tecla, sem esperar Enter ou perda de foco.
        espessura.getEditor().textProperty().addListener((obs, antigo, novo) -> {
            try {
                espessura.increment(0);
            } catch (RuntimeException ignorado) {
                // texto parcial inválido — mantém o último valor válido
            }
        });

        VBox caixa = new VBox(6,
                new Label("Preenchimento"), corPreenchimento,
                new Label("Borda"), corBorda,
                new Label("Espessura"), espessura);
        ctx.setCorPreenchimentoAtual(hex(corPreenchimento.getValue()));
        ctx.setCorBordaAtual(hex(corBorda.getValue()));
        ctx.setEspessuraAtual(espessura.getValue());
        return caixa;
    }

    /** Atualiza o estilo padrão e, se houver seleção, aplica a mudança na hora. */
    private void aoMudarEstilo() {
        if (sincronizando) {
            return;
        }
        Double esp = espessura.getValue();
        if (esp == null) {
            return;
        }
        String preenchimento = hex(corPreenchimento.getValue());
        String borda = hex(corBorda.getValue());
        ctx.setCorPreenchimentoAtual(preenchimento);
        ctx.setCorBordaAtual(borda);
        ctx.setEspessuraAtual(esp);
        if (!ctx.getSelecionadas().isEmpty()) {
            ctx.aplicarEstiloSelecionadas(preenchimento, borda, esp);
        }
    }

    private VBox criarGrade() {
        grade.setEditable(true);
        grade.setMaxWidth(Double.MAX_VALUE);
        snap.setSelected(ctx.isSnapAtivo());
        snap.setOnAction(e -> ctx.setSnapAtivo(snap.isSelected()));
        grade.valueProperty().addListener((obs, antigo, novo) -> {
            if (!sincronizando && novo != null) {
                ctx.setTamanhoGrade(novo);
            }
        });
        // Confirma o valor digitado a cada tecla, sem esperar Enter ou perda de foco.
        grade.getEditor().textProperty().addListener((obs, antigo, novo) -> {
            try {
                grade.increment(0);
            } catch (RuntimeException ignorado) {
                // texto parcial inválido (ex.: "1") — mantém o último valor válido
            }
        });
        return new VBox(6, snap, new Label("Tamanho da grade"), grade);
    }

    // ------------------------------------------------------------------ camadas

    private VBox criarCamadas() {
        camadas.setItems(itensCamadas);
        camadas.setPrefHeight(160);
        camadas.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FormaGeometrica forma, boolean vazio) {
                super.updateItem(forma, vazio);
                if (vazio || forma == null) {
                    setText(null);
                } else {
                    int indice = ctx.getFormas().indexOf(forma) + 1;
                    String grupo = forma.getGroupId() == null ? "" : " [grupo]";
                    setText(indice + ". " + forma.getTipo() + grupo);
                }
            }
        });
        camadas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (!sincronizando && novo != null) {
                ctx.selecionar(novo, false);
            }
        });

        Button frente = new Button("Trazer p/ frente");
        Button tras = new Button("Enviar p/ trás");
        frente.setMaxWidth(Double.MAX_VALUE);
        tras.setMaxWidth(Double.MAX_VALUE);
        frente.setOnAction(e -> moverSelecionadaCamada(true));
        tras.setOnAction(e -> moverSelecionadaCamada(false));
        return new VBox(6, camadas, frente, tras);
    }

    private void moverSelecionadaCamada(boolean paraFrente) {
        FormaGeometrica forma = camadas.getSelectionModel().getSelectedItem();
        if (forma != null) {
            ctx.moverCamada(forma, paraFrente);
        }
    }

    // ------------------------------------------------------------------ comandos

    private VBox criarComandos() {
        Button agrupar = botao("Agrupar", ctx::agrupar);
        Button desagrupar = botao("Desagrupar", ctx::desagrupar);
        Button duplicar = botao("Duplicar (clonar)", ctx::duplicarSelecionadas);
        Button excluir = botao("Excluir seleção", ctx::removerSelecionadas);
        HBox grupos = new HBox(6, agrupar, desagrupar);
        HBox.setHgrow(agrupar, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(desagrupar, javafx.scene.layout.Priority.ALWAYS);
        return new VBox(6, grupos, duplicar, excluir);
    }

    private Button botao(String rotulo, Runnable acao) {
        Button b = new Button(rotulo);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> acao.run());
        return b;
    }

    // ------------------------------------------------------------------ sincronização

    /** Atualiza a lista de camadas e a seleção conforme o estado do contexto. */
    private void sincronizar() {
        sincronizando = true;
        try {
            List<FormaGeometrica> formas = ctx.getFormas().stream().toList();
            itensCamadas.setAll(formas);
            camadas.getSelectionModel().clearSelection();
            for (FormaGeometrica f : ctx.getSelecionadas()) {
                camadas.getSelectionModel().select(f);
            }
            if (!ctx.getSelecionadas().isEmpty()) {
                FormaGeometrica primeira = ctx.getSelecionadas().get(0);
                corPreenchimento.setValue(Color.web(primeira.getCorPreenchimentoHex()));
                corBorda.setValue(Color.web(primeira.getCorBordaHex()));
                espessura.getValueFactory().setValue(primeira.getEspessuraBorda());
            } else {
                espessura.getValueFactory().setValue(ctx.getEspessuraAtual());
            }
            grade.getValueFactory().setValue(ctx.getTamanhoGrade());
            snap.setSelected(ctx.isSnapAtivo());
        } finally {
            sincronizando = false;
        }
    }

    private static Label titulo(String texto) {
        Label l = new Label(texto);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return l;
    }

    private static String hex(Color cor) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(cor.getRed() * 255),
                (int) Math.round(cor.getGreen() * 255),
                (int) Math.round(cor.getBlue() * 255));
    }
}
