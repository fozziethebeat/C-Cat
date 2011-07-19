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
import gov.llnl.ontology.util.ExtendedList;
import gov.llnl.ontology.util.ExtendedMap;
import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.SynsetPagerank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


/**
 * A {@link WordSenseDisambiguation} implementation for Personalized Page Rank Word Sense
 * Disambiguation.  This implementation is based on the following paper:
 *
 * <ul>
 *
 *  <li style="font-family:Garamond, Georgia, serif">E. Agirre and A Sorora.
 *  "Personalizing PageRank for word sense disambiguation", in <i>Proceedings
 *  of the 12th Conference of the European Chapter of the Association for
 *  Computational Linguisticis</i> 2009.
 * </ul>
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @author Keith Stevens
 */
public class PersonalizedPageRankWSD extends SlidingWindowDisambiguation {

    public static final String LINK = "related";
    private OntologyReader wordnet;
    
    private Map<Synset, Integer> synsetMap;

    private List<Synset> synsetList;

    public void setup(OntologyReader wordnet) {
        this.wordnet = wordnet;

        // Create the list of synsets that should serve as the base graph during
        // Word Sense Dismabiguation.  This requires creating the list of
        // synsets and a mapping from each synset to it's index in the list.
        synsetMap = Maps.newHashMap();
        synsetList = Lists.newArrayList();
        int synsetIndex = 0;
        for (String lemma : wordnet.wordnetTerms()) {
            Synset[] synsets = wordnet.getSynsets(lemma);
            for (int s = 0; s < synsets.length; ++s,++synsetIndex)
                if (!synsetMap.containsKey(synsets[s])) {
                    synsetMap.put(synsets[s], synsetIndex);
                    synsetList.add(synsets[s]);
                }
        }
        SynsetPagerank.setupTransitionAttributes(synsetList, synsetMap);
    }

    protected void processContext(Annotation focus,
                                  Queue<Annotation> prevWords,
                                  Queue<Annotation> nextWords) {
        Map<Synset, Integer> localMap = new ExtendedMap<Synset, Integer>(
                synsetMap);
        List<Synset> localList = new ExtendedList<Synset>(synsetList);

        // Create an artificial synset for each word in the given sentence that
        // should be disambiguated. Start by adding the focus term itself.  If
        // that cannot be added, return and skip disambiguating this context.
        if (addTermNode(localList, localMap, focus) == 0)
            return;

        // Add the artificial synsets for each of the context words.
        for (Annotation prev : prevWords)
            addTermNode(localList, localMap, prev);
        for (Annotation next: nextWords)
            addTermNode(localList, localMap, next);

        // Place an even random surfer probability on each artificial synset.
        double numTerms = localList.size() - synsetList.size();
        SparseDoubleVector sourceWeights = new CompactSparseVector();
        for (int i = synsetList.size(); i < localList.size(); ++i)
            sourceWeights.set(i, 1d/numTerms);

        // Run the page rank algorithm over the created graph.
        SynsetPagerank.setupTransitionAttributes(localList, localMap);
        SparseDoubleVector pageRanks = SynsetPagerank.computePageRank(
                localList, sourceWeights, .85);

        // Determine the best sense for the focus word. 
        int focusIndex = synsetList.size();
        Synset maxSynset = null;
        double maxRank = 0;
        for (Synset related : localList.get(focusIndex).getRelations(LINK)) {
            int index = localMap.get(related);
            double rank = pageRanks.get(index);
            if (maxRank <= rank) {
                maxRank = rank;
                maxSynset = related;
            }
        }

        // Store the word sense annotation.
        AnnotationUtil.setWordSense(focus, maxSynset.getName());
    }

    /**
     * Adds a new artificial {@link Synset} corresponding to the word in {@code
     * word}.  This new {@link Synset} will be connected to each of it's
     * possible word senses via a fake "related" link.  Returns 1 if the word
     * was added to {@code localMap} and 0 otherwise.
     */
    private int addTermNode(List<Synset> synsetList,
                            Map<Synset, Integer> localMap, 
                            Annotation word) {
        String token = AnnotationUtil.word(word);

        // Ignore words without senses in word net.
        Synset[] synsets = wordnet.getSynsets(token, PartsOfSpeech.NOUN);
        if (synsets == null)
            return 0;

        // Create a link for each artificial synset to the word's possible
        // senses.
        BaseSynset termSynset = new BaseSynset(PartsOfSpeech.NOUN);
        for (Synset possibleSense : synsets)
            termSynset.addRelation(LINK, possibleSense);

        // Add the word to the list of known synsets.
        synsetList.add(termSynset);
        localMap.put(termSynset, localMap.size());
        return 1;
    }
}
