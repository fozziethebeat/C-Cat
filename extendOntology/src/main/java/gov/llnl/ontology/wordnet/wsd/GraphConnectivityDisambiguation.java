package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Deque;


/**
 * @author Keith Stevens
 */
public abstract class GraphConnectivityDisambiguation
        implements WordSenseDisambiguation {

    private OntologyReader reader;

    /**
     * Disambiguates the {@link Annotations} in {@code targetWords} by  using
     * the {@link Set} {@link Synset}s carved out of the ontology for a given
     * setnence.
     *
     * @param targetWords A list of {@link Annotation}s and their possible
     *        senses that need to be disambiguated.
     * @param synsets The set of {@link Synset}s to consider for any graph
     *        traversal.
     * @param synsetBasis A mapping from {@link Synset} names to indices in a
     *        graph.
     * @param adjacencyMatrix An adjaceny matrix detailing how {@link Synset}s
     *        in {@code synsets} are connected.
     */
    protected abstract void processSentenceGraph(
            List<AnnotationSynset> targetWords,
            Set<Synset> synsets,
            StringBasisMapping synsetBasis,
            Matrix adjacencyMatrix);

    /**
     * {@inheritDoc}
     */
    public void setup(OntologyReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheritDoc}
     */
    public void disambiguate(List<Sentence> sentences) {
        for (Sentence sentence : sentences) {
            // Create the data structures needed to represent the carved out
            // WordNet graph for this sentence.

            // This set simply marks the set of all interests senses that we are
            // tracking.
            Set<Synset> synsets = new HashSet<Synset>();

            // This basis mapping will be used to index each synset into the
            // shortest path matrix.
            StringBasisMapping synsetBasis = new StringBasisMapping();

            // This matrix records the shortest path found so far between any
            // two synsets.  It will be dense, but needs to expand dynamically,
            // as we don't know the final number of synsets.
            Matrix adjacencyMatrix = new GrowingSparseMatrix();

            // Carve out a connected graph for the words in this sentence.  Only
            // select content words, i.e., Nouns, Verbs, Adverbs, or Ajdectives.

            List<AnnotationSynset> targetWords =
                new ArrayList<AnnotationSynset>();
            // First select the senses for the content words already in the
            // sentence.
            for (Annotation annot : sentence) {
                PartsOfSpeech pos = AnnotationUtil.synsetPos(annot);
                if (pos == null)
                    continue;

                Synset[] annotSenses = reader.getSynsets(
                        AnnotationUtil.word(annot), pos);
                for (Synset sense : annotSenses)
                    synsets.add(sense);

                targetWords.add(new AnnotationSynset(annotSenses, annot));
            }

            // Now perform a depth first search starting from the known synsets
            // to find any paths connecting synsets within this set.  Upon
            // finding such a path, add all of those synsets to synsets.
            // Hopefully this won't cause a concurrent modification exception :(
            Deque<Synset> path = new LinkedList<Synset>();
            for (Synset synset : synsets) 
                for (Synset related : synset.allRelations())
                    search(synset, related, synsets, path,
                           synsetBasis, adjacencyMatrix, 5);

            // Now that we've carved out the interesting subgraph and recorded
            // the shortest path between the synsets, pass it off to the sub
            // class which will do the rest of the disambiguation.
            processSentenceGraph(targetWords, synsets,
                                 synsetBasis, adjacencyMatrix);
        }
    }

    /**
     * Searches through the synset network starting from {@code current}.  It is
     * assumed that the search originated at {@code start} and will end either
     * at a {@link Synset} in {@code goals} or be terminated due to excessive
     * length.  When a valid path is found, {@code adjacencyMatrix} is
     * updated with the shortest distance found between {@code start} and the
     * goal node.  The same is done for each node found along the {@code path}.
     */
    private static void search(Synset start,
                               Synset current,
                               Set<Synset> goals,
                               Deque<Synset> path, 
                               StringBasisMapping synsetBasis,
                               Matrix adjacencyMatrix,
                               int maxLength) {
        // If we've found a goal node, update the shortest distance found
        // between the start node and the goal node.  Also include shortest path
        // information between each of the nodes along our path and the goal
        // node.
        if (goals.contains(current)) {
            updateLinks(start, current, synsetBasis, adjacencyMatrix);
            if (path.size() >= 2) {
                Iterator<Synset> pathIter = path.iterator();
                Synset prev = pathIter.next();
                while (pathIter.hasNext())
                    prev = updateLinks(prev, pathIter.next(), 
                                       synsetBasis, adjacencyMatrix);
            }

            goals.addAll(path);
            return;
        }

        // Bactrack out of the search if we've gone to deep.
        if (path.size() >= maxLength)
            return;

        // Push the current node onto the path and search it's neighbors.  After
        // searching all neighbors, backtrack.
        path.addFirst(current);
        for (Synset related : current.allRelations())
            search(start, related, goals, path,
                   synsetBasis, adjacencyMatrix, maxLength);
        path.removeFirst();
    }
    
    /**
     * Updates the shortest path distance between {@code start} and {@code
     * current}.
     */
    private static Synset updateLinks(Synset start,
                                      Synset current,
                                      StringBasisMapping synsetBasis,
                                      Matrix adjacencyMatrix) {
        int startId = synsetBasis.getDimension(start.getName());
        int endId = synsetBasis.getDimension(current.getName());
        adjacencyMatrix.set(startId, endId, 1);
        return current;
    }

    public class AnnotationSynset {
        Synset[] senses;
        Annotation annotation;

        public AnnotationSynset(Synset[] senses, Annotation annotation) {
            this.senses = senses;
            this.annotation = annotation;
        }
    }
}

