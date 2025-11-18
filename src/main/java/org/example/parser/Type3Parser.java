package org.example.parser;

import org.example.model.DerivationTree;
import org.example.model.Grammar;
import org.example.model.Production;

import java.util.ArrayList;
import java.util.List;

public class Type3Parser implements Parser {

    private Grammar grammar;

    public Type3Parser(Grammar grammar) {
        this.grammar = grammar;
        validateType3();
    }

    private void validateType3() {
        for (Production prod : grammar.getProductions()) {
            List<String> right = prod.getRightSymbols();

            if (prod.isEpsilonProduction()) {
                continue;
            }

            if (right.size() > 2) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod + " (tiene más de 2 símbolos)");
            }

            if (!grammar.isTerminal(right.get(0))) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod + " (debe comenzar con terminal)");
            }

            if (right.size() == 2 && !grammar.isNonTerminal(right.get(1))) {
                throw new IllegalArgumentException(
                        "Producción no es Tipo 3: " + prod + " (segundo símbolo debe ser no terminal)");
            }
        }
    }

    @Override
    public ParseResult parse(String input) {
        if (input.isEmpty() || input.equals("ε")) {
            return parseEpsilon();
        }

        List<String> inputSymbols = tokenize(input);
        DerivationTree tree = new DerivationTree(grammar.getStartSymbol());

        if (parseIterative(tree.getRoot(), inputSymbols)) {
            return new ParseResult(true, tree,
                    "Cadena aceptada (Parser Tipo 3 - " + inputSymbols.size() + " símbolos procesados)");
        }

        return new ParseResult(false, null, "Cadena rechazada por gramática regular");
    }

    private boolean parseIterative(DerivationTree.TreeNode root, List<String> input) {
        String currentState = grammar.getStartSymbol();
        DerivationTree.TreeNode currentNode = root;
        int position = 0;

        while (position < input.size()) {
            String currentSymbol = input.get(position);
            Production usedProduction = findProduction(currentState, currentSymbol);

            if (usedProduction == null) {
                return false;
            }

            List<String> right = usedProduction.getRightSymbols();
            String nextState = (right.size() == 2) ? right.get(1) : null;

            currentNode.setUsedProduction(usedProduction);
            currentNode.addChild(new DerivationTree.TreeNode(currentSymbol));

            if (nextState != null) {
                DerivationTree.TreeNode nextNode = new DerivationTree.TreeNode(nextState);
                currentNode.addChild(nextNode);
                currentNode = nextNode;
                currentState = nextState;
            } else {
                position++;
                return position == input.size();
            }

            position++;
        }

        for (Production prod : grammar.getProductionsFor(currentState)) {
            if (prod.isEpsilonProduction()) {
                currentNode.setUsedProduction(prod);
                currentNode.addChild(new DerivationTree.TreeNode("ε"));
                return true;
            }
        }

        return false;
    }

    private Production findProduction(String currentState, String symbol) {
        for (Production prod : grammar.getProductionsFor(currentState)) {
            if (prod.isEpsilonProduction()) {
                continue;
            }

            List<String> right = prod.getRightSymbols();
            if (right.get(0).equals(symbol)) {
                return prod;
            }
        }
        return null;
    }

    private List<String> tokenize(String input) {
        List<String> symbols = new ArrayList<>();
        int i = 0;

        while (i < input.length()) {
            boolean matched = false;

            for (String terminal : grammar.getTerminals()) {
                if (input.startsWith(terminal, i)) {
                    symbols.add(terminal);
                    i += terminal.length();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                symbols.add(String.valueOf(input.charAt(i)));
                i++;
            }
        }

        return symbols;
    }

    private ParseResult parseEpsilon() {
        for (Production prod : grammar.getProductionsFor(grammar.getStartSymbol())) {
            if (prod.isEpsilonProduction()) {
                DerivationTree tree = new DerivationTree(grammar.getStartSymbol());
                tree.getRoot().setUsedProduction(prod);
                tree.getRoot().addChild(new DerivationTree.TreeNode("ε"));
                return new ParseResult(true, tree, "Cadena vacía aceptada (Parser Tipo 3)");
            }
        }

        return new ParseResult(false, null, "Cadena vacía no aceptada");
    }
}