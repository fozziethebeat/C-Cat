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
import gov.llnl.ontology.wordnet.Synset.Relation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.util.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    /**
     * The name of the undirected link relation that represents any related link
     * between two {@link Synset}s.
     */
    private static final String LINK = "link";

    /**
     * The {@link OntologyReader} used to access {@link Synset}s.
     */
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

        // For every synset, check for the number of senses for each word in the
        // definition.  For any monosemous word, add a "gloss" relation between
        // the current synset and the monosemous sense of the word.
        for (Synset synset : reader.allSynsets()) {
            // First, create a new, undirected link for every link this synset
            // has with all other synsets.  We do this by adding a new "fake"
            // link type called "link" exists in both synsets that share a
            // relation.  These are guaranteed to not have self links.
            for (Relation relation : Relation.values())
                for (Synset related : synset.getRelations(relation)) {
                    synset.addRelation(LINK, related);
                    related.addRelation(LINK, synset);
                }

            // Now add the gloss links.
            for (String glossTerm : synset.getDefinition().split("\\s+")) {
                Synset[] glossSynsets = reader.getSynsets(glossTerm);
                // Check that this term has synsets and that the known sense is
                // not the current synset (we want to avoid self links).  If
                // everything is ok, add a "undirected" link between the two
                // synsets.
                if (glossSynsets.length == 1 && synset != glossSynsets[0]) {
                    synset.addRelation(LINK, glossSynsets[0]);
                    glossSynsets[0].addRelation(LINK, synset);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Sentence disambiguate(Sentence sentence) {
        return disambiguate(sentence, null);
    }

    /**
     * {@inheritDoc}
     */
    public Sentence disambiguate(Sentence sentence, Set<Integer> focusIndices) {
        // Create the new disambiguated sentence.
        Sentence disambiguated = new Sentence(
                sentence.start(), sentence.end(), sentence.numTokens());

        // Create the data structures needed to represent the carved out
        // WordNet graph for this sentence.

        // This set simply marks the set of all interests senses that we are
        // tracking.
        Set<Synset> synsets = Sets.newHashSet();

        // Carve out a connected graph for the words in this sentence.  Only
        // select content words, i.e., Nouns, Verbs, Adverbs, or Ajdectives.
        List<AnnotationSynset> targetWords = Lists.newArrayList();

        // First select the senses for the content words already in the
        // sentence.
        int i = 0;
        for (Annotation annot : sentence) {
            Annotation result = new Annotation();
            AnnotationUtil.setSpan(result, AnnotationUtil.span(annot));
            disambiguated.addAnnotation(i, result);

            if (focusIndices == null || 
                focusIndices.isEmpty() || 
                focusIndices.contains(i++)) {
                Synset[] annotSenses = getSynsets(annot);

                // Skip any words that have no senses.
                if (annotSenses == null || annotSenses.length == 0)
                    continue;

                for (Synset sense : annotSenses)
                    synsets.add(sense);

                String term = AnnotationUtil.word(annot);
                targetWords.add(
                        new AnnotationSynset(annotSenses, result, term));
            }
        }

        // Now perform a depth first search starting from the known synsets
        // to find any paths connecting synsets within this set.
        Deque<Synset> path = Lists.newLinkedList();
        Map<Pair<Synset>, Deque<Synset>> shortestPaths = Maps.newHashMap();
        for (Synset synset : synsets) 
            for (Synset related : synset.getRelations(LINK))
                search(synset, related, synsets, path, shortestPaths, 6);


        // Build the mapping between each synset's first sense key and a
        // unique dimension.  Also compute the adjacency matrix for the relevant
        // senses.
        Matrix adjacencyMatrix = new GrowingSparseMatrix();
        StringBasisMapping synsetBasis = new StringBasisMapping();
        for (Map.Entry<Pair<Synset>, Deque<Synset>> e :
                shortestPaths.entrySet()) {
            // Add the last value in the path to the path list.  With this, the
            // loop below will automatically add a link between the last node
            // and it's previous link.
            e.getValue().addLast(e.getKey().y);

            // Record each link in the path to the adjacency matrix.  Each
            // synset should get it's own unique index, and each edge has weight
            // 1.
            Synset prev = e.getKey().x;
            for (Synset node : e.getValue()) {
                int j = synsetBasis.getDimension(prev.getSenseKey());
                int k = synsetBasis.getDimension(node.getSenseKey());
                adjacencyMatrix.set(j, k, 1.0);
                prev = node;
            }
        }
        synsetBasis.setReadOnly(true);

        // Now that we've carved out the interesting subgraph and recorded
        // the shortest path between the synsets, pass it off to the sub
        // class which will do the rest of the disambiguation.
        processSentenceGraph(targetWords, synsets,
                             synsetBasis, adjacencyMatrix);

        return disambiguated;
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
                               Map<Pair<Synset>, Deque<Synset>> shortestPaths,
                               int maxLength) {
        // If we've found a goal node, update the shortest distance found
        // between the start node and the goal node.  Also include shortest path
        // information between each of the nodes along our path and the goal
        // node.
        if (goals.contains(current)) {
            Pair<Synset> key = new Pair<Synset>(start, current);
            Deque<Synset> oldPath = shortestPaths.get(key);
            if (oldPath == null || oldPath.size() > path.size())
                shortestPaths.put(key, Lists.newLinkedList(path));
        }

        // Bactrack out of the search if we've gone to deep.
        if (path.size() >= maxLength)
            return;

        // Push the current node onto the path and search it's neighbors.  After
        // searching all neighbors, backtrack.
        path.addLast(current);
        for (Synset related : current.getRelations(LINK))
            search(start, related, goals, path, shortestPaths, maxLength);
        path.removeLast();
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
         * The original term describing {@code annotation}.
         */
        String term;

        /**
         * Creates a new {@link AnnotationSynset}.
         */
        public AnnotationSynset(Synset[] senses, 
                                Annotation annotation,
                                String term) {
            this.senses = senses;
            this.annotation = annotation;
            this.term = term;
        }
    }

    /**
     * Returns all of the {@link Synset}s found given the word and part of
     * speech information, if present, in {@code annot}.  If the part of speech
     * is available, but provides no synsets, all possible synsets are returned
     * for the word, under the assumption that the tag may be incorrect.
     */
    protected Synset[] getSynsets(Annotation annot) {
        String word = AnnotationUtil.word(annot);
        String pos = AnnotationUtil.pos(annot);
        if (pos == null) 
            return reader.getSynsets(word);

        Synset[] synsets = reader.getSynsets(
                word, PartsOfSpeech.fromPennTag(pos));
        if (synsets == null || synsets.length == 0)
            return reader.getSynsets(word);
        return synsets;
    }
}
