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
 * A simple struct based implementation of a {@link RelatedForm}.
 *
 * @author Keith Stevens
 */
public class SimpleRelatedForm implements RelatedForm {

    /**
     * The index of the related {@link Lemma} in the source {@link Sysnet}.
     */
    public int sourceIndex;

    /**
     * The index of the related {@link Lemma} in the other {@link Sysnet}.
     */
    public int otherIndex;

    /**
     * Creates a new {@link SimpleRelatedForm} relating the lemma at index
     * {@code sourceIndex} for the current {@link Synset} to the lemma at index
     * {@code otherIndex} of another, related {@link Synset}.
     */
    public SimpleRelatedForm(int sourceIndex, int otherIndex) {
        this.sourceIndex = sourceIndex;
        this.otherIndex = otherIndex;
    }

    /**
     * {@inheritDoc}
     */
    public int sourceIndex() {
        return sourceIndex;
    }

    /**
     * {@inheritDoc}
     */
    public int otherIndex() {
        return otherIndex;
    }
}

