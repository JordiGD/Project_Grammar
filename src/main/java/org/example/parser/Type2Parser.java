package org.example.parser;

import org.example.model.DerivationTree;
import org.example.model.Grammar;
import org.example.model.Production;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Type2Parser implements Parser {

    private Grammar grammar;
    private int maxDepth;
    private int maxSteps;
    private Set<String> visitedStates;
    private int stepCount;

    public Type2Parser(Grammar grammar) {
        this.grammar = grammar;
        // Límites adaptativos basados en la complejidad de la gramática
        this.maxDepth = Math.max(20, grammar.getProductions().size() * 2);
        this.maxSteps = Math.max(1000, grammar.getProductions().size() * 100);
        this.visitedStates = new HashSet<>();
    }

    @Override
    public ParseResult parse(String input) {
        // Caso especial: cadena vacía
        if (input.isEmpty() || input.equals("ε")) {
            return parseEpsilon();
        }

        // Reinicializar contadores para cada parsing
        this.stepCount = 0;
        this.visitedStates = new HashSet<>();

        // Intentar parsing recursivo con backtracking mejorado
        DerivationTree tree = new DerivationTree(grammar.getStartSymbol());
        List<String> inputSymbols = tokenize(input);
        
        try {
            boolean accepted = parseRecursiveImproved(tree.getRoot(), inputSymbols, 0, 0);
            if (accepted) {
                // Verificación final: asegurar que la cadena generada coincida exactamente
                String generated = tree.getGeneratedString();
                String original = String.join("", inputSymbols);
                
                if (generated.equals(original) || (generated.equals("ε") && original.isEmpty())) {
                    return new ParseResult(true, tree, 
                        String.format("Cadena aceptada (pasos: %d, cadena: '%s')", stepCount, generated));
                } else {
                    return new ParseResult(false, null, 
                        String.format("Error interno: cadena generada '%s' no coincide con entrada '%s'", generated, original));
                }
            }
        } catch (ParseLimitExceededException e) {
            return new ParseResult(false, null, 
                "Parsing interrumpido: " + e.getMessage() + 
                ". La gramática podría ser ambigua o tener recursión infinita.");
        }

        return new ParseResult(false, null, 
            String.format("Cadena rechazada - no pertenece al lenguaje (pasos explorados: %d)", stepCount));
    }

    /**
     * Tokeniza la entrada en símbolos
     */
    private List<String> tokenize(String input) {
        List<String> symbols = new ArrayList<>();

        // Si los terminales incluyen símbolos multi-carácter, usar matching más inteligente
        Set<String> terminals = grammar.getTerminals();
        int i = 0;

        while (i < input.length()) {
            boolean matched = false;

            // Intentar match con terminales multi-carácter primero (más largos primero)
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
     * Parsing recursivo mejorado con control de límites inteligente
     */
    private boolean parseRecursiveImproved(DerivationTree.TreeNode node,
                                         List<String> input, int position, int depth) 
                                         throws ParseLimitExceededException {
        // Verificar límites
        if (++stepCount > maxSteps) {
            throw new ParseLimitExceededException("Máximo número de pasos excedido: " + maxSteps);
        }
        
        if (depth > maxDepth) {
            throw new ParseLimitExceededException("Profundidad máxima excedida: " + maxDepth);
        }

        String symbol = node.getSymbol();

        // Si es terminal, verificar match exacto
        if (grammar.isTerminal(symbol)) {
            // Debe haber entrada disponible Y debe coincidir exactamente
            if (position < input.size() && input.get(position).equals(symbol)) {
                return true;
            }
            // Si no hay coincidencia o no hay más entrada, fallar
            return false;
        }

        // Control de estados visitados para detectar ciclos
        String state = createStateKey(symbol, position, depth);
        if (visitedStates.contains(state)) {
            return false; // Evitar ciclo infinito
        }
        visitedStates.add(state);

        // Si es no terminal, probar todas las producciones con heurística
        List<Production> productions = grammar.getProductionsFor(symbol);
        
        // Ordenar producciones: epsilon al final, terminales primero
        productions.sort((p1, p2) -> {
            if (p1.isEpsilonProduction() && !p2.isEpsilonProduction()) return 1;
            if (!p1.isEpsilonProduction() && p2.isEpsilonProduction()) return -1;
            
            boolean p1StartsWithTerminal = !p1.getRightSymbols().isEmpty() && 
                                          grammar.isTerminal(p1.getRightSymbols().get(0));
            boolean p2StartsWithTerminal = !p2.getRightSymbols().isEmpty() && 
                                          grammar.isTerminal(p2.getRightSymbols().get(0));
            
            if (p1StartsWithTerminal && !p2StartsWithTerminal) return -1;
            if (!p1StartsWithTerminal && p2StartsWithTerminal) return 1;
            
            return Integer.compare(p1.length(), p2.length());
        });

        for (Production prod : productions) {
            // Crear nodos hijos según la producción
            List<DerivationTree.TreeNode> children = new ArrayList<>();
            for (String s : prod.getRightSymbols()) {
                children.add(new DerivationTree.TreeNode(s));
            }

            // Intentar parsear con esta producción
            try {
                if (tryProductionImproved(node, children, prod, input, position, depth + 1)) {
                    visitedStates.remove(state); // Limpiar estado exitoso
                    return true;
                }
            } catch (ParseLimitExceededException e) {
                visitedStates.remove(state);
                throw e;
            }
        }

        visitedStates.remove(state);
        return false;
    }

    /**
     * Crea una clave única para el estado actual del parsing
     */
    private String createStateKey(String symbol, int position, int depth) {
        return symbol + ":" + position + ":" + (depth % 10); // Limitar depth en la clave
    }

    /**
     * Excepción personalizada para límites de parsing
     */
    private static class ParseLimitExceededException extends Exception {
        public ParseLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Intenta aplicar una producción (versión mejorada)
     */
    private boolean tryProductionImproved(DerivationTree.TreeNode node,
                                        List<DerivationTree.TreeNode> children,
                                        Production production,
                                        List<String> input,
                                        int position,
                                        int depth) throws ParseLimitExceededException {
        // Caso especial: producción epsilon
        if (production.isEpsilonProduction()) {
            // Para nodo raíz con epsilon: solo aceptar si no hay más entrada
            // Para nodos internos con epsilon: siempre permitir (no consume entrada)
            boolean isRoot = (position == 0);
            if (isRoot) {
                // Nodo raíz con epsilon: solo si la entrada está vacía
                if (position >= input.size()) {
                    node.setUsedProduction(production);
                    node.addChild(new DerivationTree.TreeNode("ε"));
                    return true;
                }
                return false;
            } else {
                // Nodo interno con epsilon: permitir siempre
                node.setUsedProduction(production);
                node.addChild(new DerivationTree.TreeNode("ε"));
                return true;
            }
        }

        // Verificación temprana: si la producción es más larga que input restante
        int minTerminals = countMinTerminals(production);
        if (position + minTerminals > input.size()) {
            return false;
        }

        // Intentar parsear cada símbolo hijo
        int currentPos = position;
        List<DerivationTree.TreeNode> successfulChildren = new ArrayList<>();
        
        for (int i = 0; i < children.size(); i++) {
            DerivationTree.TreeNode child = children.get(i);
            
            if (currentPos > input.size()) {
                return false;
            }

            if (grammar.isTerminal(child.getSymbol())) {
                // Terminal: debe hacer match exacto
                if (currentPos < input.size() &&
                        input.get(currentPos).equals(child.getSymbol())) {
                    successfulChildren.add(child);
                    currentPos++;
                } else {
                    return false;
                }
            } else {
                // No terminal: recursión con límites
                if (parseRecursiveImproved(child, input, currentPos, depth)) {
                    successfulChildren.add(child);
                    // Calcular cuántos símbolos consumió este hijo
                    int consumedByChild = countConsumedSymbolsImproved(child);
                    currentPos += consumedByChild;
                    
                    // Verificación: si el símbolo no es nullable, DEBE consumir al menos algo
                    if (consumedByChild == 0 && !isNullable(child.getSymbol())) {
                        // Un no terminal no-nullable que no consumió nada es un error
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        // Verificación estricta: solo aceptar si se consume exactamente la entrada esperada
        boolean isRoot = (position == 0);
        boolean consumedAll = (currentPos == input.size());
        boolean madeProgress = (currentPos > position);
        
        // Para el nodo raíz, DEBE consumir toda la entrada
        // Para nodos internos, DEBE hacer algún progreso
        if (isRoot && consumedAll) {
            // Éxito: nodo raíz consumió toda la entrada
            node.setUsedProduction(production);
            for (DerivationTree.TreeNode child : successfulChildren) {
                node.addChild(child);
            }
            return true;
        } else if (!isRoot && madeProgress) {
            // Éxito: nodo interno hizo progreso
            node.setUsedProduction(production);
            for (DerivationTree.TreeNode child : successfulChildren) {
                node.addChild(child);
            }
            return true;
        }

        // Fallo: no se cumplen las condiciones de aceptación
        return false;
    }


    
    /**
     * Cuenta el número mínimo de terminales en una producción
     */
    private int countMinTerminals(Production production) {
        int count = 0;
        for (String symbol : production.getRightSymbols()) {
            if (grammar.isTerminal(symbol)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Verifica si un no terminal puede generar epsilon
     */
    private boolean isNullable(String symbol) {
        if (grammar.isTerminal(symbol)) return false;
        
        for (Production prod : grammar.getProductionsFor(symbol)) {
            if (prod.isEpsilonProduction()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Versión mejorada para contar símbolos consumidos
     */
    private int countConsumedSymbolsImproved(DerivationTree.TreeNode node) {
        if (node.getSymbol().equals("ε")) {
            return 0;
        }
        
        if (node.isLeaf()) {
            return grammar.isTerminal(node.getSymbol()) ? 1 : 0;
        }
        
        int count = 0;
        for (DerivationTree.TreeNode child : node.getChildren()) {
            count += countConsumedSymbolsImproved(child);
        }
        return count;
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
                return new ParseResult(true, tree, "Cadena vacía aceptada");
            }
        }

        return new ParseResult(false, null, "Cadena vacía no aceptada");
    }
}
