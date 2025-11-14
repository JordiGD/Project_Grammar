package org.example.parser;

import org.example.model.DerivationTree;

public class ParseResult {
    private boolean accepted;
    private DerivationTree derivationTree;
    private String message;

    public ParseResult(boolean accepted, DerivationTree derivationTree, String message) {
        this.accepted = accepted;
        this.derivationTree = derivationTree;
        this.message = message;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public DerivationTree getDerivationTree() {
        return derivationTree;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado: ").append(accepted ? "ACEPTADA" : "RECHAZADA").append("\n");
        sb.append("Mensaje: ").append(message).append("\n");

        if (accepted && derivationTree != null) {
            sb.append(derivationTree.toGraphicalString());
        }

        return sb.toString();
    }
}