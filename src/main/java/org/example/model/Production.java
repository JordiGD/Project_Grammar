package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Production {

    private String left;      // Lado izquierdo (no terminal)
    private String right;     // Lado derecho completo
    private List<String> rightSymbols; // Símbolos del lado derecho parseados

    public Production(String left, String right) {
        this.left = left;
        this.right = right;
        this.rightSymbols = parseRight(right);
    }

    /**
     * Parsea el lado derecho separando los símbolos
     * Los símbolos deben estar separados por espacios para símbolos multi-carácter
     * Sin espacios, cada carácter se trata como un símbolo individual
     */
    private List<String> parseRight(String right) {
        List<String> symbols = new ArrayList<>();

        // Caso especial: cadena vacía
        if (right.equals("ε") || right.equals("epsilon") || right.isEmpty()) {
            symbols.add("ε");
            return symbols;
        }

        // Si contiene espacios, dividir por espacios (símbolos multi-carácter)
        if (right.contains(" ")) {
            String[] parts = right.split("\\s+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    symbols.add(part);
                }
            }
        } else {
            // Si no hay espacios, cada carácter es un símbolo individual
            // NOTA: Para símbolos como "id", "num", etc., use espacios: "F -> id" no "F->id"
            for (char c : right.toCharArray()) {
                symbols.add(String.valueOf(c));
            }
        }

        return symbols;
    }

    /**
     * Verifica si esta producción es epsilon (produce cadena vacía)
     */
    public boolean isEpsilonProduction() {
        return rightSymbols.size() == 1 && rightSymbols.get(0).equals("ε");
    }

    /**
     * Obtiene la longitud de la producción (número de símbolos)
     */
    public int length() {
        if (isEpsilonProduction()) {
            return 0;
        }
        return rightSymbols.size();
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public List<String> getRightSymbols() {
        return new ArrayList<>(rightSymbols);
    }

    @Override
    public String toString() {
        return left + " → " + right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }
}
