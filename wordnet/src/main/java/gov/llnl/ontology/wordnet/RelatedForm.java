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
 * Represents an informal relation between two {@link Lemma}s.  Two {@link
 * Lemma}s can be connected by two {@link Sysnet}s if the {@link Lemma}s are
 * derivationally related to each other in both meaning and form. 
 *
 * @author Keith Stevens
 */
public interface RelatedForm {

    /**
     * Returns the index of the {@link Lemma} from this {@link Sysnet} that is
     * related to another {@link Lemma}.
     */
    int sourceIndex();

    /**
     * Returns the index of the {@link Lemma} from a related {@link Synset} that
     * is related to a {@link Lemma} in this {@link Synset}.
     */
    int otherIndex();
}
