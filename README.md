WOA 2022
====================

The repository was built as additional material of the extended version of paper entitled "Can agents talk about what they are doing? A proposal with Jason and speech acts" by Valeria Seidita, Francesco Lanza, Antonio Chella and Angelo Maria Pio Sabella" submitted to the WOA 2022: Workshop “From Objects to Agents”.
The extended version is entitled "Agent talks about itself: an implementation using Jason, CArtAgO and Speech Acts" by same authors.

The repository contains code related to the example given in the extended version.
There has been a shift from using Jason to using the Jacamo framework for implementation.
The Jason interpreter has been enriched with a new agent class directly available in the source code and utility functions for this class, by means of which it is possible to equip agents of inner speech.
This first version does not include an ontology and requires certain care in properly programming the plans and knowledge base, as specific annotations and regular expressions are used, along with string manipulation, for justification.
In addition, the message selection features have also been customized to prioritize messages towards itself.
The functionality to make an agent get missing plans (as shown in the diagram) by sending a request to other agents is still under development and testing.

Prerequisites:
-----------------

- JDK 17 ([here](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html))
- Ubuntu 20.04 / macOS Monterey 12 or latest (not tested on Windows)
- Jacamo 1.1[^1]
- Jason 3.1[^1]
- Gradle 7.4.2 or 7.5.1[^2] ([here](https://gradle.org/install/)) 

[^1]: use the version provided in this repository and follow the subsequent instruction
[^2]: not required, but recommended

Prepare the enviroment:
----------------

Go inside the folder with Jacamo (same things for Jason) and run:
```
./gradlew config
```
Set $JAVA_HOME, $JACAMO_HOME and $JASON_HOME.
The steps for these configurations are shown in the output of the previous script.

At the end of these steps, the build directories will be generated and the jason and jacamo scripts work from the terminal (try typing jason or jacamo from a new shell.

***In 'jacamo 1.1/build/libs' the file jason-3.1.jar is excluded, so you have to copy it from the path jason 3.1/build/libs and paste it into the previous directory***

For more details on setup Jacamo and/or Jason follow the instruction for Developer in the README at this links: 
- https://github.com/jacamo-lang/jacamo
- https://github.com/jason-lang/jason

Run example:
-------------

After running gradle task and building jacamo.jar (build dir), go inside the example folder and run the script:
```
jacamo lay_the_table.jcm 
```
