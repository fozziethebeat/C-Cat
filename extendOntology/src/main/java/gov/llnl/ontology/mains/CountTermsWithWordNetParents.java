package gov.llnl.ontology.mains;

import gov.llnl.ontology.util.Counter;

import gov.llnl.ontology.wordnet.Lemma;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * This main traverses Dependency Parsed documents in the CoNLL format and
 * counts the number of times each noun in a sentence has one of it's WordNet
 * parent terms in the sentence.  It is assumed that if two words are in the
 * sentence, they can be connected via some dependency path since all sentences
 * are connected graphs.  Optionally, these nouns can also be sorted into
 * ascending or descending order and either all or only the top N can be
 * emitted.
 *
 * @author Keith Stevens
 */
public class CountTermsWithWordNetParents {

    private static final PartsOfSpeech NOUN = PartsOfSpeech.NOUN;

    private OntologyReader wordnet;

    private DependencyExtractor extractor;

    private Counter counter;

    public CountTermsWithWordNetParents(OntologyReader wordnet,
                                        DependencyExtractor extractor) {
        this.wordnet = wordnet;
        this.extractor = extractor;

        counter = new Counter();
    }

    public void processDocument(BufferedReader document) throws IOException {
        for (DependencyTreeNode[] tree = null;
             (tree = extractor.readNextTree(document)) != null; ) {
            Set<String> nounsInSentence = new HashSet<String>();
            for (DependencyTreeNode node : tree)
                if (node.pos().toLowerCase().startsWith("n"))
                    nounsInSentence.add(node.word());

            for (DependencyTreeNode node : tree) {
                if (!node.pos().toLowerCase().startsWith("n"))
                    continue;

                String focusWord = node.word();
                Synset[] synsets = wordnet.getSynsets(focusWord, NOUN);

                for (Synset synset : synsets)
                    for (Synset parent : synset.getParents())
                        for (Lemma lemma : parent.getLemmas())
                            if (nounsInSentence.contains(lemma.getLemmaName()))
                                counter.count(focusWord);
            }
        }
    }

    public void printTerms(boolean orderWords,
                           boolean orderAscending, 
                           int numRetainedWords) {
        Collection<String> validNounsObserved = (orderWords)
            ? counter.itemsSorted(orderAscending)
            : counter.items();

        for (String term : validNounsObserved) {
            if (numRetainedWords-- <= 0)
                break;
            System.out.println(term);
        }
    }

    public static void main(String[] args) throws IOException {
        ArgOptions options = new ArgOptions();

        options.addOption('r', "retainNumWords",
                          "If set, only the top N nouns will be emitted, " +
                          "rather than all nouns, this is only used in " +
                          "conjunction with the order option.",
                          true, "INT", "Optional");
        options.addOption('o', "order",
                          "If set, nouns will be ordered based on the number " +
                          "of valid occurrences observed.  Valid values for " +
                          "order are a for ascending and d for descending.",
                          true, "a|d", "Optional");
        options.parseOptions(args);
        
        if (options.numPositionalArgs() != 2) {
            System.out.println("usage: java CTWWNP [OPTIONS] " +
                               " /path/to/wordnet/dir" +
                               " dependency-parsed-CoNLL-file.txt\n\n"
                               + options.prettyPrint());
            System.exit(1);
        }

        OntologyReader wordnet = WordNetCorpusReader.initialize(
                options.getPositionalArg(0));
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Iterator<Document> docIter = new DependencyFileDocumentIterator(
                options.getPositionalArg(1));

        boolean orderWords = options.hasOption('o');
        int numRetainedWords = (orderWords) 
            ? options.getIntOption("retainNumWords")
            : Integer.MAX_VALUE;
        boolean orderAscending = options.getStringOption('o', "a").equals("a");

        CountTermsWithWordNetParents counter = new CountTermsWithWordNetParents(
                wordnet, extractor);

        while (docIter.hasNext())
            counter.processDocument(docIter.next().reader());
        counter.printTerms(orderWords, orderAscending, numRetainedWords);
    }

}
