import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Java program for converting standard RDF Turtle data into a simplified triple format.
 * The output data can either be in CSV or Turtle format but the program can be easily modified to allow other formats.
 * Blank nodes and collections are given human-readable IDs to allow for hassle-free analysis after the data is processed.
 * The processed data can be queried using a SPARQL endpoint which can be tested by uploading the data to a platform like Triply. {@link https://triplydb.com/}
 * It is also possible to upload the data to a SQL database because of its rectangular shape.
 * The following resources can be useful for understanding RDF data: {@link https://open.hpi.de/courses/semanticweb2016/}, {@link https://www.stardog.com/trainings/}
 * {@author Pieter-Ruan Cronje}
 */

public class TurtleParser {

	private ArrayList<String[]> TRIPLE_STORE = new ArrayList<String[]>();

	private ArrayList<String> URLs = new ArrayList<String>();
	private ArrayList<String> LITERALS1 = new ArrayList<String>(); // For strings enclosed within '...'
	private ArrayList<String> LITERALS2 = new ArrayList<String>(); // For strings enclosed within "..."
	private ArrayList<String> LITERALS3 = new ArrayList<String>(); // For strings enclosed within """..."""
	private ArrayList<String> BLANK_NODES = new ArrayList<String>();
	private ArrayList<String> COLLECTIONS = new ArrayList<String>();

	private ArrayList<String> prefixes = new ArrayList<String>();
	private ArrayList<String> prefixURLs = new ArrayList<String>();

	private String PREFIX = ":";
	private String PREFIX_URL = "www.example.org";

	private int COLLECTION_ID = 0;
	private int BLANK_ID = 0;

	/**
	 * Creates an instance of the turtle parser.
	 * @param fileName name of the file containing the turtle data.
	 */
	public TurtleParser(String fileName) {
		String[] data = readData(fileName);
		String[][] triples = splitTriples(data);
		expandTriples(triples);
		processBlankNodesAndCollections();
		insertLiterals();
		insertURLs();
		replaceBlankAndCollectionIDs();
		removeNewLines();
		collectPrefixes();
		replacePrefixes();
		removePrefixStatements();
	}

	/**
	 * Outputs the data in CSV format with a header and tabs as the delimeter.
	 */
	public void printDataCSV() {
		System.out.println("Subject\tPredicate\tObject");
		int count;
		for (String[] triple : TRIPLE_STORE) {
			count = 0;
			for (String component : triple) {
				System.out.print(component );
				if (count < 2) System.out.print("\t");
				count++;
			}
			System.out.println();
		}
	}

	/**
	 * Outputs the data in RDF Turtle format.
	 * Blank node and collection IDs are given the base URL as their prefix which can be found after '@prefix :' or '@base' in the turtle data.
	 * In the event that there is no base the URL will be "http://www.example-domain.org#" which can be changed within this function.  
	 */
	public void printDataTurtle() {
		boolean hasBase = false;
		for (String prefix : prefixes)
			if (prefix.equals("base:")) hasBase = true;
		for (String[] triple : TRIPLE_STORE) {
			if (triple[0].startsWith("blank_node_") || triple[0].startsWith("collection_") || !triple[0].startsWith("http")) {
				 if (hasBase) System.out.print("<" + prefixURLs.get(prefixes.indexOf("base:")) + triple[0] + "> ");
				 else System.out.print("<http://www.example-domain.org#" + triple[0] + "> ");
			} else 
				System.out.print("<" + triple[0] + "> ");
			if (triple[1].startsWith("element_")) {
				if (hasBase) System.out.print("<" + prefixURLs.get(prefixes.indexOf("base:")) + triple[1] + "> ");
				 else System.out.print("<http://www.example-domain.org#" + triple[1] + "> ");
			} else
				System.out.print("<" + triple[1] + "> ");
			if (triple[2].startsWith("blank_node_") || triple[2].startsWith("collection_")) {
				if (hasBase) System.out.println("<" + prefixURLs.get(prefixes.indexOf("base:")) + triple[2] + "> .");
				else System.out.println("<http://www.example-domain.org#" + triple[2] + "> .");
			} else if (triple[2].startsWith("\"") || !triple[2].startsWith("http")) {
				if (triple[2].contains("\"^^")) {
					int literalEnd = triple[2].indexOf("\"^^");
					System.out.println(triple[2].substring(0, literalEnd+3) + "<" + triple[2].substring(literalEnd+3,triple[2].length()) + "> .");
				} else if (triple[2].startsWith("\""))
					System.out.println(triple[2] + " .");
				else
					System.out.println("\"" + triple[2] + "\"" + " .");
			} else
				System.out.println("<" + triple[2] + "> .");
		}
	}

	/**
	 * Returns the triple data.
	 * @return (ArrayList<String[]>) the triple data as an ArrayList containing string arrays of length 3.
	 */
	public ArrayList<String[]> getData() {
		return TRIPLE_STORE;
	}

	/**
	 * Reads the turtle file and splits the data into unprocessed triples.
	 * @param fileName name of the file containing the turtle data.
	 * @return (String[]) an array of the unprocessed triples.
	 */
	private String[] readData(String fileName) {

		String data_string = "";
		File file = new File(fileName);

		try {
			Scanner reader = new Scanner(file);
			while (reader.hasNextLine()) data_string += (reader.nextLine() + " ~!NEWLINE!~ ");
			reader.close();
		} catch (FileNotFoundException err) {
			System.out.println("File '" + fileName + "' does not exist.");
		}

		data_string = data_string.replaceAll("\"{4," + data_string.length() + "}", "");
		data_string = data_string.replaceAll("\\\\\"", "");
		data_string = data_string.replaceAll("\\s+", " ");
		data_string = data_string.replaceAll("@base", "@prefix :");

		Pattern pattern = Pattern.compile("<(.*?)>");
		Matcher matcher = pattern.matcher(data_string);
		while(matcher.find()) {
			URLs.add(matcher.group(1));
		}

		int url_id = 0;
		data_string = data_string.replaceAll("<(.*?)>", " ~!URL!~ ");
		while (data_string.contains("~!URL!~"))
			data_string = data_string.replaceFirst("~!URL!~", "~!URL<" + (url_id++) + ">!~");

		pattern = Pattern.compile("\"\"\"(.*?)\"\"\"");
		matcher = pattern.matcher(data_string);
		while(matcher.find()) {
			LITERALS3.add(matcher.group(1));
		}
		data_string = data_string.replaceAll("\"\"\"(.*?)\"\"\"", "~!LITERAL3!~");
		
		pattern = Pattern.compile("\"(.*?)\"");
		matcher = pattern.matcher(data_string);
		while(matcher.find()) {
			LITERALS2.add(matcher.group(1));
		}
		data_string = data_string.replaceAll("\"(.*?)\"", "~!LITERAL2!~");
		
		pattern = Pattern.compile("'(.*?)'");
		matcher = pattern.matcher(data_string);
		while(matcher.find()) {
			LITERALS1.add(matcher.group(1));
		}
		data_string = data_string.replaceAll("'(.*?)'", "~!LITERAL1!~");

		int literal_id_3 = 0;
		while (data_string.contains("~!LITERAL3!~"))
			data_string = data_string.replaceFirst("~!LITERAL3!~", "~!LITERAL3<" + (literal_id_3++) + ">!~");
		
		int literal_id_2 = 0;
		while (data_string.contains("~!LITERAL2!~"))
			data_string = data_string.replaceFirst("~!LITERAL2!~", "~!LITERAL2<" + (literal_id_2++) + ">!~");
		
		int literal_id_1 = 0;
		while (data_string.contains("~!LITERAL1!~"))
			data_string = data_string.replaceFirst("~!LITERAL1!~", "~!LITERAL1<" + (literal_id_1++) + ">!~");

		while (data_string.contains("#")) {
			data_string = data_string.replaceFirst("#(.*?)~!NEWLINE!~", "");
		}

		data_string = data_string.replaceAll("\\s+", " ");
		data_string = data_string.replaceAll("~!NEWLINE!~", " ");
		data_string = data_string.replaceAll("\\s+", " ");

		data_string = harvestCollections(data_string);
		data_string = data_string.replaceAll("\\s+", " ");

		data_string = harvestBlankNodes(data_string);
		data_string = data_string.replaceAll("\\s+", " ");

		String[] data = data_string.split("\\s[.]\\s", data_string.length()/3);

		for (int i = 0; i < data.length; i++)
			if (data[i].startsWith(" "))
				data[i] = data[i].substring(1,data[i].length());

		return data;
	}

	/**
	 * Separates the the unprocessed triples into their 3 fundamental components.
	 * @param data (String[]) unprocessed triples
	 * @return (String[][]) still uprocessed triples but now in three colums (subject, predicate, object)
	 */
	private String[][] splitTriples(String[] data) {
		String[][] triples = new String[data.length-1][3];
		String[] newTriple = new String[3];
		for (int i = 0; i < data.length-1; i++) {
			newTriple = data[i].split("\\s+", 3);
			triples[i][0] = newTriple[0];
			triples[i][1] = newTriple[1];
			triples[i][2] = newTriple[2];
		}
		eliminateWhiteSpace(triples);
		for (String[] triple : triples)
			if (triple[1].equals("a")) triple[1] = "rdf:type";
		return triples;
	}

	/**
	 * Converts the existing data into simpler triples by looking at the usage of commas and semicolons according to the Turtle syntax.
	 * @param triples (String[][]) unprocessed triples separated into their 3 fundamental components.
	 */
	private void expandTriples(String[][] triples) {
		for (String[] triple : triples) {

			// super refers to the main unprocessed component of the triple.
			String super_subject = triple[0];
			String super_predicate = triple[1];
			String super_object = triple[2];

			// turtle has a variety of ways to express the same data, wherever I replace a symbol with another symbol it is inorder the simplify the process.
			if (super_object.contains(";.")) super_object = super_object.replace(";.", " . ");
			super_object = super_object.replaceAll(";\\s+\\.", " . ");

			if (super_object.contains(";")) {
				
				String[] predicates_and_objects = super_object.split(";");
								
				if (predicates_and_objects[0].contains(",")) {
					String[] objects = predicates_and_objects[0].split(",");
					for (int x = 0; x < objects.length; x++)
						TRIPLE_STORE.add(new String[]{super_subject, super_predicate, objects[x]});
				} else
					TRIPLE_STORE.add(new String[]{super_subject, super_predicate, predicates_and_objects[0]});
				
				for (int j = 1; j < predicates_and_objects.length; j++) {
					String[] predicate_and_object = predicates_and_objects[j].split("\\s");
					String predicate = predicate_and_object[0];
					String object = predicate_and_object[1];

					if (object.contains(",")) {
						String[] objects = object.split(",");
						for (int x = 0; x < objects.length; x++)
							TRIPLE_STORE.add(new String[]{super_subject, predicate, objects[x]});
					} else
						TRIPLE_STORE.add(new String[]{super_subject, predicate, object});
				}

			} else if (super_object.contains(",")) {
				String[] objects = super_object.split(",");
				for (int x = 0; x < objects.length; x++)
					TRIPLE_STORE.add(new String[]{super_subject, super_predicate, objects[x]});
			} else
				TRIPLE_STORE.add(new String[]{super_subject, super_predicate, super_object});
		}

		for (String[] triple : TRIPLE_STORE)
			if (triple[0].equals("")) TRIPLE_STORE.remove(triple);
	}

	/**
	 * Iterates through the data and removes the whitespace.
	 * @param triples (String[][]) unprocessed triples separated into subject, predicate, and object
	 */
	private void eliminateWhiteSpace(String[][] triples) {
		// regular expressions to capture whitespace (this was my first time using regex so I'm sure it can be simplified)
		String[] patterns = {
			"\\s+[\\[]\\s+",
			"\\s+[\\]]\\s+",
			"\\s+[\\[]",
			"\\s+[\\]]+",
			"[\\[]\\s+",
			"[\\]]\\s+",
			"\\s+[;]\\s+",
			"\\s+[,]\\s+",
			"\\s+[;]",
			"\\s+[,]",
			"[;]\\s+",
			"[,]\\s+"
		};
		String[] replacements = {
			"[",
			"]",
			"[",
			"]",
			"[",
			"]",
			";",
			",",
			";",
			",",
			";",
			","
		};
		for (String[] triple : triples)
			for (int i = 0; i < patterns.length; i++)
				triple[2] = triple[2].replaceAll(patterns[i], replacements[i]);
	}  

	/**
	 * Checks if a string contains the prefix identifier.
	 * @param string (String) string to check for prefix identifier.
	 * @return true if the string contains the prefix identifier, false otherwise
	 */
	private boolean containsPrefix(String string) {
		for (String prefix : prefixes) {
			if (string.contains(prefix)){
				PREFIX = prefix;
				PREFIX_URL = prefixURLs.get(prefixes.indexOf(prefix));
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces the literal IDs in the data with their corresponding literals.
	 */
	private void insertLiterals() {
		for (String[] triple : TRIPLE_STORE) {
			Pattern pattern1 = Pattern.compile("~!LITERAL1<(.*?)>!~");
			Matcher matcher1 = pattern1.matcher(triple[2]);
			if (matcher1.find()) {
				int index = Integer.parseInt(matcher1.group(1));
				triple[2] = triple[2].replace("~!LITERAL1<" + index +">!~", "\"" + LITERALS1.get(index) + "\"");
			}
			Pattern pattern2 = Pattern.compile("~!LITERAL2<(.*?)>!~");
			Matcher matcher2 = pattern2.matcher(triple[2]);
			if (matcher2.find()) {
				int index = Integer.parseInt(matcher2.group(1));
				triple[2] = triple[2].replace("~!LITERAL2<" + index +">!~", "\"" + LITERALS2.get(index) + "\"");
			}
			Pattern pattern3 = Pattern.compile("~!LITERAL3<(.*?)>!~");
			Matcher matcher3 = pattern3.matcher(triple[2]);
			if (matcher3.find()) {
				int index = Integer.parseInt(matcher3.group(1));
				triple[2] = triple[2].replace("~!LITERAL3<" + index +">!~", "\"" + LITERALS3.get(index) + "\"");
			}
		}
	}

	/**
	 * Replaces the URL IDs in the data with their corresponding URLs.
	 */
	private void insertURLs() {
		for (String[] triple : TRIPLE_STORE) {
			Pattern pattern = Pattern.compile("~!URL<(.*?)>!~");
			Matcher matcher = pattern.matcher(triple[0]);
			if (matcher.find()) {
				int index = Integer.parseInt(matcher.group(1));
				triple[0] = URLs.get(index);
			}
			matcher = pattern.matcher(triple[1]);
			if (matcher.find()) {
				int index = Integer.parseInt(matcher.group(1));
				triple[1] = URLs.get(index);
			}
			matcher = pattern.matcher(triple[2]);
			if (matcher.find()) {
				int index = Integer.parseInt(matcher.group(1));
				triple[2] = URLs.get(index);
			}
		}
	}

	/**
	 * Removes the new lines in the data.
	 */
	private void removeNewLines() {
		for (String[] triple : TRIPLE_STORE) {
			if (triple[2].contains("~!NEWLINE!~")) {
				triple[2] = triple[2].replaceAll("~!NEWLINE!~+", "");
				triple[2] = triple[2].replaceAll("\\s+", " ");
			}
		}
	}

	/**
	 * Collects all the prefixes and stores them.
	 */
	private void collectPrefixes() {
		prefixes.add("rdf:");
		prefixURLs.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		for (String[] triple : TRIPLE_STORE) {
			if (triple[0].equals("@prefix")) {
				if (triple[1].equals(":")) prefixes.add("base:");
				else prefixes.add(triple[1]);
				prefixURLs.add(triple[2]);
			}
		}
	}

	/**
	 * Replaces all the prefixes with their corresponding URLs.
	 */
	private void replacePrefixes() {
		for (String[] triple : TRIPLE_STORE) {
			if (triple[0].startsWith(":"))
				triple[0] = triple[0].replace(":", prefixURLs.get(prefixes.indexOf("base:")));
			else if (containsPrefix(triple[0])) triple[0] = triple[0].replace(PREFIX, PREFIX_URL);
			
			if (triple[1].startsWith(":"))
				triple[1] = triple[1].replace(":", prefixURLs.get(prefixes.indexOf("base:")));
			else if (containsPrefix(triple[1])) triple[1] = triple[1].replace(PREFIX, PREFIX_URL);
			
			if (triple[2].startsWith(":"))
				triple[2] = triple[2].replace(":", prefixURLs.get(prefixes.indexOf("base:")));
			else if (containsPrefix(triple[2])) triple[2] = triple[2].replace(PREFIX, PREFIX_URL);
		}
	}

	/**
	 * Removes all the triples specifying a prefix for a URL.
	 */
	private void removePrefixStatements() {
		Iterator<String[]> iterator = TRIPLE_STORE.iterator();
		while (iterator.hasNext()) {
			String[] triple = iterator.next();
			if (triple[0].equals("@prefix")) iterator.remove();
		}
	}

	/**
	 * Collects all the content inside the blank nodes and replaces it with an ID.
	 * @param data (String) the turtle data.
	 * @return (String) the turtle data with the blank node content removed.
	 */
	private String harvestBlankNodes(String data) {
		Stack<Integer> openBrackets = new Stack<Integer>();
		int openBracketIndex = -1, closedBracketIndex = -1;
		String subString = "";
		for (int i = 0; i < data.length(); i++) {
			if (data.charAt(i) == '[') openBrackets.push(i);
			if (data.charAt(i) == ']') {
				openBracketIndex = openBrackets.pop();
				closedBracketIndex = i;
				subString = data.substring(openBracketIndex+1, closedBracketIndex);
				BLANK_NODES.add(subString);
				data = data.replace("[" + subString + "]", " ~!BLANK<" + (BLANK_ID++) + ">!~ " + whiteSpaceForBlankNode(subString, BLANK_ID));
			}
		}
		return data;
	}

	/**
	 * Generates the amount of whitespace needed to replace a blank node.
	 * @param subString (String) the content of the blank node.
	 * @param blank_id (int) the ID of the blank node.
	 * @return (String) whitespace.
	 */	
	public String whiteSpaceForBlankNode(String subString, int blank_id) {
		int spaceRemoved = subString.length() + 2;
		int spaceFilled = 13;
		String id = "" + blank_id;
		spaceFilled += id.length();
		int spaceNeeded = spaceRemoved - spaceFilled;
		String whiteSpace = "";
		for (int i = 0; i < spaceNeeded; i++) {
			whiteSpace += " ";
		}
		return whiteSpace;
	}

	/**
	 * Collects all the content inside the collections and replaces it with an ID.
	 * @param data (String) the turtle data.
	 * @return (String) the turtle data with the collection content removed.
	 */
	private String harvestCollections(String data) {
		Stack<Integer> openBrackets = new Stack<Integer>();
		int openBracketIndex = -1, closedBracketIndex = -1;
		String subString = "";
		for (int i = 0; i < data.length(); i++) {
			if (data.charAt(i) == '(') openBrackets.push(i);
			if (data.charAt(i) == ')') {
				openBracketIndex = openBrackets.pop();
				closedBracketIndex = i;
				subString = data.substring(openBracketIndex+1, closedBracketIndex);
				COLLECTIONS.add(subString);
				data = data.replace("(" + subString + ")", " ~!COLLECTION<" + (COLLECTION_ID++) + ">!~ " + whiteSpaceForCollection(subString, COLLECTION_ID));
			}
		}
		return data;
	}

	/**
	 * Generates the amount of whitespace needed to replace a collection.
	 * @param subString (String) the content of the collection.
	 * @param blank_id (int) the ID of the collection.
	 * @return (String) whitespace.
	 */	
	public String whiteSpaceForCollection(String subString, int collection_id) {
		int spaceRemoved = subString.length() + 2;
		int spaceFilled = 18;
		String id = "" + collection_id;
		spaceFilled += id.length();
		int spaceNeeded = spaceRemoved - spaceFilled;
		String whiteSpace = "";
		for (int i = 0; i < spaceNeeded; i++) {
			whiteSpace += " ";
		}
		return whiteSpace;
	}

	/**
	 * Replaces the existing blank node and collection IDs with a more readable version.
	 * The previous IDs were purposefully strange inorder to prevent any conflicts with the other data.
	 */
	private void replaceBlankAndCollectionIDs() {
		Pattern pattern = Pattern.compile("<(.*?)>");
		Matcher matcher;
		int id = 0;
		for (String[] triple : TRIPLE_STORE) {
			if (triple[0].contains("~!BLANK")) {
				matcher = pattern.matcher(triple[2]);
				if (matcher.find()) id = Integer.parseInt(matcher.group(1));
				triple[0] = "blank_node_(id=" + id + ")";
			} else if (triple[0].contains("~!COLLECTION")) {
				matcher = pattern.matcher(triple[2]);
				if (matcher.find()) id = Integer.parseInt(matcher.group(1));
				triple[0] = "collection_(id=" + id + ")";
			}
			if (triple[2].contains("~!BLANK")) {
				matcher = pattern.matcher(triple[2]);
				if (matcher.find()) id = Integer.parseInt(matcher.group(1));
				triple[2] = "blank_node_(id=" + id + ")";
			} else if (triple[2].contains("~!COLLECTION")) {
				matcher = pattern.matcher(triple[2]);
				if (matcher.find()) id = Integer.parseInt(matcher.group(1));
				triple[2] = "collection_(id=" + id + ")";
			}
		}
	}

	/**
	 * Processes the content of the blank nodes and collections to add the triples to the triple storage.
	 */
	private void processBlankNodesAndCollections() {
		for (int i = 0; i < COLLECTIONS.size(); i++) {
			String collection = COLLECTIONS.get(i);
			collection = harvestBlankNodes(collection);
			processCollection(collection, i);
		}
		for (int i = 0; i < BLANK_NODES.size(); i++) {
			processBlankNode(BLANK_NODES.get(i), i);
		}
	}

	/**
	 * Processes a blank node to recover the triples inside of it.
	 * @param blank (String) content of the blank node.
	 * @param blank_id (int) ID of the blank node.
	 */
	private void processBlankNode(String blank, int blank_id) {
		blank = blank.replaceAll("\\s+", " ");
		if (blank.endsWith(" ")) blank = blank.substring(0, blank.length()-1);
		if (blank.endsWith(";") || blank.endsWith(".")) blank = blank.substring(0, blank.length()-1);
		if (blank.endsWith(" ")) blank = blank.substring(0, blank.length()-1);
		
		String[] entries = blank.split(";");
		for (String entry : entries) {
			if (entry.startsWith(" ")) entry = entry.substring(1, entry.length());
			if (entry.endsWith(" ")) entry = entry.substring(0, entry.length() - 1);
			String[] predicate_and_object = entry.split("\\s");
			if (predicate_and_object.length == 2) TRIPLE_STORE.add(new String[]{"blank_node_(id=" + blank_id + ")", predicate_and_object[0], predicate_and_object[1]});
		}
	}

	/**
	 * Processes a collection to recover the triples inside of it.
	 * @param blank (String) content of the collection.
	 * @param blank_id (int) ID of the collection.
	 */
	private void processCollection(String collection, int collection_id) {
		collection = collection.replaceAll("\\s+", " ");
		if (collection.startsWith(" ")) collection = collection.substring(1, collection.length());
		String[] items = collection.split(" ");
		for (int i = 0; i < items.length; i++)
			TRIPLE_STORE.add(new String[]{"collection_(id=" + collection_id + ")", "element_(#" + (i+1) + ")", items[i]});
	}
}
