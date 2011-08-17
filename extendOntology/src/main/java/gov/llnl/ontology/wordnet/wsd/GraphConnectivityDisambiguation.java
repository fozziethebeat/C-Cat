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

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import com.google.common.collect.Lists;

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
 * An abstract base class for any of the graph centrality Word Sense Disambiguation
 * algorithms described in the following paper:
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
 * This base class extracts a small connected graph from the wordnet heirarchy
 * that is centered around content words in a sentence that needs to be
 * disambiguated.  The target sense for each content word will be included in
 * the graph, along with any {@link Synset}s in the shortest path connecting
 * these target senses.  The extracted graph structure for each focus word to be
 * disambiguated will passed to subclasses as an affinity {@link Matrix} that
 * records the known edges.  
 *
 * </p>
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
     *        senses that need to be disambiguated.  The {@link
     *        Annotation} in each {@link AnnotationSynset} should be updated
     *        with the selected word sense.
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
    public List<Sentence> disambiguate(List<Sentence> sentences) {
        List<Sentence> resultSentences = Lists.newArrayList();

        for (Sentence sentence : sentences) {
            // Create the new disambiguated sentence.
            Sentence disambiguated = new Sentence(
                    sentence.start(), sentence.end(), sentence.numTokens());
            resultSentences.add(disambiguated);

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

            List<AnnotationSynset> targetWords = Lists.newArrayList();

            // First select the senses for the content words already in the
            // sentence.
            int i = 0;
            for (Annotation annot : sentence) {
                Annotation result = new Annotation();
                AnnotationUtil.setSpan(result, AnnotationUtil.span(annot));
                disambiguated.addAnnotation(i++, result);

                PartsOfSpeech pos = AnnotationUtil.synsetPos(annot);
                if (pos == null)
                    continue;

                Synset[] annotSenses = reader.getSynsets(
                        AnnotationUtil.word(annot), pos);
                for (Synset sense : annotSenses)
                    synsets.add(sense);

                targetWords.add(new AnnotationSynset(annotSenses, result));
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

        return resultSentences;
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

    /**
     * A structure class that represents a {@link Annotation} that needs to be
     * disambiguated and it's possible target {@link Synset}s.
     */
    public class AnnotationSynset {

        /**
         * The target {@link Synset}s.
         */
        Synset[] senses;

        /**
         * The {@link Annotation} to be disambiguated.
         */
        Annotation annotation;

        /**
         * Creates a new {@link AnnotationSynset}.
         */
        public AnnotationSynset(Synset[] senses, Annotation annotation) {
            this.senses = senses;
            this.annotation = annotation;
        }
    }
}

