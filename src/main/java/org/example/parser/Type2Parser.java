package org.example.parser;

import org.example.model.DerivationTree;
import org.example.model.Grammar;
import org.example.model.Production;

import java.util.ArrayList;
import java.util.List;

public class Type2Parser implements Parser {

    private Grammar grammar;
    private int stepCount;
    private final int MAX_STEPS = 10000;

    public Type2Parser(Grammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public ParseResult parse(String input) {
        stepCount = 0;

        if (input.isEmpty() || input.equals("ε")) {
            return parseEpsilon();
        }

        List<String> inputSymbols = tokenize(input);
        DerivationTree tree = new DerivationTree(grammar.getStartSymbol());

        if (parseRecursive(tree.getRoot(), inputSymbols, 0)) {
            String generated = tree.getGeneratedString();
            String original = String.join("", inputSymbols);

            if (generated.equals(original)) {
                return new ParseResult(true, tree,
                        String.format("Cadena aceptada (pasos: %d, cadena: '%s')", stepCount, generated));
            }
        }

        return new ParseResult(false, null,
                String.format("Cadena rechazada - no pertenece al lenguaje (pasos explorados: %d)", stepCount));
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

    private boolean parseRecursive(DerivationTree.TreeNode node, List<String> input, int position) {
        if (++stepCount > MAX_STEPS) {
            return false;
        }

        String symbol = node.getSymbol();

        if (grammar.isTerminal(symbol)) {
            return position < input.size() && input.get(position).equals(symbol);
        }

        for (Production prod : grammar.getProductionsFor(symbol)) {
            if (tryProduction(node, prod, input, position)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryProduction(DerivationTree.TreeNode node, Production production,
                                  List<String> input, int position) {
        if (production.isEpsilonProduction()) {
            if (position == 0 && !input.isEmpty()) {
                return false;
            }
            node.setUsedProduction(production);
            node.addChild(new DerivationTree.TreeNode("ε"));
            return true;
        }

        List<DerivationTree.TreeNode> children = createChildren(production);
        int currentPos = position;
        List<DerivationTree.TreeNode> successfulChildren = new ArrayList<>();

        for (DerivationTree.TreeNode child : children) {
            if (grammar.isTerminal(child.getSymbol())) {
                if (currentPos < input.size() && input.get(currentPos).equals(child.getSymbol())) {
                    successfulChildren.add(child);
                    currentPos++;
                } else {
                    return false;
                }
            } else {
                if (parseRecursive(child, input, currentPos)) {
                    successfulChildren.add(child);
                    currentPos += countConsumedSymbols(child);
                } else {
                    return false;
                }
            }
        }

        node.setUsedProduction(production);
        for (DerivationTree.TreeNode child : successfulChildren) {
            node.addChild(child);
        }
        return true;
    }

    private List<DerivationTree.TreeNode> createChildren(Production production) {
        List<DerivationTree.TreeNode> children = new ArrayList<>();
        for (String s : production.getRightSymbols()) {
            children.add(new DerivationTree.TreeNode(s));
        }
        return children;
    }

    private int countConsumedSymbols(DerivationTree.TreeNode node) {
        if (node.getSymbol().equals("ε")) {
            return 0;
        }

        if (node.isLeaf()) {
            return grammar.isTerminal(node.getSymbol()) ? 1 : 0;
        }

        int count = 0;
        for (DerivationTree.TreeNode child : node.getChildren()) {
            count += countConsumedSymbols(child);
        }
        return count;
    }

    private ParseResult parseEpsilon() {
        for (Production prod : grammar.getProductionsFor(grammar.getStartSymbol())) {
            if (prod.isEpsilonProduction()) {
                DerivationTree tree = new DerivationTree(grammar.getStartSymbol());
                tree.getRoot().setUsedProduction(prod);
                tree.getRoot().addChild(new DerivationTree.TreeNode("ε"));
                return new ParseResult(true, tree, "Cadena vacía aceptada");
            }
        }
        return new ParseResult(false, null, "Cadena vacía no aceptada");
    }
}