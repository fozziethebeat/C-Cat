/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.PathSimilarity;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.SynsetSimilarity;

import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class BuilderScorer {

    private final Map<String, Set<Synset>> wordParents;

    public BuilderScorer(Map<String, Set<Synset>> wordParents) {
        this.wordParents = wordParents;
    }

    public void scoreAdditions(OntologyReader wordnet) {
        double totalScore = 0;
        int totalAnswered = 0;
        SynsetSimilarity simMethod = new PathSimilarity();
        for (Map.Entry<String, Set<Synset>> entry : wordParents.entrySet()) {
            String word = entry.getKey();
            Set<Synset> parents = entry.getValue();

            // Get the synset for the word that was added.  If it was not in
            // fact added, skip this test instance.
            Synset[] synsets = wordnet.getSynsets(word, PartsOfSpeech.NOUN);
            if (synsets.length == 0)
                continue;
            Synset addedSynset = synsets[0];

            // For each pairwise combination of added parents and real parents,
            // find the pairing that gives the highest similarity and consider
            // this to be the best addition made for the given synset.
            double maxParentSim = 0;
            for (Synset addedParent : addedSynset.getParents()) {
                System.out.printf("%s -> %s\n", word, addedParent.getName());
                for (Synset realParent : parents) {
                    double pathSim = simMethod.similarity(
                            realParent, addedParent);
                    maxParentSim = Math.max(pathSim, maxParentSim);
                }
            }

            // Add the score for this added test word.
            totalAnswered++;
            totalScore += maxParentSim;
        }
        System.out.printf("Final Score: %f Total Answered: %d Average: %f\n",
                          totalScore, totalAnswered, totalScore/totalAnswered);
    }
}
