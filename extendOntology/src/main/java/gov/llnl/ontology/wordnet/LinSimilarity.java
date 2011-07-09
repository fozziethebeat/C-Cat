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
 * Implements the Lin Similarity.  This measure scores {@link Synset}s based on
 * the information content of their lowest common subsumer and of the two
 * {@link Synset}s.  Formally, this is:
 * 2 * IC({@code lcs}) / (IC({@code * synset1}) + IC({@code synset2}))
 *
 * @author Keith Stevens
 */
public class LinSimilarity implements SynsetSimilarity {

    /**
     * The {@link InformationContent} responsible for reporting corpus
     * statistics for each {@link Synset}.
     */
    private final InformationContent ic;

    /**
     * Computes the {@link ResnickSimilarity} for two {@link Synset}s.
     */
    private final SynsetSimilarity resSim;

    /**
     * Constructs a new {@link LinSimilarity}.
     */
    public LinSimilarity(InformationContent ic) {
        this.ic = ic;
        resSim = new ResnickSimilarity(ic);
    }

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        double ic1 = ic.informationContent(synset1);
        double ic2 = ic.informationContent(synset2);
        if (ic1 == -1 || ic2 == -1)
            return 0;
        double icSubsumer = resSim.similarity(synset1, synset2);
        return (2d * icSubsumer) / (ic1 + ic2);
    }
}
