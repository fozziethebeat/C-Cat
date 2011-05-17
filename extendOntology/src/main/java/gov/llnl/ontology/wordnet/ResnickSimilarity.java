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

import java.util.List;


/**
 * Implements the Resnick similarity.  This measure scores {@link Synset}s with
 * the information content of their lowest common subsumer.
 *
 * @author Keith Stevens
 */
public class ResnickSimilarity implements SynsetSimilarity {

    /**
     * The {@link InformationContent} responsible for reporting corpus
     * statistics for each {@link Synset}.
     */
    private final InformationContent ic;

    /**
     * Constructs a new {@link ResnickSimilarity}.
     */
    public ResnickSimilarity(InformationContent ic) {
        this.ic = ic;
    }

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        List<Synset> subsumers = SynsetRelations.lowestCommonHypernyms(
                synset1, synset2);
        if (subsumers.size() == 0)
            return 0;
        double bestIC = 0;
        for (Synset subsumer : subsumers)
            bestIC = Math.max(bestIC, ic.informationContent(subsumer));
        return bestIC;
    }
}
