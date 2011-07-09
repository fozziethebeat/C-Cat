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
 * This interface represents a single term that maps to a {@link Synset} in the
 * WordNet dictionary.
 *
 * </p>
 *
 * Each {@link Lemma} has several attributes:
 * <ul>
 * <li> A key that uniquely identifies this {@link Lemma} against other {@link
 * Lemma}s for the same {@link Synset}. </li>
 * <li> The {@link Synset} for this {@link Lemma} </li>
 * <li> The name of this {@link Lemma} </li>
 * <li> The lexigicgrapher file name that is attributed to this {@link Lemma}
 * </li> <li> The lexigicgrapher file name index that is attributed to this
 * {@link Lemma} </li>
 * <li> The lexigicgrapher index that is attributed to this {@link Lemma} </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public interface Lemma {

    /**
     * Returns the key for this {@link Lemma}.
     */
    String getKey();

    /**
     * Returns the {@link Synset} for this {@link Lemma}.
     */
    Synset getSynset();

    /**
     * Returns the name of this {@link Lemma}
     */
    String getLemmaName();
    
    /**
     * Returns the lexicographer file name for this {@link lemma}.
     */
    String getLexicographerName();

    /**
     * Returns the index corresponding to the lexicographer file name for this
     * {@link Lemma}.
     */
    int getLexNameIndex();

    /**
     * Returns the lexical id for this {@link Lemma}
     */
    int getLexicalId();
}
