package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class DerivationTree {

    private TreeNode root;

    public static class TreeNode {
        private String symbol;
        private List<TreeNode> children;
        private Production usedProduction;

        public TreeNode(String symbol) {
            this.symbol = symbol;
            this.children = new ArrayList<>();
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }

        public String getSymbol() {
            return symbol;
        }

        public List<TreeNode> getChildren() {
            return children;
        }

        public Production getUsedProduction() {
            return usedProduction;
        }

        public void setUsedProduction(Production production) {
            this.usedProduction = production;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    public DerivationTree(String startSymbol) {
        this.root = new TreeNode(startSymbol);
    }

    public TreeNode getRoot() {
        return root;
    }

    /**
     * Genera una representación textual del árbol usando indentación
     */
    public String toStringIndented() {
        StringBuilder sb = new StringBuilder();
        printNode(root, "", true, sb);
        return sb.toString();
    }

    private void printNode(TreeNode node, String prefix, boolean isTail, StringBuilder sb) {
        sb.append(prefix);
        sb.append(isTail ? "└── " : "├── ");
        sb.append(node.getSymbol());

        if (node.getUsedProduction() != null) {
            sb.append(" [").append(node.getUsedProduction()).append("]");
        }

        sb.append("\n");

        List<TreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printNode(children.get(i),
                    prefix + (isTail ? "    " : "│   "),
                    i == children.size() - 1,
                    sb);
        }
    }

    /**
     * Obtiene todas las hojas del árbol (símbolos terminales)
     */
    public List<String> getLeaves() {
        List<String> leaves = new ArrayList<>();
        collectLeaves(root, leaves);
        return leaves;
    }

    private void collectLeaves(TreeNode node, List<String> leaves) {
        if (node.isLeaf()) {
            if (!node.getSymbol().equals("ε")) {
                leaves.add(node.getSymbol());
            }
        } else {
            for (TreeNode child : node.getChildren()) {
                collectLeaves(child, leaves);
            }
        }
    }

    /**
     * Obtiene la cadena generada leyendo las hojas de izquierda a derecha
     */
    public String getGeneratedString() {
        List<String> leaves = getLeaves();
        if (leaves.isEmpty()) {
            return "ε";
        }
        return String.join("", leaves);
    }

    /**
     * Genera representación gráfica simple del árbol
     */
    public String toGraphicalString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Árbol de Derivación ===\n\n");
        sb.append(toStringIndented());
        sb.append("\nCadena generada: ").append(getGeneratedString()).append("\n");
        return sb.toString();
    }
}
