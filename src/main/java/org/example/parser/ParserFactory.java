package org.example.parser;

import org.example.model.Grammar;

public class ParserFactory {

    /**
            * Crea el parser óptimo para la gramática dada
     */
    public static Parser createParser(Grammar grammar) {
        if (grammar == null) {
            throw new IllegalArgumentException("La gramática no puede ser nula");
        }

        switch (grammar.getType()) {
            case TYPE_3:
                return new Type3Parser(grammar);
            case TYPE_2:
                return new Type2Parser(grammar);
            default:
                throw new IllegalArgumentException("Tipo de gramática no soportado: " + grammar.getType());
        }
    }
}
