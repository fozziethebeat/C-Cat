package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.TextUtil;
import gov.llnl.ontology.util.StringPair;
import gov.llnl.ontology.wordnet.Lemma;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import com.google.common.collect.Maps;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.util.WorkQueue;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class DisambiguateDocTags {

    public static void main(String[] args) throws Exception {
        final BasisMapping<String, String> basis = new StringBasisMapping();

        System.err.println("Loading synset vectors");
        // Build up term occurence vectors for every Synset using the
        // glosses and examples for the target synset and it's distance 1
        // neighbors.
        final OntologyReader reader = WordNetCorpusReader.initialize(args[0]);
        final Map<Synset, SparseDoubleVector> synsetVectors = Maps.newHashMap();
        for (Synset synset : reader.allSynsets()) {
            SparseDoubleVector synVector = new CompactSparseVector();
            synsetVectors.put(synset, synVector);
            addLexicallTerms(synset, basis, synVector);
            for (Synset related : synset.allRelations())
                addLexicallTerms(related, basis, synVector);
        }
        System.err.printf("Loaded %s synsets\n", synsetVectors.size());

        // Set the basis mapping to read only so that words in one set but not
        // the second do not get added to the vectors.
        basis.setReadOnly(true);

        System.err.println("Loading tag vectors");
        // Read in each tag,word count line and build up tag by term occurrence
        // vectors.
        BufferedReader br = new BufferedReader(new FileReader(args[1]));
        Map<String, SparseDoubleVector> tagVectors = Maps.newHashMap();
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] pairCount = line.split("\\t", 2);
            StringPair pair = StringPair.fromString(pairCount[0]);

            SparseDoubleVector tagVector = tagVectors.get(pair.x);
            if (tagVector == null) {
                tagVector = new CompactSparseVector();
                tagVectors.put(pair.x, tagVector);
            }

            int index = basis.getDimension(pair.y.toLowerCase());
            if (index < 0)
                continue;
            int count = Integer.parseInt(pairCount[1]);
            tagVector.set(index, count);
        }

        WorkQueue workQueue = new WorkQueue();
        Object key = workQueue.registerTaskGroup(tagVectors.size());

        System.err.println("Comparing tags to synsets");
        // For each tag vector, find the synset with the highest cosine
        // similarity.
        for (final Map.Entry<String, SparseDoubleVector> entry : 
                tagVectors.entrySet())
            workQueue.add(key, new Runnable() {
                public void run() {
                    System.err.println("Comparing " + entry.getKey());
                    String[] tagParts = entry.getKey().toLowerCase().split("/");
                    String lastPart = tagParts[tagParts.length-1];
                    lastPart = lastPart.replaceAll("\\(.*", "").trim();

                    Synset[] synsets = reader.getSynsets(lastPart);
                    Synset bestSynset = null;
                    double bestScore = -1;
                    if (synsets.length == 1) {
                        System.err.println("Using only sense");
                        bestSynset = synsets[0];
                    } else if (synsets.length > 1) {
                        System.err.println("Finding best sense");
                        SparseDoubleVector tagVector = entry.getValue();
                        for (String part : tagParts) {
                            addTerm(part, basis, tagVector);
                            for (String term : part.split("\\s+"))
                                addTerm(term, basis, tagVector);
                        }

                        bestSynset = synsets[0];
                        for (int i = 0; i < synsets.length; ++i) {
                            double score = Similarity.cosineSimilarity(
                                tagVector, synsetVectors.get(synsets[i]));
                            if (score > bestScore) {
                                bestScore = score;
                                bestSynset = synsets[i];
                            }
                        }
                    } else {
                        System.err.println("Searching all senses");
                        SparseDoubleVector tagVector = entry.getValue();
                        for (String part : tagParts) {
                            addTerm(part, basis, tagVector);
                            for (String term : part.split("\\s+"))
                                addTerm(term, basis, tagVector);
                        }

                        for (Map.Entry<Synset, SparseDoubleVector> synEntry :
                                synsetVectors.entrySet()) {
                            double score = Similarity.cosineSimilarity(
                                entry.getValue(), synEntry.getValue());
                            if (score > bestScore) {
                                bestScore = score;
                                bestSynset = synEntry.getKey();
                            }
                        }
                    }
                    if (bestSynset != null)
                        System.out.printf("%s %s\n", entry.getKey(),
                                          bestSynset.getName());
                    System.err.println("Done comparing " + entry.getKey());
                }
            });
        workQueue.await(key);
    }

    public static void addLexicallTerms(Synset synset,
                                        BasisMapping<String, String> basis,
                                        SparseDoubleVector vector) {
        // Account for all of the lemmas.
        for (Lemma lemma : synset.getLemmas())
            addTerm(lemma.getLemmaName(), basis, vector);

        // Account for each word in the examples.
        for (String example : synset.getExamples())
            for (String term : example.split("\\s"))
                addTerm(term, basis, vector);

        // Account for each word in the definition.
        for (String term : synset.getDefinition().split("\\s+"))
            addTerm(term, basis, vector);
    }

    public static void addTerm(String term,
                               BasisMapping<String, String> basis,
                               SparseDoubleVector vector) {
        int index = basis.getDimension(TextUtil.cleanTerm(term));
        if (index < 0)
            return;
        vector.add(index, 1);
    }
}
