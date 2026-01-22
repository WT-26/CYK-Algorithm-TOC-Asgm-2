import java.io.*;
import java.util.*;

public class CYK {

    /** Input word to test */
    public static String word;

    /** Start symbol (e.g., S) */
    public static String startingSymbol;

    /** Token-word mode: enabled when args.length > 2 */
    public static boolean isTokenWord = false;

    /** Terminals set Σ */
    public static ArrayList<String> terminals = new ArrayList<>();

    /** Non-terminals set V */
    public static ArrayList<String> nonTerminals = new ArrayList<>();

    /**
     * Grammar productions:
     * LHS -> list of RHS strings
     */
    public static TreeMap<String, ArrayList<String>> grammar = new TreeMap<>();

    public static void main(String[] args) {

        // Required usage
        if (args.length < 2) {
            System.out.println("Usage: java CYK <GrammarFile> <Word>");
            System.exit(1);
        }

        // Token mode if more than 2 arguments
        if (args.length > 2) {
            isTokenWord = true;
        }

        doSteps(args);
    }

    /** Main flow */
    public static void doSteps(String[] args) {
        parseGrammar(args);

        // EPS special case
        if (wordLength() == 0) {
            printEpsilonResult();
            return;
        }

        String[][] cykTable = createCYKTable();
        doCyk(cykTable);
        printResult(cykTable);
    }

    /**
     * Parses grammar file:
     * Line 1: start symbol
     * Line 2: terminals
     * Line 3: non-terminals
     * Line 4+: productions
     */
    public static void parseGrammar(String[] args) {
        Scanner input = openFile(args[0]);
        ArrayList<String> tmp = new ArrayList<>();
        int line = 2;

        // Read word
        word = getWord(args);

        // Starting symbol
        startingSymbol = input.next().trim();
        input.nextLine();

        // Read terminals and non-terminals
        while (input.hasNextLine() && line <= 3) {
            String raw = input.nextLine();
            tmp.addAll(Arrays.asList(toArray(raw)));

            if (line == 2) terminals.addAll(tmp);
            if (line == 3) nonTerminals.addAll(tmp);

            tmp.clear();
            line++;
        }

        // Read productions
        while (input.hasNextLine()) {
            String raw = input.nextLine().trim();
            if (raw.isEmpty()) continue;

            tmp.addAll(Arrays.asList(toArray(raw)));
            if (tmp.isEmpty()) continue;

            String leftSide = tmp.get(0);
            tmp.remove(0);

            //  DO NOT overwrite old productions
            grammar.putIfAbsent(leftSide, new ArrayList<>());
            grammar.get(leftSide).addAll(tmp);

            tmp.clear();
        }

        input.close();
    }

    /**
     * Reads input word
     * - Non-token mode: args[1]
     * - Token mode: join args[1..]
     * - "eps" => epsilon
     */
    public static String getWord(String[] args) {
        if (!isTokenWord) {
            if (args[1].equalsIgnoreCase("eps")) return "";
            return args[1];
        }
        return String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
    }

    /** Word length depending on token mode */
    public static int wordLength() {
        return isTokenWord ? toArray(word).length : word.length();
    }

    /** EPS (empty string) result */
    public static void printEpsilonResult() {
        boolean accepted = grammar.containsKey(startingSymbol)
                && grammar.get(startingSymbol).contains("*");

        System.out.println("====================================");
        System.out.println("Start Symbol: " + startingSymbol);
        System.out.println("Word        : EPS (empty string)");
        System.out.println("Result      : " + (accepted ? "ACCEPTED " : "REJECTED "));
        System.out.println("====================================");
    }

    /** Pretty set printer */
    private static String asSet(List<String> list) {
        return "{" + String.join(", ", list) + "}";
    }

    /** Print grammar and CYK result */
    public static void printResult(String[][] cykTable) {
        System.out.println("Word: " + word);

        System.out.println("\nG = (" + asSet(terminals)
                + ", " + asSet(nonTerminals)
                + ", P, " + startingSymbol + ")\n\nProductions P:");

        for (String s : grammar.keySet()) {
            System.out.println(s + " -> " + String.join(" | ", grammar.get(s)));
        }

        System.out.println("\nApplying CYK-Algorithm:\n");
        drawTable(cykTable);
    }

    /** Draw CYK table + final accept/reject */
    public static void drawTable(String[][] cykTable) {
        int l = findLongestString(cykTable) + 2;
        String formatString = "| %-" + l + "s ";

        // Border building
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int x = 0; x <= l + 2; x++) {
            sb.append(x == l + 2 ? "+" : "-");
        }
        String low = sb.toString();
        sb.delete(0, 1);
        String lowRight = sb.toString();

        // Print table rows
        for (int i = 0; i < cykTable.length; i++) {
            for (int j = 0; j <= cykTable[i].length; j++) {
                System.out.print((j == 0) ? low : lowRight);
            }
            System.out.println();

            for (int j = 0; j < cykTable[i].length; j++) {
                String cell = cykTable[i][j].isEmpty() ? "-" : cykTable[i][j];
                System.out.format(formatString, cell.replaceAll("\\s+", ","));
                if (j == cykTable[i].length - 1) System.out.print("|");
            }
            System.out.println();
        }
        System.out.println(low + "\n");

        // correct acceptance check (top cell = [n][0])
        boolean accepted = false;
        String topCell = cykTable[cykTable.length - 1][0].trim();

        if (!topCell.isEmpty()) {
            List<String> vars = Arrays.asList(topCell.split("\\s+"));
            accepted = vars.contains(startingSymbol);
        }

        System.out.println("====================================");
        System.out.println("Start Symbol: " + startingSymbol);
        System.out.println("Word        : " + word);
        System.out.println("Result      : " + (accepted ? "ACCEPTED " : "REJECTED "));
        System.out.println("====================================");
    }

    /** Find longest string for formatting */
    public static int findLongestString(String[][] cykTable) {
        int x = 0;
        for (String[] row : cykTable) {
            for (String v : row) {
                if (v.length() > x) x = v.length();
            }
        }
        return x;
    }

    /** Create triangular CYK table */
    public static String[][] createCYKTable() {
        int n = wordLength();

        String[][] cykTable = new String[n + 1][];
        cykTable[0] = new String[n];

        for (int i = 1; i < cykTable.length; i++) {
            cykTable[i] = new String[n - (i - 1)];
            Arrays.fill(cykTable[i], "");
        }
        return cykTable;
    }

    /** Apply CYK algorithm */
    public static void doCyk(String[][] table) {

        // Step 1: header row
        for (int i = 0; i < table[0].length; i++) {
            table[0][i] = manageWord(word, i);
        }

        // Step 2: fill row 1
        for (int i = 0; i < table[1].length; i++) {
            table[1][i] = String.join(" ", checkIfProduces(new String[]{table[0][i]}));
        }

        // Step 3+: fill for length >= 2
        for (int len = 2; len < table.length; len++) {
            for (int start = 0; start < table[len].length; start++) {

                Set<String> resultSet = new HashSet<>();

                for (int split = 1; split < len; split++) {
                    String[] left = toArray(table[split][start]);
                    String[] right = toArray(table[len - split][start + split]);

                    for (String a : left) {
                        for (String b : right) {
                            resultSet.addAll(Arrays.asList(checkIfProduces(new String[]{a + b})));
                        }
                    }
                }

                table[len][start] = String.join(" ", resultSet);
            }
        }
    }

    /** Get symbol/token at position */
    public static String manageWord(String word, int pos) {
        if (!isTokenWord) return Character.toString(word.charAt(pos));
        return toArray(word)[pos];
    }

    /** Find all LHS that produce given RHS */
    public static String[] checkIfProduces(String[] rhsList) {
        ArrayList<String> result = new ArrayList<>();

        for (String lhs : grammar.keySet()) {
            for (String rhs : rhsList) {
                if (rhs == null || rhs.isEmpty()) continue;
                if (grammar.get(lhs).contains(rhs)) {
                    result.add(lhs);
                }
            }
        }

        return result.toArray(new String[0]);
    }

    /** Split by whitespace (robust) */
    public static String[] toArray(String input) {
        if (input == null) return new String[]{};
        input = input.trim();
        if (input.isEmpty()) return new String[]{};
        return input.split("\\s+");
    }

    /** Open grammar file safely */
    public static Scanner openFile(String file) {
        try {
            return new Scanner(new File(file));
        } catch (FileNotFoundException e) {
            System.out.println("Error: Can't find or open the file: " + file);
            System.exit(1);
            return null;
        }
    }
}
