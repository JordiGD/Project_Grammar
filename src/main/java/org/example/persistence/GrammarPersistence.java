package org.example.persistence;

import org.example.model.Grammar;
import org.example.model.Production;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GrammarPersistence {

    /**
     * Guarda una gramática en un archivo
     */
    public static void save(Grammar grammar, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("{\n");

            // Tipo
            writer.write("  \"type\": \"" + grammar.getType() + "\",\n");

            // Símbolo inicial
            writer.write("  \"startSymbol\": \"" + grammar.getStartSymbol() + "\",\n");

            // No terminales
            writer.write("  \"nonTerminals\": [");
            writer.write(joinQuoted(grammar.getNonTerminals()));
            writer.write("],\n");

            // Terminales
            writer.write("  \"terminals\": [");
            writer.write(joinQuoted(grammar.getTerminals()));
            writer.write("],\n");

            // Producciones
            writer.write("  \"productions\": [\n");
            List<Production> productions = grammar.getProductions();
            for (int i = 0; i < productions.size(); i++) {
                Production p = productions.get(i);
                writer.write("    {\"left\": \"" + p.getLeft() +
                        "\", \"right\": \"" + escape(p.getRight()) + "\"}");
                if (i < productions.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  ]\n");

            writer.write("}\n");
        }
    }

    /**
     * Carga una gramática desde un archivo
     */
    public static Grammar load(String filename) throws IOException {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        return parseJSON(content.toString());
    }

    /**
     * Parser simple de JSON para gramáticas
     */
    private static Grammar parseJSON(String json) {
        Grammar grammar = new Grammar();

        // Extraer tipo
        String typeStr = extractValue(json, "type");
        grammar.setType(Grammar.GrammarType.valueOf(typeStr));

        // Extraer símbolo inicial
        grammar.setStartSymbol(extractValue(json, "startSymbol"));

        // Extraer no terminales
        String ntArray = extractArray(json, "nonTerminals");
        grammar.setNonTerminals(parseStringArray(ntArray));

        // Extraer terminales
        String tArray = extractArray(json, "terminals");
        grammar.setTerminals(parseStringArray(tArray));

        // Extraer producciones
        String prodArray = extractArray(json, "productions");
        grammar.setProductions(parseProductions(prodArray));

        return grammar;
    }

    private static String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String extractArray(String json, String key) {
        String pattern = "\"" + key + "\":\\s*\\[([^\\]]+)\\]";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern,
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static Set<String> parseStringArray(String arrayContent) {
        Set<String> result = new HashSet<>();
        String[] items = arrayContent.split(",");
        for (String item : items) {
            String cleaned = item.trim().replaceAll("\"", "");
            if (!cleaned.isEmpty()) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private static List<Production> parseProductions(String arrayContent) {
        List<Production> productions = new ArrayList<>();

        // Dividir por objetos
        String[] objects = arrayContent.split("\\},\\s*\\{");

        for (String obj : objects) {
            obj = obj.replaceAll("[{}]", "").trim();

            String left = extractProductionField(obj, "left");
            String right = extractProductionField(obj, "right");

            if (!left.isEmpty() && !right.isEmpty()) {
                productions.add(new Production(left, unescape(right)));
            }
        }

        return productions;
    }

    private static String extractProductionField(String obj, String field) {
        String pattern = "\"" + field + "\":\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(obj);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String joinQuoted(Set<String> items) {
        List<String> quoted = new ArrayList<>();
        for (String item : items) {
            quoted.add("\"" + item + "\"");
        }
        return String.join(", ", quoted);
    }

    private static String escape(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String str) {
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
