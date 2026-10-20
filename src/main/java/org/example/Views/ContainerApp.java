package org.example.Views;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.Controller.EditorContext;
import org.example.Controller.FerramentaSelecionar;
import org.example.Controller.GerenciadorPersistencia;
import org.example.Controller.PersistenciaException;
import org.example.Models.FormaGeometrica;
import org.example.Models.Poligono;
import org.example.Models.Ponto;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Janela principal: menus, painel de desenho, barra lateral e barra de status.
 * Também coordena abrir/salvar e a confirmação de alterações não salvas.
 */
public class ContainerApp extends Application {

    private EditorContext ctx;
    private Stage stage;
    private Path arquivoAtual;
    private final Label status = new Label();
    private final Label coordenadas = new Label("(0, 0)");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        ctx = new EditorContext();
        ctx.setFerramenta(new FerramentaSelecionar());

        PainelDesenho painel = new PainelDesenho(ctx);
        painel.setAoMoverCursor(p -> coordenadas.setText(p.toString()));
        ScrollPane areaDesenho = new ScrollPane(painel);
        areaDesenho.setPannable(false);
        areaDesenho.setStyle("-fx-background: #FFFFFF; -fx-background-color: #FFFFFF;");

        ScrollPane lateral = new ScrollPane(new BarraLateralView(ctx));
        lateral.setFitToWidth(true);
        lateral.setPrefWidth(250);

        HBox barraStatus = new HBox(24, status, coordenadas);
        barraStatus.setPadding(new Insets(6, 10, 6, 10));
        barraStatus.setStyle("-fx-background-color: #F3F4F6; -fx-border-color: #D1D5DB; -fx-border-width: 1 0 0 0;");

        BorderPane raiz = new BorderPane();
        raiz.setTop(criarMenu());
        raiz.setLeft(lateral);
        raiz.setCenter(areaDesenho);
        raiz.setBottom(barraStatus);

        Scene cena = new Scene(raiz, 1200, 720);
        cena.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                ctx.concluirAcao();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                ctx.cancelarAcao();
            }
        });

        ctx.adicionarOuvinte(this::atualizarStatus);
        atualizarStatus();

        stage.setScene(cena);
        stage.setOnCloseRequest(e -> {
            if (!confirmarDescarte()) {
                e.consume();
            }
        });
        stage.show();
    }

    // ------------------------------------------------------------------ menu

    private MenuBar criarMenu() {
        MenuItem novo = item("Novo", new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), this::novo);
        MenuItem abrir = item("Abrir...", new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::abrir);
        MenuItem salvar = item("Salvar", new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::salvar);
        MenuItem salvarComo = item("Salvar como...", null, this::salvarComo);
        MenuItem sair = item("Sair", null, this::sair);
        Menu arquivo = new Menu("Arquivo", null, novo, abrir, salvar, salvarComo, new javafx.scene.control.SeparatorMenuItem(), sair);

        MenuItem desfazer = item("Desfazer", new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), ctx::desfazer);
        MenuItem refazer = item("Refazer", new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), ctx::refazer);
        MenuItem excluir = item("Excluir seleção", new KeyCodeCombination(KeyCode.DELETE), ctx::removerSelecionadas);
        MenuItem agrupar = item("Agrupar", new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN), ctx::agrupar);
        MenuItem desagrupar = item("Desagrupar",
                new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), ctx::desagrupar);
        Menu editar = new Menu("Editar", null, desfazer, refazer,
                new javafx.scene.control.SeparatorMenuItem(), excluir, agrupar, desagrupar);

        return new MenuBar(arquivo, editar);
    }

    private static MenuItem item(String rotulo, KeyCombination atalho, Runnable acao) {
        MenuItem m = new MenuItem(rotulo);
        m.setAccelerator(atalho);
        m.setOnAction(e -> acao.run());
        return m;
    }

    // ------------------------------------------------------------------ arquivo

    private void novo() {
        if (confirmarDescarte()) {
            ctx.novoDocumento();
            arquivoAtual = null;
            stage.setTitle(titulo());
        }
    }

    private void abrir() {
        if (!confirmarDescarte()) {
            return;
        }
        FileChooser escolha = seletorXml("Abrir desenho");
        File arquivo = escolha.showOpenDialog(stage);
        if (arquivo == null) {
            return;
        }
        try {
            GerenciadorPersistencia.Documento doc = GerenciadorPersistencia.carregar(arquivo.toPath());
            ctx.carregarDocumento(doc.largura(), doc.altura(), doc.grade(), doc.snap(), doc.formas());
            arquivoAtual = arquivo.toPath();
            stage.setTitle(titulo());
        } catch (PersistenciaException e) {
            alertaErro("Não foi possível abrir o arquivo",
                    "O desenho atual foi mantido.\n\n" + e.getMessage());
        }
    }

    private void salvar() {
        if (arquivoAtual == null) {
            salvarComo();
            return;
        }
        try {
            GerenciadorPersistencia.salvar(arquivoAtual, ctx);
            stage.setTitle(titulo());
        } catch (PersistenciaException e) {
            alertaErro("Não foi possível salvar", e.getMessage());
        }
    }

    private void salvarComo() {
        File destino = seletorXml("Salvar desenho como").showSaveDialog(stage);
        if (destino == null) {
            return;
        }
        arquivoAtual = destino.toPath().toString().toLowerCase().endsWith(".xml")
                ? destino.toPath()
                : Path.of(destino.getPath() + ".xml");
        salvar();
    }

    private void sair() {
        if (confirmarDescarte()) {
            Platform.exit();
        }
    }

    private FileChooser seletorXml(String titulo) {
        FileChooser fc = new FileChooser();
        fc.setTitle(titulo);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Desenho XML", "*.xml"));
        return fc;
    }

    /**
     * Pergunta antes de descartar alterações. Retorna {@code true} se a ação pode prosseguir.
     */
    private boolean confirmarDescarte() {
        if (!ctx.isAlterado()) {
            return true;
        }
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Alterações não salvas");
        alerta.setHeaderText("Existem alterações não salvas.");
        alerta.setContentText("Deseja salvar antes de continuar?");
        ButtonType salvarBtn = new ButtonType("Salvar");
        ButtonType descartarBtn = new ButtonType("Descartar");
        ButtonType cancelarBtn = ButtonType.CANCEL;
        alerta.getButtonTypes().setAll(salvarBtn, descartarBtn, cancelarBtn);

        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isEmpty() || resposta.get() == cancelarBtn) {
            return false;
        }
        if (resposta.get() == salvarBtn) {
            salvar();
            return !ctx.isAlterado();
        }
        return true;
    }

    private void alertaErro(String cabecalho, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Erro");
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }

    // ------------------------------------------------------------------ status

    private void atualizarStatus() {
        double area = 0;
        double perimetro = 0;
        for (FormaGeometrica f : ctx.getSelecionadas()) {
            if (f instanceof Poligono && !((Poligono) f).isValido()) {
                continue;
            }
            area += f.calcularArea();
            perimetro += f.calcularPerimetro();
        }
        status.setText(String.format(
                "Ferramenta: %s   |   Formas: %d   |   Selecionadas: %d   |   Área: %.2f   |   Perímetro: %.2f   |   Grade: %.0f%s",
                ctx.getFerramenta() == null ? "-" : ctx.getFerramenta().getNome(),
                ctx.getFormas().size(),
                ctx.getSelecionadas().size(),
                area,
                perimetro,
                ctx.getTamanhoGrade(),
                ctx.isSnapAtivo() ? " (snap)" : ""));
        if (stage != null) {
            stage.setTitle(titulo());
        }
    }

    private String titulo() {
        String nome = arquivoAtual == null ? "Sem título" : arquivoAtual.getFileName().toString();
        return "Editor Vetorial - " + nome + (ctx.isAlterado() ? " *" : "");
    }
}
