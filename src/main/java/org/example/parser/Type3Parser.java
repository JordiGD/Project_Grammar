package org.example.parser;

import org.example.model.DerivationTree;
import org.example.model.Grammar;
import org.example.model.Production;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Type3Parser implements Parser {

    private Grammar grammar;

    public Type3Parser(Grammar grammar) {
        this.grammar = grammar;
        validateType3();
    }

    /**
     * Valida que la gramática sea realmente Tipo 3
     */
    private void validateType3() {
        for (Production prod : grammar.getProductions()) {
            List<String> right = prod.getRightSymbols();

            // Producción epsilon es válida
            if (prod.isEpsilonProduction()) {
                continue;
            }

            // Regular derecha: debe ser aB o a
            // Máximo 2 símbolos: terminal + no terminal (opcional)
            if (right.size() > 2) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod +
                                " (tiene más de 2 símbolos en el lado derecho)");
            }

            // El primer símbolo debe ser terminal
            if (!grammar.isTerminal(right.get(0))) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod +
                                " (debe comenzar con terminal)");
            }

            // Si hay segundo símbolo, debe ser no terminal
            if (right.size() == 2 && !grammar.isNonTerminal(right.get(1))) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod +
                                " (el segundo símbolo debe ser no terminal)");
            }
        }
    }

    @Override
    public ParseResult parse(String input) {
        // Caso especial: cadena vacía
        if (input.isEmpty() || input.equals("ε")) {
            return parseEpsilon();
        }

        // Tokenizar entrada
        List<String> inputSymbols = tokenize(input);

        // Construir árbol mientras parseamos
        DerivationTree tree = new DerivationTree(grammar.getStartSymbol());

        // Simular autómata finito
        if (parseIterative(tree.getRoot(), inputSymbols)) {
            return new ParseResult(true, tree,
                    "Cadena aceptada (Parser Tipo 3 - " + inputSymbols.size() + " símbolos procesados)");
        }

        return new ParseResult(false, null, "Cadena rechazada por gramática regular");
    }

    /**
     * Parser iterativo que simula un autómata finito
     */
    private boolean parseIterative(DerivationTree.TreeNode root, List<String> input) {
        String currentState = grammar.getStartSymbol();
        DerivationTree.TreeNode currentNode = root;
        int position = 0;

        while (position < input.size()) {
            String currentSymbol = input.get(position);

            // Buscar una producción que consuma este símbolo
            Production usedProduction = null;
            String nextState = null;

            for (Production prod : grammar.getProductionsFor(currentState)) {
                if (prod.isEpsilonProduction()) {
                    continue;
                }

                List<String> right = prod.getRightSymbols();

                // Verificar si el terminal coincide
                if (right.get(0).equals(currentSymbol)) {
                    usedProduction = prod;
                    nextState = (right.size() == 2) ? right.get(1) : null;
                    break;
                }
            }

            if (usedProduction == null) {
                return false; // No hay transición posible
            }

            // Construir el árbol
            currentNode.setUsedProduction(usedProduction);
            DerivationTree.TreeNode terminalNode = new DerivationTree.TreeNode(currentSymbol);
            currentNode.addChild(terminalNode);

            // Si hay siguiente estado (no terminal), agregarlo
            if (nextState != null) {
                DerivationTree.TreeNode nextNode = new DerivationTree.TreeNode(nextState);
                currentNode.addChild(nextNode);
                currentNode = nextNode;
                currentState = nextState;
            } else {
                // No hay más estados, debe ser el final
                position++;
                if (position == input.size()) {
                    return true; // Aceptado
                } else {
                    return false; // Quedan símbolos sin procesar
                }
            }

            position++;
        }

        // Al terminar, verificar si el estado actual puede producir epsilon
        for (Production prod : grammar.getProductionsFor(currentState)) {
            if (prod.isEpsilonProduction()) {
                currentNode.setUsedProduction(prod);
                currentNode.addChild(new DerivationTree.TreeNode("ε"));
                return true;
            }
        }

        // Si no hay producción epsilon y aún hay un no terminal, rechazar
        return false;
    }

    /**
     * Tokeniza la entrada en símbolos
     */
    private List<String> tokenize(String input) {
        List<String> symbols = new ArrayList<>();
        Set<String> terminals = grammar.getTerminals();
        int i = 0;

        while (i < input.length()) {
            boolean matched = false;

            // Intentar match con terminales multi-carácter (más largos primero)
            List<String> sortedTerminals = new ArrayList<>(terminals);
            sortedTerminals.sort((a, b) -> b.length() - a.length());

            for (String terminal : sortedTerminals) {
                if (input.startsWith(terminal, i)) {
                    symbols.add(terminal);
                    i += terminal.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                // Si no hay match, tomar carácter individual
                symbols.add(String.valueOf(input.charAt(i)));
                i++;
            }
        }

        return symbols;
    }

    /**
     * Maneja el caso de cadena vacía
     */
    private ParseResult parseEpsilon() {
        List<Production> productions = grammar.getProductionsFor(grammar.getStartSymbol());

        for (Production prod : productions) {
            if (prod.isEpsilonProduction()) {
                DerivationTree tree = new DerivationTree(grammar.getStartSymbol());
                tree.getRoot().setUsedProduction(prod);
                tree.getRoot().addChild(new DerivationTree.TreeNode("ε"));
                return new ParseResult(true, tree,
                        "Cadena vacía aceptada (Parser Tipo 3)");
            }
        }

        return new ParseResult(false, null, "Cadena vacía no aceptada");
    }

    /**
     * Convierte la gramática Tipo 3 a descripción de autómata
     */
    public String toAutomatonDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Autómata Finito Equivalente ===\n\n");
        sb.append("Estados (Q): ").append(grammar.getNonTerminals()).append("\n");
        sb.append("Estado inicial (q0): ").append(grammar.getStartSymbol()).append("\n");
        sb.append("Alfabeto (Σ): ").append(grammar.getTerminals()).append("\n");
        sb.append("\nTransiciones (δ):\n");

        for (String state : grammar.getNonTerminals()) {
            for (Production prod : grammar.getProductionsFor(state)) {
                if (prod.isEpsilonProduction()) {
                    sb.append("  ").append(state).append(" es estado final (acepta ε)\n");
                } else {
                    List<String> right = prod.getRightSymbols();
                    String symbol = right.get(0);
                    String nextState = (right.size() == 2) ? right.get(1) : "ACCEPT";
                    sb.append("  δ(").append(state).append(", ").append(symbol)
                            .append(") = ").append(nextState).append("\n");
                }
            }
        }

        return sb.toString();
    }
}
