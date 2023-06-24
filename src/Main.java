public class Main {
  public static void main(String[] args) {
    TurtleParser parser = new TurtleParser(args[0]);
    parser.printDataTurtle();
    // parser.printDataCSV("\t");
  }
}
