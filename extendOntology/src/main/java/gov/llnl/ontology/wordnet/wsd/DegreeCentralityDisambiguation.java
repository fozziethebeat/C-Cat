package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.Synset;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.matrix.Matrix;

import java.util.List;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class DegreeCentralityDisambiguation
        extends GraphConnectivityDisambiguation {
     
    protected void processSentenceGraph(List<AnnotationSynset> targetWords,
                                        Set<Synset> synsets,
                                        StringBasisMapping synsetBasis,
                                        Matrix adjacencyMatrix) {
        int[] degreeCounts = new int[synsets.size()];
        for (int r = 0; r < adjacencyMatrix.rows(); ++r)
            for (int c = 0; c < adjacencyMatrix.columns(); ++c)
                if (adjacencyMatrix.get(r,c) != 0d) {
                    degreeCounts[r]++;
                    degreeCounts[c]++;
                }

        for (AnnotationSynset annotSynset : targetWords) {
            Annotation word = annotSynset.annotation;
            Synset bestSense = null;
            double bestDegree = 0;
            for (Synset synset : annotSynset.senses) {
                int index = synsetBasis.getDimension(synset.getName());
                if (degreeCounts[index] >= bestDegree) {
                    bestDegree = degreeCounts[index];
                    bestSense = synset;
                }
            }
            if (bestSense != null)
                AnnotationUtil.setWordSense(word, bestSense.getName());
        }
    }
}

