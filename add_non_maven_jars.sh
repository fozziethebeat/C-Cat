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

# Add wordsi 2.0 (Not in trunk yet)
mvn install:install-file -DgroupId=graph.edu.ucla.sspace \
-DartifactId=sspace-graph -Dversion=1.0 -Dpackaging=jar \
-Dfile=lib/sspace-graph.jar

# Add malt 1.5.3 (Not in trunk yet)
mvn install:install-file -DgroupId=org.maltparser -DartifactId=maltparser \
-Dversion=1.5.3 -Dpackaging=jar -Dfile=lib/malt.jar

# Add liblinear 1.7 (Not in trunk yet)
mvn install:install-file -DgroupId=lib.linear -DartifactId=liblinear \
-Dversion=1.7 -Dpackaging=jar -Dfile=lib/liblinear-1.7-with-deps.jar

# Add libsvm 1.0 (Not in trunk yet)
mvn install:install-file -DgroupId=lib.svm -DartifactId=libsvm \
-Dversion=1.0 -Dpackaging=jar -Dfile=lib/libsvm.jar

# Add hbase 0.90.1 (Not in trunk yet)
mvn install:install-file -DgroupId=org.apache.hbase -DartifactId=hbase \
-Dversion=0.90.1 -Dpackaging=jar -Dfile=lib/hbase-0.90.1.jar

mvn install:install-file -DgroupId=org.htmlparser -DartifactId=htmlparser \
-Dversion=1.6 -Dpackaging=jar -Dfile=lib/htmlparser.jar 
