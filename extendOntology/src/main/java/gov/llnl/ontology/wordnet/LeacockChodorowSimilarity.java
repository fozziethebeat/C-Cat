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


/**
 *
 * Implements the Leacock Chodorow Similarity measure.  This measure scores
 * {@link Synset} similarity based on the shortest path connecting the two and
 * the maximum depth of the taxonomy for the particular part of speech.  The
 * score is given as -log(p/2d) where p is the shortest path length and d is the
 * maximum taxonomy depth.  If {@code synset1} and {@code synset2} have
 * different {@link PartsOfSpeech}, -1 is returned.
 *
 * @author Keith Stevens
 */
public class LeacockChodorowSimilarity implements SynsetSimilarity {

    /**
     * The {@link OntologyReader} responsible for accessing internal {@link
     * Synset}s.
     */
    private final OntologyReader wordnet;

    /**
     * Creates an instance of {@link LeacockChodorowSimilarity}.
     */
    public LeacockChodorowSimilarity(OntologyReader reader) {
        this.wordnet = reader;
    }

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        if (synset1.getPartOfSpeech() != synset2.getPartOfSpeech())
            return 0;
        int maxDepth = wordnet.getMaxDepth(synset1.getPartOfSpeech());
        int distance = SynsetRelations.shortestPathDistance(synset1, synset2);
        return (distance >= 0 && distance <= Integer.MAX_VALUE)
            ? -1 * Math.log((distance + 1) / (2d * maxDepth))
            : 0;
    }
}
