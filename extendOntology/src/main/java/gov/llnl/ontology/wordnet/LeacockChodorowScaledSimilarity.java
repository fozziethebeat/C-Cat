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
 * Implements a scaled version of the Leacock Chodorow Similarity measure.  This
 * is guaranateed to return a value between 0 and 1.  The normalization factor
 * is unique for each part of speech and WordNet version.  The scaling is done
 * by dividing the raw lch similarity by the maximum similarity for a particular
 * part of speech.  This maximum similarity is defined by the depth of the
 * hierarchy for that part of speech.
 *
 * @author Keith Stevens
 */
public class LeacockChodorowScaledSimilarity extends LeacockChodorowSimilarity {

    /**
     * The {@link OntologyReader} responsible for accessing internal {@link
     * Synset}s.
     */
    private final OntologyReader wordnet;

    /**
     * Creates an instance of {@link LeacockChodorowScaledSimilarity}.
     */
    public LeacockChodorowScaledSimilarity(OntologyReader reader) {
        super(reader);
        this.wordnet = reader;
    }

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        double lchSim = super.similarity(synset1, synset2);
        int maxDepth = wordnet.getMaxDepth(synset1.getPartOfSpeech());
        double maxSim = -1 * Math.log(1/(2d* maxDepth));
        return lchSim / maxSim;
    }
}
