public class Main {
    public static void main(String[] args) {
        TurtleParser parser = new TurtleParser(args[0]);
        // parser.printDataCSV();
        // System.out.println("\n\n\n\n");
        parser.printDataTurtle();
    }
}
