# C-Cat Wordnet - A simple Java library for Princeton Wordnet

## Introduction

C-Cat Wordnet is a java framework for using and editing the Princeton Wordnet
lexical ontology.  It includes a simple to use tools for retrieving synsets,
comparing synsets with similarity measurse, disambiguating sentences, and
directly modifying the synset hierachy to include new information.

## Build and Installation

C-Cat Wordnet uses maven for compilation and dependency management.  It depends
a sister sub-module for the larger C-Cat package (util).  After installing the
C-Cat util package, run the following from the wordnet directory:

  mvn package 

This will create an all inclusive jar that includes the C-Cat Wordnet library,
all depenendencies, and a copy of the Princeton Wordnet 3.0 dictionary files.

## Using Wordnet

### Key Components

The Wordnet library centers around two core concepts: a `OntologyReader` and a
set of `Synset`s.  The `OntologyReader` reads a Wordnet database from files on
disk or included in the running classpath and creates an in memory structure
mapping lexical units to `Synset`s.  Each `Synset` connects together words that
have a shared meaning and is further connected to other `Synset`s that are
semantically related.  For instance, one `Synset` may collect 'child', 'kid',
and 'youngster' together and link to the more abstract `Synset` for 'juvenile'.

The default `OntologyReader` reads the standard Princeton Wordnet dictionary
files and is named `WordNetCorpusReader`.  You can load the database using two
simple methods:

  OntologyReader reader = WordNetCorpusReader.initialize("/path/to/dict");

or

  OntologyReader reader = WordNetCorpusReader.initialize("dict", true);

The first method simply reads the dictionary files from the specified path and
loads the entire hierarhcy into memory.  The second method makes use of the
dictionary files included in the all inclusive jar and reads the dictionary
files from *within* the jar itself.  This shows the simplest example for loading
the database from within nearly any environment.

### Making Simple Queries

The `OntologyReader` provides several methods for accessing `Synset`s given
words.  To get all possible `Synset`s for the word 'cat', you can do the following:

  Synset[] allSynsets = reader.getSynsets("cat");
  Synset[] nounSynsets = reader.getSynsets("cat", PartsOfSpeech.NOUN);
  Synset cat1 = reader.getSynset("cat", PartsOfSpeech.NOUN, 1);

Each example shows how to access increasingly more specific `Synset`s for a
word.  The first returns any `Synset` associated with 'cat'.  The second returns
only noun `Synset`s associated with 'cat'.  The last returns only the first noun
`Synset` for 'cat'.  

Once you've obtained a synset, you can access a variety of details:

  // Get the definition
  cat1.getGloss();

  // Iterate through the lemmas
  for (Lemma lemma : cat1.getLemmas())
      System.out.println(lemma);

  // Print the Synset name.  This is a simple reference for the Synset.
  System.out.println(cat1.getName());

  // Print the name of each parent Synset
  for (Synset parent : cat1.getParents())
      System.out.println(parent.getName());

  // Print the name of any related Synset
  for (Synset related : cat1.allRelations())
      System.out.println(related.getName());

You can furthremore modify the synset with new details:

  // Change the definition
  cat1.setDefinition("A cute fluffy pet");

  // Add a new related Synset into the hierachy.
  Synset newSyn = new BaseSynset("nori.n.1");
  cat1.addRelation(Relation.HYPONYM, newSyn);
  newSyn.addRelation(Relation.HYPERNYM, cat1);

### Saving Your Modifications

The C-Cat Wordnet package makes it easy to save these changes to any format you
can design.  If you want to save the changes we've made to the database above,
you can simply do:

  OntologyWriter writer = new WordNetCorpusWriter();
  writer.saveOntology(reader, "/path/to/new/dict");

This will save the modified hierarchy to the specified path in the standard
Princeton Wordnet database format, which can easily be loaded again using the
C-Cat Wordnet package or nearly any other library for the Princeton Wordnet.
