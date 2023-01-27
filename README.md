## RDF-Turtle-Parser
    * Overview
    * Implementation
    * Usage

### Overview
A Java program for converting standard RDF Turtle data into a simplified triple format.
The output data can either be in CSV or Turtle format but the program can be easily modified to allow other formats.

Blank nodes and collections are given human-readable IDs to allow for hassle-free analysis after the data is processed.
The processed data can be queried using a SPARQL endpoint which can be tested by uploading the data to a platform like Triply. <https://triplydb.com/>
It is also possible to upload the data to a SQL database because of its rectangular shape.

The following resources can be useful for understanding RDF data: <https://open.hpi.de/courses/semanticweb2016/>, <https://www.stardog.com/trainings/>

### Implementation
The program makes use of a lot of regular expressions (of which my knowledge is extremely limited) to process the turtle text.
String segments of interest (URLs, Literals, Blank Nodes, Collections) are identified, captured, and given IDs to allow for the processing of the turtle data without any interference. The IDs are replaced with their content after the data is processed.

### Usage
Requirements:
    * a Java installation

Clone (download) this repository to your local machine and open a terminal in that directory.
Run the command 'javac *.java'

After that you can test the program with some of the turtle data supplied in the repository by supplying the file name as a command line argument.

For example 'java Main time.ttl > test.ttl' will save the expanded turtle data of 'time.ttl' to the file 'test.ttl'.

You can impelent TurtleParser into your Java code in the following way:
'TurtleParser your_parser_name = new TurtleParser("the_name_of_your_turtle_file.ttl");'

The methods printDataTurtle() and printDataCSV() print the data in their corresponding formats. This data can then be piped into a file.
'your_parser_name.printDataTurtle();'
                or
'your_parser_name.printDataCSV();'

The data is now ready to be analised.

<https://posit.co/download/rstudio-desktop/>

<https://triplydb.com/>