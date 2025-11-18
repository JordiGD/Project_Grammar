package org.example.generator;

import org.example.model.Grammar;
import org.example.model.Production;

import java.util.*;

public class StringGenerator {

    private Grammar grammar;
    private static final int MAX_ITERATIONS = 10000;

    public StringGenerator(Grammar grammar) {
        this.grammar = grammar;
    }

    /**
     * Genera las primeras n cadenas del lenguaje ordenadas por longitud
     */
    public List<String> generateStrings(int n) {
        Set<String> generated = new LinkedHashSet<>();
        Queue<DerivationState> queue = new LinkedList<>();

        DerivationState initial = new DerivationState();
        initial.sententialForm = Collections.singletonList(grammar.getStartSymbol());
        initial.depth = 0;
        queue.offer(initial);

        int iterations = 0;

        while (!queue.isEmpty() && generated.size() < n && iterations < MAX_ITERATIONS) {
            iterations++;
            DerivationState current = queue.poll();

            // Si la forma sentencial solo tiene terminales, es una cadena del lenguaje
            if (isTerminalString(current.sententialForm)) {
                String str = sententialFormToString(current.sententialForm);
                generated.add(str);
                continue;
            }

            // Expandir el primer no terminal (leftmost derivation)
            int nonTerminalIndex = findFirstNonTerminal(current.sententialForm);

            if (nonTerminalIndex == -1) {
                continue;
            }

            String nonTerminal = current.sententialForm.get(nonTerminalIndex);
            List<Production> productions = grammar.getProductionsFor(nonTerminal);

            // Aplicar cada producción
            for (Production prod : productions) {
                DerivationState newState = applyProduction(current, nonTerminalIndex, prod);

                // Limitar profundidad para evitar bucles infinitos
                if (newState.depth < 20 && newState.sententialForm.size() < 50) {
                    queue.offer(newState);
                }
            }
        }

        return new ArrayList<>(generated);
    }

    /**
     * Aplica una producción a una forma sentencial
     */
    private DerivationState applyProduction(DerivationState state, int index, Production prod) {
        DerivationState newState = new DerivationState();
        newState.depth = state.depth + 1;
        newState.sententialForm = new ArrayList<>();

        // Copiar símbolos antes del índice
        for (int i = 0; i < index; i++) {
            newState.sententialForm.add(state.sententialForm.get(i));
        }

        // Agregar símbolos de la producción (excepto epsilon)
        if (!prod.isEpsilonProduction()) {
            newState.sententialForm.addAll(prod.getRightSymbols());
        }

        // Copiar símbolos después del índice
        for (int i = index + 1; i < state.sententialForm.size(); i++) {
            newState.sententialForm.add(state.sententialForm.get(i));
        }

        return newState;
    }

    /**
     * Encuentra el primer no terminal en la forma sentencial
     */
    private int findFirstNonTerminal(List<String> sententialForm) {
        for (int i = 0; i < sententialForm.size(); i++) {
            if (grammar.isNonTerminal(sententialForm.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Verifica si una forma sentencial solo contiene terminales
     */
    private boolean isTerminalString(List<String> sententialForm) {
        if (sententialForm.isEmpty()) {
            return true;
        }

        for (String symbol : sententialForm) {
            if (!grammar.isTerminal(symbol) && !symbol.equals("ε")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convierte una forma sentencial a cadena
     */
    private String sententialFormToString(List<String> sententialForm) {
        if (sententialForm.isEmpty()) {
            return "ε";
        }

        StringBuilder sb = new StringBuilder();
        for (String symbol : sententialForm) {
            if (!symbol.equals("ε")) {
                sb.append(symbol);
            }
        }

        return sb.length() == 0 ? "ε" : sb.toString();
    }

    /**
     * Estado de derivación para BFS
     */
    private static class DerivationState {
        List<String> sententialForm;
        int depth;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DerivationState that = (DerivationState) o;
            return Objects.equals(sententialForm, that.sententialForm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sententialForm);
        }
    }
}
