WOA 2022
====================

The repository was built as additional material of the extended version of paper entitled "Can agents talk about what they are doing? A proposal with Jason and speech acts" by Valeria Seidita, Francesco Lanza, Antonio Chella and Angelo Maria Pio Sabella" submitted to the WOA 2022: Workshop “From Objects to Agents”.
The extended version is entitled "Agent talks about itself: an implementation using Jason, CArtAgO and Speech Acts" by same authors.

The repository contains code related to the example given in the extended version.
There has been a shift from using Jason to using the Jacamo framework for implementation.
The Jason interpreter has been enriched with a new agent class directly available in the source code and utility functions for this class, by means of which it is possible to equip agents of inner speech.
This first version does not include an ontology and requires certain care in properly programming the plans and knowledge base, as specific annotations and regular expressions are used, along with string manipulation, for justification.

Prerequisites:
-----------------

- JDK 17
- Ubuntu 20.04 / macOS Monterey 12 or latest
- Jacamo 1.1 (use the one provided in the repository)
- Jason 3.1 (needed for developers, use the version provided)
- Gradle 7.4.2 or 7.5.1

Prepare the enviroment:
----------------

Go inside the folder with Jacamo (same things for Jason) and run:
```
./gradlew config
```
Set $JAVA_HOME, $JACAMO_HOME, and if used $JASON_HOME

For more details on setup follow the instruction for Jacamo developer in the README at this link: 
- https://github.com/jacamo-lang/jacamo

Run example:
-------------

After running gradle task and building jacamo.jar (build dir), go inside the example folder and run the script:
```
jacamo example_name.jcm 
```
