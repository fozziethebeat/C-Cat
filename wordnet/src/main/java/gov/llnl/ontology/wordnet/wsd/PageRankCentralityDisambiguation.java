/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
 * An implementation of the PageRank based Word Sense Disambiguation
 * algorithm as described in the following paper:
 *
 * <ul>
 *  <li style="font-family:Garamond, Georgia, serif">Navigli, R.; Lapata, M.; ,
 *  "An Experimental Study of Graph Connectivity for Unsupervised Word Sense
 *  Disambiguation," Pattern Analysis and Machine Intelligence, IEEE
 *  Transactions on , vol.32, no.4, pp.678-692, April 2010.  Available 
 *  <a href="http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=4782967&isnumber=5420323">here</a>
 *  </li>
 * </ul>
 *
 * </p>
 *
 * This algorithm uses a the small connected graph created by {@link
 * GraphConnectivityDisambiguation} and selects the possible word sense that has
 * the highest PageRank in the extracted subgraph.
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @author Keith Stevens
 */
public class PageRankCentralityDisambiguation
        extends GraphConnectivityDisambiguation {

    /**
     * A {@link Synset} relation name to connected {@link Synsets} in the
     * subgraph.
     */
    public static final String LINK = "relation";

    /**
     * {@InheritDoc}
     */
    protected void processSentenceGraph(List<AnnotationSynset> targetWords,
                                        Set<Synset> synsets,
                                        StringBasisMapping synsetBasis,
                                        Matrix adjacencyMatrix) {
        // Create the list of synsets that will be in the PageRank graph and a
        // mapping from synsets to their indices.
        List<Synset> synsetList = Lists.newArrayList();
        Map<Synset, Integer> synsetMap = Maps.newHashMap();
        for (Synset synset : synsets) {
            int index = synsetBasis.getDimension(synset.getSenseKey());
            Synset newSynset = new BaseSynset(synset.getPartOfSpeech());
            newSynset.addSenseKey(synset.getSenseKey());
            synsetList.add(newSynset);
            synsetMap.put(newSynset, index);
        }

        // Add a link to each synset based on the adjacency matrix.
        for (int r = 0; r < adjacencyMatrix.rows(); ++r) 
            for (int c = 0; c < adjacencyMatrix.columns(); ++c)
                if (adjacencyMatrix.get(r,c) != 0d) {
                    Synset s1 = synsetList.get(r);
                    Synset s2 = synsetList.get(c);
                    s1.addRelation(LINK, s2);
                    s2.addRelation(LINK, s1);
                }

        // Evenly distribute the initial page rank score to each synset in the
        // graph.
        double length = synsetList.size();
        SparseDoubleVector ranks = new CompactSparseVector(synsetList.size());
        for (int i = 0; i < ranks.length(); ++i)
            ranks.set(i , 1/length);

        // Initialze the page rank algorithm and compute the page rank.
        SynsetPagerank.setupTransitionAttributes(synsetList, synsetMap);
        ranks = SynsetPagerank.computePageRank(synsetList, ranks, .15);

        // Find the target synset for each word that has the highest page rank
        // and mark the result annotation with that sense.
        for (AnnotationSynset annotSynset : targetWords) {
            Annotation word = annotSynset.annotation;
            Synset bestSense = annotSynset.senses[0];
            double bestRank = 0;
            for (Synset synset : annotSynset.senses) {
                int index = synsetBasis.getDimension(synset.getSenseKey());
                double rank = ranks.get(index);
                if (rank >= bestRank) {
                    bestRank = rank;
                    bestSense = synset;
                }
            }

            String term = AnnotationUtil.word(word);
            AnnotationUtil.setWordSense(word, bestSense.getSenseKey(term));
        }
    }

    /**
     * Returns "prcd", the acronyms for this {@link WordSenseDisambiguation}
     * algorithm.
     */
    public String toString() {
        return "prcd";
    }
}

