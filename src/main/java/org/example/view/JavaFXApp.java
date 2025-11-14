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

    // Componentes UI
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

        // Layout principal
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Menu Bar
        root.setTop(createMenuBar());

        // Center: Main Content
        root.setCenter(createMainContent());

        // Bottom: Status Bar
        root.setBottom(createStatusBar());

        // Scene con configuración especial para sobrescribir tema del sistema
        Scene scene = new Scene(root, 1200, 700);
        
        // Aplicar estilos ANTES del CSS para que tengan prioridad
        scene.getRoot().setStyle("-fx-base: #2c3e50; -fx-accent: #3498db; -fx-default-button: #3498db;");
        
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Aplicar estilos CSS inline adicionales para forzar colores
        scene.getStylesheets().add("data:text/css," + 
            ".menu-bar { -fx-background-color: #2c3e50 !important; } " +
            ".menu-bar .menu { -fx-text-fill: white !important; } " +
            ".menu-bar .label { -fx-text-fill: white !important; } " +
            ".menu { -fx-text-fill: white !important; } " +
            ".menu .label { -fx-text-fill: white !important; }");
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Múltiples intentos de forzar estilos con diferentes delays
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

    /**
     * Método agresivo para forzar estilos de menú que funciona con cualquier tema
     */
    private void forceMenuStylesAggressive(BorderPane root) {
        MenuBar menuBar = (MenuBar) root.getTop();
        if (menuBar != null) {
            // Configurar tema base para toda la aplicación
            menuBar.getScene().getRoot().setStyle("-fx-base: #2c3e50; -fx-accent: white;");
            
            // Estilo muy agresivo para MenuBar
            menuBar.setStyle(
                "-fx-background-color: #2c3e50 !important; " +
                "-fx-text-fill: white !important; " +
                "-fx-base: #2c3e50 !important;"
            );
            
            // Aplicar estilos a cada menú individualmente
            for (Menu menu : menuBar.getMenus()) {
                // Estilo directo muy específico
                menu.setStyle(
                    "-fx-text-fill: white !important; " +
                    "-fx-font-weight: bold !important; " +
                    "-fx-background-color: transparent !important; " +
                    "-fx-base: #2c3e50 !important;"
                );
                
                // Forzar clase CSS
                menu.getStyleClass().clear();
                menu.getStyleClass().addAll("menu", "white-text-menu");
            }
            
            // Aplicar también a todos los nodos hijos
            applyWhiteTextToAllNodes(menuBar);
        }
    }
    
    /**
     * Aplica texto blanco a todos los nodos recursivamente
     */
    private void applyWhiteTextToAllNodes(javafx.scene.Node node) {
        try {
            // Si es un Label, aplicar color blanco
            if (node instanceof Label) {
                Label label = (Label) node;
                label.setTextFill(javafx.scene.paint.Color.WHITE);
                label.setStyle("-fx-text-fill: white !important; -fx-font-weight: bold;");
            }
            
            // Si es un Parent, aplicar recursivamente a todos los hijos
            if (node instanceof javafx.scene.Parent) {
                javafx.scene.Parent parent = (javafx.scene.Parent) node;
                for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                    applyWhiteTextToAllNodes(child);
                }
            }
        } catch (Exception e) {
            // Continuar silenciosamente si hay error
        }
    }
    


    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Aplicar estilos completos para asegurar visibilidad
        menuBar.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");

        // Menú Archivo con estilo explícito
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

        // Menú Ejemplos con estilo explícito
        Menu examplesMenu = new Menu("Ejemplos");
        examplesMenu.setStyle(WHITE_MENU_STYLE);

        MenuItem arithmetic = new MenuItem("Expresiones Aritméticas (simple)");
        arithmetic.setOnAction(e -> loadExample("arithmetic"));

        MenuItem arithmeticId = new MenuItem("Expresiones con Identificadores");
        arithmeticId.setOnAction(e -> loadExample("arithmetic_id"));

        MenuItem palindrome = new MenuItem("Palíndromos Pares");
        palindrome.setOnAction(e -> loadExample("palindrome"));

        MenuItem anbn = new MenuItem("Lenguaje a^n b^n");
        anbn.setOnAction(e -> loadExample("anbn"));

        MenuItem identifier = new MenuItem("Identificadores (Tipo 3)");
        identifier.setOnAction(e -> loadExample("identifier"));

        examplesMenu.getItems().addAll(arithmetic, arithmeticId, palindrome, anbn, identifier);

        // Menú Ayuda con estilo explícito
        Menu helpMenu = new Menu("Ayuda");
        helpMenu.setStyle(WHITE_MENU_STYLE);

        MenuItem about = new MenuItem("Acerca de");
        about.setOnAction(e -> showAboutDialog());

        MenuItem manual = new MenuItem("Manual de Uso");
        manual.setOnAction(e -> showManualDialog());

        helpMenu.getItems().addAll(manual, about);

        menuBar.getMenus().addAll(fileMenu, examplesMenu, helpMenu);
        return menuBar;
    }

    private SplitPane createMainContent() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35, 0.65);

        // Panel izquierdo: Gramática
        VBox leftPanel = createGrammarPanel();

        // Panel central: Parser
        VBox centerPanel = createParserPanel();

        // Panel derecho: Generador
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

        // Input area
        HBox inputBox = new HBox(10);
        Label inputLabel = new Label("Cadena:");
        inputField = new TextField();
        inputField.setPromptText("Ingrese la cadena a parsear (ε para cadena vacía)");
        parseButton = new Button("Parsear");
        parseButton.setOnAction(e -> parseString());
        parseButton.setDisable(true);

        inputBox.getChildren().addAll(inputLabel, inputField, parseButton);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Result area
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

        // Botones
        ButtonType createButton = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        // Grid para inputs
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Tipo
        Label typeLabel = new Label("Tipo:");
        ComboBox<Grammar.GrammarType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(Grammar.GrammarType.TYPE_2, Grammar.GrammarType.TYPE_3);
        typeCombo.setValue(Grammar.GrammarType.TYPE_2);

        // No terminales
        Label ntLabel = new Label("No Terminales:");
        TextField ntField = new TextField("S,A,B");
        ntField.setPromptText("Separados por comas");

        // Terminales
        Label tLabel = new Label("Terminales:");
        TextField tField = new TextField("a,b");
        tField.setPromptText("Separados por comas");

        // Símbolo inicial
        Label startLabel = new Label("Símbolo Inicial:");
        TextField startField = new TextField("S");

        // Producciones
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

        // Converter
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

    // Constantes para estilos repetidos
    private static final String TITLE_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold;";
    private static final String WHITE_MENU_STYLE = 
        "-fx-text-fill: white !important; " +
        "-fx-background-color: transparent !important; " +
        "-fx-font-weight: bold !important; " +
        "-fx-base: #2c3e50 !important;";

    private void loadExample(String type) {
        switch (type) {
            case "arithmetic":
                createArithmeticGrammar();
                break;
            case "arithmetic_id":
                createArithmeticIdGrammar();
                break;
            case "palindrome":
                createPalindromeGrammar();
                break;
            case "anbn":
                createAnBnGrammar();
                break;
            case "identifier":
                createIdentifierGrammar();
                break;
            default:
                // Caso por defecto - no hacer nada
                statusLabel.setText("Ejemplo no encontrado: " + type);
                return;
        }
        updateUI();
    }

    private void updateUI() {
        boolean hasGrammar = currentGrammar != null;

        if (hasGrammar) {
            grammarDisplay.setText(currentGrammar.toString());

            // Mostrar info del parser
            if (currentGrammar.getType() == Grammar.GrammarType.TYPE_3) {
                Type3Parser parser = new Type3Parser(currentGrammar);
                grammarDisplay.appendText("\n" + parser.toAutomatonDescription());
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

    // Métodos auxiliares
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

    // Gramáticas de ejemplo (igual que ConsoleUI)
    private void createArithmeticGrammar() {
        Set<String> nt = new HashSet<>(Arrays.asList("E", "T", "F"));
        Set<String> t = new HashSet<>(Arrays.asList("+", "*", "(", ")", "x"));
        List<Production> p = Arrays.asList(
                new Production("E", "E + T"), new Production("E", "T"),
                new Production("T", "T * F"), new Production("T", "F"),
                new Production("F", "( E )"), new Production("F", "x")
        );
        currentGrammar = new Grammar(nt, t, p, "E", Grammar.GrammarType.TYPE_2);
        statusLabel.setText("Gramática de ejemplo: Expresiones Aritméticas (usar 'x' para variables)");
    }

    private void createArithmeticIdGrammar() {
        // Ejemplo con símbolos multi-carácter correctamente separados por espacios
        Set<String> nt = new HashSet<>(Arrays.asList("E", "T", "F"));
        Set<String> t = new HashSet<>(Arrays.asList("+", "*", "(", ")", "id"));
        List<Production> p = Arrays.asList(
                new Production("E", "E + T"), new Production("E", "T"),
                new Production("T", "T * F"), new Production("T", "F"),
                new Production("F", "( E )"), new Production("F", "id")
        );
        currentGrammar = new Grammar(nt, t, p, "E", Grammar.GrammarType.TYPE_2);
        statusLabel.setText("Gramática de ejemplo: Expresiones con identificadores multi-carácter (usar 'id')");
    }

    private void createPalindromeGrammar() {
        Set<String> nt = new HashSet<>(Arrays.asList("S"));
        Set<String> t = new HashSet<>(Arrays.asList("a", "b"));
        List<Production> p = Arrays.asList(
                new Production("S", "a S a"), new Production("S", "b S b"),
                new Production("S", "ε")
        );
        currentGrammar = new Grammar(nt, t, p, "S", Grammar.GrammarType.TYPE_2);
        statusLabel.setText("Gramática de ejemplo: Palíndromos Pares");
    }

    private void createAnBnGrammar() {
        Set<String> nt = new HashSet<>(Arrays.asList("S"));
        Set<String> t = new HashSet<>(Arrays.asList("a", "b"));
        List<Production> p = Arrays.asList(
                new Production("S", "a S b"), new Production("S", "ε")
        );
        currentGrammar = new Grammar(nt, t, p, "S", Grammar.GrammarType.TYPE_2);
        statusLabel.setText("Gramática de ejemplo: a^n b^n");
    }

    private void createIdentifierGrammar() {
        Set<String> nt = new HashSet<>(Arrays.asList("S", "A"));
        Set<String> t = new HashSet<>(Arrays.asList("a", "b", "0", "1"));
        List<Production> p = Arrays.asList(
                new Production("S", "a A"), new Production("S", "b A"),
                new Production("A", "a A"), new Production("A", "b A"),
                new Production("A", "0 A"), new Production("A", "1 A"),
                new Production("A", "ε")
        );
        currentGrammar = new Grammar(nt, t, p, "S", Grammar.GrammarType.TYPE_3);
        statusLabel.setText("Gramática de ejemplo: Identificadores (Tipo 3)");
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

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Analizador Sintáctico - UPTC");
        alert.setContentText(
                "Versión 1.0\n\n" +
                        "Desarrollado para la asignatura de Lenguajes Formales\n" +
                        "Universidad Pedagógica y Tecnológica de Colombia\n" +
                        "Facultad de Ingeniería\n\n" +
                        "Características:\n" +
                        "• Parser para Gramáticas Tipo 2 y Tipo 3\n" +
                        "• Generación de árbol de derivación\n" +
                        "• Generador BFS de cadenas\n" +
                        "• Persistencia en JSON"
        );
        alert.showAndWait();
    }

    private void showManualDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Manual de Uso");
        alert.setHeaderText("Guía Rápida");
        alert.setContentText(
                "1. CREAR GRAMÁTICA:\n" +
                        "   Archivo → Nueva Gramática\n\n" +
                        "2. CARGAR EJEMPLO:\n" +
                        "   Ejemplos → Seleccionar uno\n\n" +
                        "3. PARSEAR CADENA:\n" +
                        "   Escribir en campo de entrada y click en Parsear\n\n" +
                        "4. GENERAR CADENAS:\n" +
                        "   Click en 'Generar 10 Cadenas Más Cortas'\n\n" +
                        "5. GUARDAR:\n" +
                        "   Archivo → Guardar Gramática"
        );
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
