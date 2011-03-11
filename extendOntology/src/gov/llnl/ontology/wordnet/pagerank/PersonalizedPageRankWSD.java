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

package gov.llnl.ontology.wordnet.pagerank;

import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


/**
 * A main executible class for running Personalized Page Rank Word Sense
 * Disambiguation.  This implementation is based on the following paper:
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">E. Agirre and A Sorora.
 *   "Personalizing PageRank for word sense disambiguation", in <i>Proceedings
 *   of the 12th Conference of the European Chapter of the Association for
 *   Computational Linguisticis</i> 2009.
 * </ul>
 *
 * @author Keith Stevens
 */
public class PersonalizedPageRankWSD {

  public static void main(String[] args) throws IOException {
    // Create the word net reader.
    OntologyReader wordnet = WordNetCorpusReader.initialize(args[0]);

    // Create the list of synsets that should serve as the base graph during
    // Word Sense Dismabiguation.  This requires creating the list of synsets
    // and a mapping from each synset to it's index in the list.
    List<Synset> synsetList = new ArrayList<Synset>();
    Map<Synset, Integer> synsetMap = new HashMap<Synset, Integer>();
    int synsetIndex = 0;
    for (String lemma : wordnet.wordnetTerms()) {
        Synset[] synsets = wordnet.getSynsets(lemma);
      for (int s = 0; s < synsets.length; ++s,++synsetIndex) {
        synsetList.add(synsets[s]);
        synsetMap.put(synsets[s], synsetIndex);
      }
    }

    // Create an artificial synset for each word in the given sentence that
    // should be disambiguated.
    int termIndex = synsetIndex;
    for (String token : args[1].split("\\s+")) {
      // Ignore words without senses in word net.
      Synset[] synsets = wordnet.getSynsets(token);
      if (synsets == null)
        continue;

      // Create a link for each artificial synset to the word's possible senses.
      BaseSynset termSynset = new BaseSynset(Synset.PartsOfSpeech.NOUN);
      for (Synset possibleSense : synsets)
        termSynset.addRelation("related", possibleSense);

      // Add the word to the list of known synsets.
      synsetList.add(termSynset);
      synsetMap.put(termSynset, synsetIndex);
      synsetIndex++;
      System.out.printf("Added %s at index %d\n", token, synsetIndex-1);
    }

    // Place an even random surfer probability on each artificial synset.
    double numTerms = synsetIndex - termIndex;
    SparseDoubleVector sourceWeights = new CompactSparseVector();
    for (int i = termIndex; i < synsetIndex; ++i)
      sourceWeights.set(i, 1d/numTerms);

    // Run the page rank algorithm over the created graph.
    SynsetPagerank.setupTransitionAttributes(synsetList, synsetMap);
    SparseDoubleVector pageRanks = SynsetPagerank.computePageRank(
        synsetList, sourceWeights, .85);

    // For each artificial synset, print out the term and it's highest scoring
    // word sense.
    for (int s = termIndex; s < synsetList.size(); ++s) {
      for (Synset related : synsetList.get(s).getRelations("related")) {
        int index = synsetMap.get(related);
        System.out.printf("%s: %f\n", related.getName(), pageRanks.get(index));
      }
    }
  }
}
