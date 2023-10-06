import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements the minmax algorithm with or without alpha-beta prune
 */
public class Minmax {

    private final Node root; // the root of the input tree
    private static int INFINITY;
    private static int NEG_INFINITY;

    enum Player {
        MIN, MAX
    }

    //Node for building the tree
    static class Node {
        String label;
        Integer value = null; // Not Null if leaf node
        List<Node> children = new ArrayList<>();

        Node(String label) {
            this.label = label;
        }
    }

    //action of node
    static class Result {
        int value;
        String move;

        Result(int value, String move) {
            this.value = value;
            this.move = move;
        }
    }


    public Minmax(Node root, int range) {
        this.root = root;
        INFINITY = range;
        NEG_INFINITY = -range;
    }

    private Result minimax(Node node, boolean isMaxPlayer, int alpha, int beta, boolean alphaBetaPruning, boolean verbose) {
        if (node.value != null) {
            return new Result(node.value, node.label);
        }

        int value = isMaxPlayer ? NEG_INFINITY : INFINITY;
        String bestMove = null;
        for (Node child : node.children) {
            Result childResult = minimax(child, !isMaxPlayer, alpha, beta, alphaBetaPruning, verbose);

            if (isMaxPlayer && childResult.value > value) {
                value = childResult.value;
                bestMove = child.label;
                alpha = Math.max(alpha, value);
            } else if (!isMaxPlayer && childResult.value < value) {
                value = childResult.value;
                bestMove = child.label;
                beta = Math.min(beta, value);
            }
            if (alphaBetaPruning && alpha >= beta) {
                break;
            }
        }

        if(alphaBetaPruning) {
            if (verbose && node != root && alpha < beta) {
                System.out.println((isMaxPlayer ? "max" : "min") + "(" + node.label + ") chooses " + bestMove + " for " + value);
            }
        } else {
            if (verbose && node != root) {
                System.out.println((isMaxPlayer ? "max" : "min") + "(" + node.label + ") chooses " + bestMove + " for " + value);
            }
        }
        return new Result(value, bestMove);
    }

    public String minimaxSearch(Node root, boolean isMaxPlayer, boolean verbose, boolean alphaBetaPruning) {
        Result result = minimax(root, isMaxPlayer, NEG_INFINITY, INFINITY, alphaBetaPruning, verbose);
        return (isMaxPlayer ? "max" : "min") + "(" + root.label + ") chooses " + result.move + " for " + result.value;
    }

    // Test the input tree
    public static void printNode(Node node) {
        System.out.print(node.label);

        if (node.value != null) {
            System.out.println(" (Leaf: " + node.value + ")");
        } else {
            System.out.println();
        }
        for (Node child : node.children) {
            printNode(child);
        }
    }

    //whether or not the tree has cycle
    private static boolean hasCycle(Node node, Set<Node> visited, Set<Node> recursionStack) {
        if (recursionStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visited.add(node);
        recursionStack.add(node);
        for (Node child : node.children) {
            if (hasCycle(child, visited, recursionStack)) {
                return true;
            }
        }
        recursionStack.remove(node);
        return false;
    }

    public static void main(String[] args) throws IOException {
        boolean verbose = false;
        boolean alphaBetaPruning = false;
        Player rootPlayer = Player.MAX;
        String graphFile = "";

        int range = Integer.MAX_VALUE;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-v":
                    verbose = true;
                    break;
                case "-ab":
                    alphaBetaPruning = true;
                    break;
                case "-range":
                    if (i + 1 < args.length) {
                        try {
                            range = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.out.println("Error: Invalid range value.");
                            return;
                        }
                    } else {
                        System.out.println("Error: Missing range value");
                        return;
                    }
                    break;
                case "min":
                    rootPlayer = Player.MIN;
                    break;
                case "max":
                    rootPlayer = Player.MAX;
                    break;
                default:
                    graphFile = args[i];
                    break;
            }

        }


        // Read the game tree from the file
        Map<String, Node> nodes = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(graphFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                if (line.contains("=")) {
                    String[] parts = line.split("=");
                    String label = parts[0].trim();
                    Node node = nodes.get(label);
                    if (node == null) {
                        node = new Node(label);
                        nodes.put(label, node);
                    }
                    node.value = Integer.parseInt(parts[1].trim());
                } else if (line.contains(":")) {
                    String[] parts = line.split(":");
                    Node node = nodes.getOrDefault(parts[0].trim(), new Node(parts[0].trim()));
                    String[] children = parts[1].trim().replace("[", "").replace("]", "").split(",");
                    for (String childLabel : children) {
                        Node child = nodes.getOrDefault(childLabel.trim(), new Node(childLabel.trim()));
                        node.children.add(child);
                        nodes.put(childLabel.trim(), child);
                    }
                    nodes.put(node.label, node);
                }
            }
        }

        // check whether it has cycle
        Set<Node> visited = new HashSet<>();
        Set<Node> stack = new HashSet<>();
        for (Node node : nodes.values()) {
            if (hasCycle(node, visited, stack)) {
                System.out.println("The tree has a cycle.");
                return;
            }
        }

        // find the roots
        List<Node> roots = new ArrayList<>();
        for (Node node : nodes.values()) {
            boolean isRoot = true;
            for (Node other : nodes.values()) {
                if (other.children.contains(node)) {
                    isRoot = false;
                    break;
                }
            }
            if (isRoot) {
                roots.add(node);
            }
        }

        // checking whether the root is empty or multiple roots
        if (roots.isEmpty()) {
            System.out.println("Error: No root node found Or The tree has a cycle");
            return;
        } else if (roots.size() > 1) {
            System.out.println("Error Multiple roots found: " + roots.stream().map(n -> "\"" + n.label + "\"").collect(Collectors.joining(" and ")));
            return;
        }

         //Check for missing leaf nodes
        for (Node node : nodes.values()) {
            for (Node child : node.children) {
                if (child.value == null && child.children.isEmpty()) {
                    System.out.println("Error: Child node \"" + child.label + "\" of \"" + node.label + "\" not found.");
                    return;
                }
            }
        }

        for (Node parent : nodes.values()) {
            List<Node> children = parent.children;
            if (children.isEmpty()) continue;

            boolean hasChildNodes = children.stream().anyMatch(child -> !child.children.isEmpty());
            boolean hasLeafNodes = children.stream().anyMatch(child -> child.children.isEmpty());

            if (hasChildNodes && hasLeafNodes) {
                String leafChildLabel = children.stream().filter(child -> child.children.isEmpty()).findFirst().get().label;
                System.out.println("Error: Child node \"" + leafChildLabel + "\" of \"" + parent.label + "\" not found.");
            }
        }

        //get the root
        Node root = roots.get(0);
        boolean isMaxPlayer = rootPlayer != Player.MIN;
        Minmax minimax = new Minmax(root, range);
        String result = minimax.minimaxSearch(root, isMaxPlayer, verbose, alphaBetaPruning);
        System.out.println(result);
    }
}
