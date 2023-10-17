## NOTE
This program is in working condition but the implementation is extremely inefficient. I now realise that a single pass tokenised approach would perform better. I learned this from _CS244: Computer Architecture_, a module that I took at Stellenbosch University where we had to implement a compiler using this approach. 

## RDF Turtle Parser
    - Overview
    - Implementation
    - Usage

### Overview
A Java program for converting standard RDF Turtle data into a simplified triple format.
The output data can either be in CSV or Turtle format but the program can be easily modified to allow other formats.

Blank nodes and collections are given human-readable IDs to allow for easy analysis after the data is processed.
The processed data can be queried using a SPARQL endpoint which can be tested by uploading the data to a platform like Triply (<https://triplydb.com/>).
It is also possible to upload the data to a SQL database because of its rectangular shape.

The following resources can be useful for understanding RDF data: <https://open.hpi.de/courses/semanticweb2016/>, <https://www.stardog.com/trainings/>

### Implementation
The program makes use of regular expressions (of which my knowledge is limited) to process the turtle text.
String segments of interest (URLs, Literals, Blank Nodes, Collections) are identified, captured, and given IDs to allow for the processing of the turtle data without any interference. The IDs are replaced with their content after the data is processed.

- Half-processed data

![semi_processed_data](https://user-images.githubusercontent.com/79271609/215045378-a9a7458a-0db1-4906-8e3c-56d891531f55.png)

- Output data

![output](https://user-images.githubusercontent.com/79271609/215045417-31c1aa61-fea1-4447-9a51-3509c7dce5af.png)

### Usage
Requirements:

    - a Java installation

Clone (download) this repository to your local machine and open a terminal in that directory.
Run the command:
```javac -d bin/ src/*.java```

After that you can test the program with some of the turtle data supplied in the repository by giving the file name as a command line argument.

For example ```java -cp bin/ Main time.ttl > out/test.ttl``` will save the expanded turtle data of 'time.ttl' to the file 'test.ttl'.

You can impelent TurtleParser into your Java code in the following way:

```TurtleParser your_parser_name = new TurtleParser("the_name_of_your_turtle_file.ttl");```

The methods printDataTurtle() and printDataCSV() print the data in their corresponding formats. This data can then be piped into a file.
```your_parser_name.printDataTurtle();``` or ```your_parser_name.printDataCSV();```

The data is now ready for analysis.

Triply: <https://triplydb.com/>

![triply](https://user-images.githubusercontent.com/79271609/215045695-4dc92331-fd3b-48a3-b5dd-e4fa3538e9f2.png)

RStudio: <https://posit.co/download/rstudio-desktop/>

![rstudio](https://user-images.githubusercontent.com/79271609/215045722-dddc4ae8-2294-47da-9b42-d9514823dd8a.png)
