# RDF Turtle Parser

## NOTE
This program is functional; however, the current implementation is highly inefficient. I have come to realize that a single-pass tokenized approach would yield better performance. This insight was gained from my experience in implementing a compiler using this method during the _CS244: Computer Architecture_ module at Stellenbosch University.

## Overview
This Java program converts standard RDF Turtle data into a simplified triple format. The output data can be in CSV or Turtle format, and the program is easily adaptable to support other formats.

Blank nodes and collections are assigned human-readable IDs, facilitating easy analysis after data processing. The processed data can be queried using a SPARQL endpoint, testable by uploading the data to platforms such as [Triply](https://triplydb.com/). Additionally, the data can be uploaded to an SQL database due to its rectangular shape.

For a better understanding of RDF data, refer to the following resources: [Semantic Web 2016 Course](https://open.hpi.de/courses/semanticweb2016/), [Stardog Training](https://www.stardog.com/trainings/).

## Implementation
The program utilizes regular expressions to process Turtle text. String segments of interest (URLs, Literals, Blank Nodes, Collections) are identified, captured, and assigned IDs. These IDs are later replaced with their content after processing.

- Half-processed data

![Semi-processed data](https://user-images.githubusercontent.com/79271609/215045378-a9a7458a-0db1-4906-8e3c-56d891531f55.png)

- Output data

![Output](https://user-images.githubusercontent.com/79271609/215045417-31c1aa61-fea1-4447-9a51-3509c7dce5af.png)

## Usage
**Requirements:**
- Java installation

Clone (download) this repository to your local machine and open a terminal in that directory. Run the command:
```bash
javac -d bin/ src/*.java
```

After that, test the program with some Turtle data provided in the repository by giving the file name as a command line argument. For example:
```bash
java -cp bin/ Main time.ttl > out/test.ttl
```

You can incorporate the `TurtleParser` into your Java code:
```java
TurtleParser your_parser_name = new TurtleParser("the_name_of_your_turtle_file.ttl");
```

The `printDataTurtle()` and `printDataCSV()` methods print the data in their corresponding formats. This data can then be piped into a file.
```java
your_parser_name.printDataTurtle();
```
or
```java
your_parser_name.printDataCSV();
```

The data is now ready for analysis.

## Tools for Data Analysis
- [Triply](https://triplydb.com/):

![Triply](https://user-images.githubusercontent.com/79271609/215045695-4dc92331-fd3b-48a3-b5dd-e4fa3538e9f2.png)

- [RStudio](https://posit.co/download/rstudio-desktop/):

![RStudio](https://user-images.githubusercontent.com/79271609/215045722-dddc4ae8-2294-47da-9b42-d9514823dd8a.png)
