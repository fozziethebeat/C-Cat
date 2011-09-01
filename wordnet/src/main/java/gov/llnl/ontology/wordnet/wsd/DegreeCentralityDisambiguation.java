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
import gov.llnl.ontology.wordnet.Synset;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.matrix.Matrix;

import java.util.List;
import java.util.Set;


/**
 * An implementation of the Degree Centrality based Word Sense Disambiguation
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
 * the highest degree in the extracted subgraph.
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @see GraphConnectivityDisambiguation
 *
 * @author Keith Stevens
 */
public class DegreeCentralityDisambiguation
        extends GraphConnectivityDisambiguation {
     
    /**
     * {@inheritDoc}
     */
    protected void processSentenceGraph(List<AnnotationSynset> targetWords,
                                        Set<Synset> synsets,
                                        StringBasisMapping synsetBasis,
                                        Matrix adjacencyMatrix) {
        // Count the degree for each link in the subgraph centered around the
        // content words stored in targetWords.  As the graph is undirected, we
        // increase the count for both nodes in each edge.
        int[] degreeCounts = new int[synsets.size()];
        for (int r = 0; r < adjacencyMatrix.rows(); ++r)
            for (int c = 0; c < adjacencyMatrix.columns(); ++c)
                if (adjacencyMatrix.get(r,c) != 0d) {
                    degreeCounts[r]++;
                    degreeCounts[c]++;
                }

        // For each word that needs to be disambiguated, get it's possible
        // target senses and determine which one has the highest degree. 
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

            // If we found a best sense, store it in the result annotation.
            if (bestSense != null)
                AnnotationUtil.setWordSense(word, bestSense.getName());
        }
    }

    /**
     * Returns "dcd", the acronyms for this {@link WordSenseDisambiguation}
     * algorithm.
     */
    public String toString() {
        return "dcd";
    }
}

