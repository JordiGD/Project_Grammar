package org.example.parser;

public interface Parser {
    /**
     * Parsea una cadena de entrada
     * @param input La cadena a parsear
     * @return Resultado del parsing
     */
    ParseResult parse(String input);
}
