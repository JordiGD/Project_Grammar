package org.example.view;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.generator.StringGenerator;
import org.example.model.Grammar;
import org.example.model.Production;
import org.example.parser.ParseResult;
import org.example.parser.Parser;
import org.example.parser.ParserFactory;
import org.example.parser.Type3Parser;
import org.example.persistence.GrammarPersistence;

import java.io.File;
import java.util.*;

public class JavaFXApp extends Application {
    private Grammar currentGrammar;
    private Stage primaryStage;

    private TextArea grammarDisplay;
    private TextArea resultDisplay;
    private TextField inputField;
    private ListView<String> generatedStrings;
    private Label statusLabel;
    private Button parseButton;
    private Button generateButton;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Analizador Sintáctico - UPTC");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(createMenuBar());
        root.setCenter(createMainContent());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1200, 700);

        scene.getRoot().setStyle("-fx-base: #2c3e50; -fx-accent: #3498db; -fx-default-button: #3498db;");

        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        scene.getStylesheets().add("data:text/css," +
                ".menu-bar { -fx-background-color: #2c3e50 !important; } " +
                ".menu-bar .menu { -fx-text-fill: white !important; } " +
                ".menu-bar .label { -fx-text-fill: white !important; } " +
                ".menu { -fx-text-fill: white !important; } " +
                ".menu .label { -fx-text-fill: white !important; }");

        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(() -> forceMenuStylesAggressive(root));
        Platform.runLater(() -> {
            try {
                Thread.sleep(100);
                forceMenuStylesAggressive(root);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        updateUI();
    }

    private void forceMenuStylesAggressive(BorderPane root) {
        MenuBar menuBar = (MenuBar) root.getTop();
        if (menuBar != null) {
            menuBar.getScene().getRoot().setStyle("-fx-base: #2c3e50; -fx-accent: white;");

            menuBar.setStyle(
                    "-fx-background-color: #2c3e50 !important; " +
                            "-fx-text-fill: white !important; " +
                            "-fx-base: #2c3e50 !important;"
            );

            for (Menu menu : menuBar.getMenus()) {
                menu.setStyle(
                        "-fx-text-fill: white !important; " +
                                "-fx-font-weight: bold !important; " +
                                "-fx-background-color: transparent !important; " +
                                "-fx-base: #2c3e50 !important;"
                );

                menu.getStyleClass().clear();
                menu.getStyleClass().addAll("menu", "white-text-menu");
            }

            applyWhiteTextToAllNodes(menuBar);
        }
    }

    private void applyWhiteTextToAllNodes(javafx.scene.Node node) {
        try {
            if (node instanceof Label) {
                Label label = (Label) node;
                label.setTextFill(javafx.scene.paint.Color.WHITE);
                label.setStyle("-fx-text-fill: white !important; -fx-font-weight: bold;");
            }

            if (node instanceof javafx.scene.Parent) {
                javafx.scene.Parent parent = (javafx.scene.Parent) node;
                for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                    applyWhiteTextToAllNodes(child);
                }
            }
        } catch (Exception e) {
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        menuBar.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");

        Menu fileMenu = new Menu("Archivo");
        fileMenu.setStyle(WHITE_MENU_STYLE);

        MenuItem newGrammar = new MenuItem("Nueva Gramática");
        newGrammar.setOnAction(e -> showNewGrammarDialog());

        MenuItem loadGrammar = new MenuItem("Cargar Gramática...");
        loadGrammar.setOnAction(e -> loadGrammarFromFile());

        MenuItem saveGrammar = new MenuItem("Guardar Gramática...");
        saveGrammar.setOnAction(e -> saveGrammarToFile());

        MenuItem exit = new MenuItem("Salir");
        exit.setOnAction(e -> primaryStage.close());

        fileMenu.getItems().addAll(newGrammar, loadGrammar, saveGrammar,
                new SeparatorMenuItem(), exit);

        menuBar.getMenus().addAll(fileMenu);
        return menuBar;
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35, 0.65);

        VBox leftPanel = createGrammarPanel();
        VBox centerPanel = createParserPanel();
        VBox rightPanel = createGeneratorPanel();

        splitPane.getItems().addAll(leftPanel, centerPanel, rightPanel);
        return splitPane;
    }

    private VBox createGrammarPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label title = new Label("📋 Gramática Actual");
        title.setStyle(TITLE_STYLE);

        grammarDisplay = new TextArea();
        grammarDisplay.setEditable(false);
        grammarDisplay.setPrefRowCount(20);
        grammarDisplay.setWrapText(true);
        grammarDisplay.setStyle("-fx-font-family: 'Courier New';");

        Button editButton = new Button("✏️ Editar");
        editButton.setOnAction(e -> showNewGrammarDialog());

        panel.getChildren().addAll(title, grammarDisplay, editButton);
        VBox.setVgrow(grammarDisplay, Priority.ALWAYS);

        return panel;
    }

    private VBox createParserPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label title = new Label("🔍 Analizador Sintáctico");
        title.setStyle(TITLE_STYLE);

        HBox inputBox = new HBox(10);
        Label inputLabel = new Label("Cadena:");
        inputField = new TextField();
        inputField.setPromptText("Ingrese la cadena a parsear (ε para cadena vacía)");
        parseButton = new Button("Parsear");
        parseButton.setOnAction(e -> parseString());
        parseButton.setDisable(true);

        inputBox.getChildren().addAll(inputLabel, inputField, parseButton);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        resultDisplay = new TextArea();
        resultDisplay.setEditable(false);
        resultDisplay.setWrapText(true);
        resultDisplay.setPrefRowCount(15);
        resultDisplay.setStyle("-fx-font-family: 'Courier New';");

        panel.getChildren().addAll(title, inputBox, resultDisplay);
        VBox.setVgrow(resultDisplay, Priority.ALWAYS);

        return panel;
    }

    private VBox createGeneratorPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        Label title = new Label("🔢 Generador de Cadenas");
        title.setStyle(TITLE_STYLE);

        generateButton = new Button("Generar 10 Cadenas Más Cortas");
        generateButton.setOnAction(e -> generateStrings());
        generateButton.setDisable(true);

        generatedStrings = new ListView<>();
        generatedStrings.setPrefHeight(400);

        panel.getChildren().addAll(title, generateButton, generatedStrings);
        VBox.setVgrow(generatedStrings, Priority.ALWAYS);

        return panel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Sin gramática cargada");
        statusBar.getChildren().add(statusLabel);

        return statusBar;
    }

    private void showNewGrammarDialog() {
        Dialog<Grammar> dialog = new Dialog<>();
        dialog.setTitle("Nueva Gramática");
        dialog.setHeaderText("Definir Gramática Formal");

        ButtonType createButton = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label typeLabel = new Label("Tipo:");
        ComboBox<Grammar.GrammarType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(Grammar.GrammarType.TYPE_2, Grammar.GrammarType.TYPE_3);
        typeCombo.setValue(Grammar.GrammarType.TYPE_2);

        Label ntLabel = new Label("No Terminales:");
        TextField ntField = new TextField("S,A,B");
        ntField.setPromptText("Separados por comas");

        Label tLabel = new Label("Terminales:");
        TextField tField = new TextField("a,b");
        tField.setPromptText("Separados por comas");

        Label startLabel = new Label("Símbolo Inicial:");
        TextField startField = new TextField("S");

        Label prodLabel = new Label("Producciones:");
        TextArea prodArea = new TextArea("S→aSa\nS→bSb\nS→ε");
        prodArea.setPrefRowCount(10);
        prodArea.setPromptText("Una por línea: A→α o A->α");

        grid.add(typeLabel, 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(ntLabel, 0, 1);
        grid.add(ntField, 1, 1);
        grid.add(tLabel, 0, 2);
        grid.add(tField, 1, 2);
        grid.add(startLabel, 0, 3);
        grid.add(startField, 1, 3);
        grid.add(prodLabel, 0, 4);
        grid.add(prodArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButton) {
                try {
                    Set<String> nt = parseSet(ntField.getText());
                    Set<String> t = parseSet(tField.getText());
                    String start = startField.getText().trim();
                    List<Production> prods = parseProductions(prodArea.getText());

                    return new Grammar(nt, t, prods, start, typeCombo.getValue());
                } catch (Exception e) {
                    showError("Error al crear gramática", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        Optional<Grammar> result = dialog.showAndWait();
        result.ifPresent(grammar -> {
            currentGrammar = grammar;
            updateUI();
            statusLabel.setText("Gramática creada exitosamente");
        });
    }

    private void loadGrammarFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Gramática");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                currentGrammar = GrammarPersistence.load(file.getAbsolutePath());
                updateUI();
                statusLabel.setText("Gramática cargada: " + file.getName());
            } catch (Exception e) {
                showError("Error al cargar", e.getMessage());
            }
        }
    }

    private void saveGrammarToFile() {
        if (currentGrammar == null) {
            showWarning("Sin Gramática", "No hay gramática para guardar");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Gramática");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fileChooser.setInitialFileName("grammar.json");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                GrammarPersistence.save(currentGrammar, file.getAbsolutePath());
                statusLabel.setText("Gramática guardada: " + file.getName());
            } catch (Exception e) {
                showError("Error al guardar", e.getMessage());
            }
        }
    }

    private void parseString() {
        if (currentGrammar == null) return;

        String input = inputField.getText();
        Parser parser = ParserFactory.createParser(currentGrammar);

        ParseResult result = parser.parse(input);
        resultDisplay.setText(result.toString());

        statusLabel.setText(result.isAccepted() ? "✓ Cadena Aceptada" : "✗ Cadena Rechazada");
    }

    private void generateStrings() {
        if (currentGrammar == null) return;

        StringGenerator generator = new StringGenerator(currentGrammar);
        List<String> strings = generator.generateStrings(10);

        generatedStrings.getItems().clear();
        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);
            int len = str.equals("ε") ? 0 : str.length();
            generatedStrings.getItems().add(String.format("%2d. \"%s\" (longitud: %d)", i + 1, str, len));
        }

        statusLabel.setText("Generadas " + strings.size() + " cadenas");
    }

    private static final String TITLE_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold;";
    private static final String WHITE_MENU_STYLE =
            "-fx-text-fill: white !important; " +
                    "-fx-background-color: transparent !important; " +
                    "-fx-font-weight: bold !important; " +
                    "-fx-base: #2c3e50 !important;";

    private void updateUI() {
        boolean hasGrammar = currentGrammar != null;

        if (hasGrammar) {
            grammarDisplay.setText(currentGrammar.toString());

            if (currentGrammar.getType() == Grammar.GrammarType.TYPE_3) {
                Type3Parser parser = new Type3Parser(currentGrammar);
            }
        } else {
            grammarDisplay.setText("No hay gramática cargada.\n\nUse el menú para crear o cargar una gramática.");
        }

        parseButton.setDisable(!hasGrammar);
        generateButton.setDisable(!hasGrammar);
        inputField.setDisable(!hasGrammar);

        resultDisplay.clear();
        generatedStrings.getItems().clear();
    }

    private Set<String> parseSet(String input) {
        Set<String> result = new HashSet<>();
        for (String s : input.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<Production> parseProductions(String input) {
        List<Production> prods = new ArrayList<>();
        for (String line : input.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("→|->|:");
            if (parts.length == 2) {
                prods.add(new Production(parts[0].trim(), parts[1].trim()));
            }
        }
        return prods;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}