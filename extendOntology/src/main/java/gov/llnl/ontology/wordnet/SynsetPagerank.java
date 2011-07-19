/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.DoubleVectorAttribute;
import gov.llnl.ontology.wordnet.Synset;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class performs PageRank over a graph of {@link Synset}s.  {@link
 * Synset}s are assumed to be connected with an arbitrary set of directional
 * links.  Each {@link Synset} will be given a single score that rates it's
 * importance in the graph.
 *
 * </p>
 *
 * This algorithm works as follows:
 * </ol>
 *     <li> Each {@link Synset} is given a set of transition probabilities.  Each
 *     outgoing link from a {@link Synset} is assumed to have equal weight.  These
 *     transition probabilities are stored as an {@link Attribute} in the {@link
 *     Synset}.  Calling {@link setupTransitionAttributes} will create these
 *     {@link Attribute}s for each {@link Synset}.  If there is a core subgraph
 *     that is held constant through multiple Page Rank runs, it is suggested
 *     that this core subgraph is stored in a separate list and {@link
 *     setupTransitionAttributes} be called once on this subgraph. </li>
 *
 *     <li> If there are other nodes beyond the core subgraph, they should be
 *     given transition probability {@link Attributes} through {@link
 *     setTransitionAttribute}. </li>
 *
 *     <li> The Page Rank algorithm is ran over the graph for a fixed number of
 *     iterations.  In each iteration, the page rank score is updated and used
 *     in conjunction with a set of random surfer probabilities. </li>
 * </ol>
 *
 * </p>
 * This implementation is designed such that Page Rank can be run over several
 * graphs that have the same core subgraph in parrallel.  Each {@link Synset} in
 * the graph is only given a set of transition probabilities.  As long as the
 * link structure is held constant, there is no need to setup new transition
 * probabilities for the core graph.
 *
 * @author Keith Stevens
 */
public class SynsetPagerank {

    /**
     * The identifier for an {@link Attribute} of transition probabilities.
     * These {@link Attribute}s will simply be {@link SparseDoubleVector}s.
     */
    public static final String TRANSITION_ATTRIBUTE = "transitionAttribute";

    /**
     * Create a {@link Attribute} for the transition probabilities of the given
     * {@link Synset}.  Each connection is given an even weight.  {@code
     * synsetMap} provides the desired indices for each possible {@link Synset}.
     *
     * @param synset The synset for which transition probability transtions will
     *        be added
     * @param synsetMap A mapping from {@link Sysnet}s to a vector indices
     *
     * @throws NullPointerException if {@code synsetMap} does not contain a
     *         mapping for an outward link in {@code synset}
     */
    public static void setTransitionAttribute(Synset synset,
                                              Map<Synset, Integer> synsetMap) {
        double numRelations = synset.getNumRelations();

        // Create the set of transition probabilities for this synset.
        SparseDoubleVector transitionProbabilities = new CompactSparseVector();
        for (String relation : synset.getKnownRelationTypes())
            for (Synset related : synset.getRelations(relation))
                transitionProbabilities.set(
                        synsetMap.get(related).intValue(), 1d/numRelations);

        // Set the attribute.
        synset.setAttribute(
                TRANSITION_ATTRIBUTE,
                new DoubleVectorAttribute<SparseDoubleVector>(
                    transitionProbabilities));
    }

    /**
     * Adds transition probability {@link Attribute}s for each {@link Synset} in
     * {@code synsetList}.  The indices for outward links are specified by
     * {@code synsetMap}.
     *
     * @param synset The list of synsets for which transition probability
     *        transtions will be added
     * @param synsetMap A mapping from {@link Sysnet}s to a vector indices
     *
     * @throws NullPointerException if {@code synsetMap} does not contain a
     *         mapping for an outward link in any {@link Synset}
     */
    public static void setupTransitionAttributes(
            List<Synset> synsetList,
            Map<Synset, Integer> synsetMap) {
        for (Synset synset : synsetList) 
            setTransitionAttribute(synset, synsetMap);
    }

    /**
     * Returns a {@link SparseDoubleVector} representing the page rank scores of
     * each synset in {@code synsetList}.  Each {@link Synset} is assumed to
     * have a {@link SparseDoubleVector} {@link Attribute} representing the
     * transition probabilities, which can be setup by calling {@code
     * setupTransitionAttributes}.  Altogether, these transition vectors form
     * the transition matrix needed in the PageRank computation.   Note that if
     * one sets the {@link sourceWeights} carefully, a customized page rank can
     * be computed.  It is recomended that a small number of values in {@link
     * sourceWeights} be set to a non zero value if {@link synsetList} contains
     * a large number of {@link Synset}s from the original word net graph.
     *
     * @param synsetList The set of {@link Sysnet}s over which to compute page
     *        rank scores
     * @param sourceWeights A vector of teleporation probabilities, i.e., the
     *        probability that a random surfer lands at a particular synset
     *        given that a random jump was made
     * @param weight The probability of making a random jump at any point in
     *        time
     */
    public static SparseDoubleVector computePageRank(
            List<Synset> synsetList,
            SparseDoubleVector sourceWeights,
            double weight) {
        SparseDoubleVector pageRanks = sourceWeights;

        // For now, just compute a fixed number of iterations.
        for (int i = 0; i < 15; ++i) {
            SparseDoubleVector newRanks = new SparseHashDoubleVector(
                    pageRanks.length());

            // While computing the new page rank vector, compute the l1 norm of
            // the old page ranks.
            double oldL1Norm = 0;

            // Compute the initial page rank values for each vertex in the
            // graph.  This is normally done by multiplying the transpose of the
            // full transition matrix by the page rank vector.  Since computing
            // this with the synset structure, we do the multiplcation in a
            // different order.  The transition probabilities for each synset
            // are multiplied by the by the probability of page rank of that
            // synset and having followed a synset link.  Each probability
            // accumulates towards the page rank of the outgoing synset.
            for (int index : pageRanks.getNonZeroIndices()) {
                // Compute the probability of following a link from a synset and
                // having landed at a particular synset.
                double oldRank = pageRanks.get(index);
                double oldScore = oldRank * weight;

                // Update the l1 norm.
                oldL1Norm += Math.abs(oldRank);

                // Get the transition probabilities.
                Synset synset = synsetList.get(index);
                Attribute attribute = synset.getAttribute(TRANSITION_ATTRIBUTE);
                SparseDoubleVector transitionProbs =
                    (SparseDoubleVector) attribute.object();

                // Increase the page rank for each outgoing link based on the
                // probability of following that link.
                for (int rankIndex : transitionProbs.getNonZeroIndices())
                    newRanks.add(rankIndex, 
                                 oldScore * transitionProbs.get(rankIndex));
            }

            // Compute the difference in L1 norms for the old and new rank
            // values.
            double newL1Norm = 0;
            for (int index : newRanks.getNonZeroIndices())
                newL1Norm += Math.abs(newRanks.get(index));
            double gamma = oldL1Norm - newL1Norm;

            // Add in random surfer probabilities.
            for (int index : newRanks.getNonZeroIndices())
                newRanks.add(index, gamma * sourceWeights.get(index));

            // Compute the change in rank scores.  This is simply the l1 norm of
            // the difference between the two page rank vectors.  We compute
            // this by first computing the difference between indices in the old
            // page rank scores and the new page rank scores.  We then simply
            // add in the l1 norm of all the indices in the new page rank scores
            // that were not in the old page rank scores.
            double delta = 0;
            Set<Integer> oldIndices = new HashSet<Integer>();
            for (int index : pageRanks.getNonZeroIndices()) {
                oldIndices.add(index);
                delta += Math.abs(newRanks.get(index) - pageRanks.get(index));
            }
            for (int index : newRanks.getNonZeroIndices()) {
                if (!oldIndices.contains(index))
                    delta += Math.abs(newRanks.get(index));
            }

            // Save the new page rank scores.
            pageRanks = newRanks;
        }

        return pageRanks;
    }
}
