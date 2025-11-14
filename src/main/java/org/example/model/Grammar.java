package org.example.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Grammar {

    private Set<String> nonTerminals;  // N
    private Set<String> terminals;     // T
    private List<Production> productions; // P
    private String startSymbol;        // S
    private GrammarType type;

    public Grammar() {
        this.nonTerminals = new HashSet<>();
        this.terminals = new HashSet<>();
        this.productions = new ArrayList<>();
    }

    public Grammar(Set<String> nonTerminals, Set<String> terminals,
                   List<Production> productions, String startSymbol, GrammarType type) {
        this.nonTerminals = new HashSet<>(nonTerminals);
        this.terminals = new HashSet<>(terminals);
        this.productions = new ArrayList<>(productions);
        this.startSymbol = startSymbol;
        this.type = type;
        validate();
    }

    public enum GrammarType {
        TYPE_2, TYPE_3
    }

    /**
     * Valida la consistencia de la gramática
     */
    private void validate() {
        if (!nonTerminals.contains(startSymbol)) {
            throw new IllegalArgumentException("El símbolo inicial debe estar en N");
        }

        for (Production p : productions) {
            if (!nonTerminals.contains(p.getLeft())) {
                throw new IllegalArgumentException(
                        "Lado izquierdo de producción debe ser no terminal: " + p.getLeft());
            }

            // Validar que los símbolos en el lado derecho sean válidos
            for (String symbol : p.getRightSymbols()) {
                if (!symbol.equals("ε") &&
                        !nonTerminals.contains(symbol) &&
                        !terminals.contains(symbol)) {
                    throw new IllegalArgumentException(
                            "Símbolo desconocido en producción: " + symbol);
                }
            }
        }
    }

    /**
     * Obtiene todas las producciones para un no terminal dado
     */
    public List<Production> getProductionsFor(String nonTerminal) {
        List<Production> result = new ArrayList<>();
        for (Production p : productions) {
            if (p.getLeft().equals(nonTerminal)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Verifica si un símbolo es terminal
     */
    public boolean isTerminal(String symbol) {
        return terminals.contains(symbol);
    }

    /**
     * Verifica si un símbolo es no terminal
     */
    public boolean isNonTerminal(String symbol) {
        return nonTerminals.contains(symbol);
    }

    // Getters y Setters
    public Set<String> getNonTerminals() {
        return new HashSet<>(nonTerminals);
    }

    public void setNonTerminals(Set<String> nonTerminals) {
        this.nonTerminals = new HashSet<>(nonTerminals);
    }

    public Set<String> getTerminals() {
        return new HashSet<>(terminals);
    }

    public void setTerminals(Set<String> terminals) {
        this.terminals = new HashSet<>(terminals);
    }

    public List<Production> getProductions() {
        return new ArrayList<>(productions);
    }

    public void setProductions(List<Production> productions) {
        this.productions = new ArrayList<>(productions);
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public void setStartSymbol(String startSymbol) {
        this.startSymbol = startSymbol;
    }

    public GrammarType getType() {
        return type;
    }

    public void setType(GrammarType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Gramática Tipo ").append(type).append("\n");
        sb.append("N = {").append(String.join(", ", nonTerminals)).append("}\n");
        sb.append("T = {").append(String.join(", ", terminals)).append("}\n");
        sb.append("S = ").append(startSymbol).append("\n");
        sb.append("P:\n");
        for (Production p : productions) {
            sb.append("  ").append(p).append("\n");
        }
        return sb.toString();
    }
}
