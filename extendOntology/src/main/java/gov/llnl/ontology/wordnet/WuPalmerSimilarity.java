/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;


/**
 * Implements the Wu-Palmer Similarity.  This measure scores {@link Synset}s
 * based on the depth of the two {@link Synset}s in the taxonomy and their
 * lowest common subsumer, i.e. the deepest node in the tree that is a
 * hypernym of both {@link Synset}s.  This lowest common subsumer is not
 * always the same as the shared hypernym that forms the shortest path between
 * the two {@link Synset}s. When there are multiple subsumers, the subsumer
 * with the longest path to the root of the taxonomy is selected.
 *
 * @author Keith Stevens
 */
public class WuPalmerSimilarity implements SynsetSimilarity {

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        Synset subsumer = SynsetRelations.lowestCommonHypernym(
                synset1, synset2);
        if (subsumer == null)
            return 0;
        double depth = subsumer.getMaxDepth() + 1;
        if (subsumer.getPartOfSpeech() == PartsOfSpeech.NOUN)
            depth++;
        double distance1 = SynsetRelations.shortestPathDistance(
                synset1, subsumer);
        double distance2 = SynsetRelations.shortestPathDistance(
                synset2, subsumer);
        distance1 += depth;
        distance2 += depth;
        return (2.0 * depth) / (distance1 + distance2);
    }
}
