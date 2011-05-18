#!/bin/bash

# Add stanford nlp (Not in trunk yet)
mvn install:install-file -DgroupId=edu.stanford.nlp -DartifactId=stanfordNlp \
-Dversion=2010-11-12 -Dpackaging=jar -Dfile=lib/stanford-corenlp-2010-11-12.jar

# Add llnl text commons
mvn install:install-file -DgroupId=gov.llnl.text -DartifactId=textCommons \
-Dversion=1.5.2 -Dpackaging=jar -Dfile=lib/text-commons-1.5.2.jar

# Add wordsi 2.0 (Not in trunk yet)
mvn install:install-file -DgroupId=edu.ucla.sspace -DartifactId=sspace \
-Dversion=2.0 -Dpackaging=jar -Dfile=lib/sspace-lib.jar
