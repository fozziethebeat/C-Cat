package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetPagerank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class PageRankCentralityDisambiguation
        extends GraphConnectivityDisambiguation {

    public static final String LINK = "relation";

    protected void processSentenceGraph(List<AnnotationSynset> targetWords,
                                        Set<Synset> synsets,
                                        StringBasisMapping synsetBasis,
                                        Matrix adjacencyMatrix) {
        List<Synset> synsetList = Lists.newArrayList();
        Map<Synset, Integer> synsetMap = Maps.newHashMap();
        for (int i = 0; i < synsetBasis.numDimensions(); ++i) {
            Synset newSynset = new BaseSynset(
                    synsetBasis.getDimensionDescription(i));
            synsetList.add(newSynset);
            synsetMap.put(newSynset, i);
        }

        for (int r = 0; r < adjacencyMatrix.rows(); ++r) 
            for (int c = 0; c < adjacencyMatrix.columns(); ++c)
                if (adjacencyMatrix.get(r,c) != 0d) {
                    Synset s1 = synsetList.get(r);
                    Synset s2 = synsetList.get(c);
                    s1.addRelation(LINK, s2);
                    s2.addRelation(LINK, s1);
                }

        double length = synsetList.size();
        SparseDoubleVector ranks = new CompactSparseVector(synsetList.size());
        for (int i = 0; i < ranks.length(); ++i)
            ranks.set(i , 1/length);
        SynsetPagerank.setupTransitionAttributes(synsetList, synsetMap);
        ranks = SynsetPagerank.computePageRank(synsetList, ranks, .15);

        for (AnnotationSynset annotSynset : targetWords) {
            Annotation word = annotSynset.annotation;
            Synset bestSense = null;
            double bestRank = 0;
            for (Synset synset : annotSynset.senses) {
                int index = synsetBasis.getDimension(synset.getName());
                double rank = ranks.get(index);
                if (rank >= bestRank) {
                    bestRank = rank;
                    bestSense = synset;
                }
            }
            if (bestSense != null)
                AnnotationUtil.setWordSense(word, bestSense.getName());
        }
    }
}

