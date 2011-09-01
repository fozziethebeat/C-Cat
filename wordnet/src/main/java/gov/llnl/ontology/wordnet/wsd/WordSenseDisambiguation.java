/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.wordnet.OntologyReader;

import java.util.List;


/**
 * An interface for any Word Sense Disambiguation algorithm.  Implementations
 * will be given a list of {@link Sentence}s that need to be disambiguated.
 * Each disambiguated word should have it's {@link Annotation} updated with a
 * word sense tag corresponding to some {@link Synset} in WordNet.
 *
 * @author Keith Stevens
 */
public interface WordSenseDisambiguation {

    /**
     * Initializes the {@link WordSenseDisambiguation} algorithm with the given
     * {@link OntologyReader}.  Any other configuration values should be set via
     * the global system properties object.
     */
    void setup(OntologyReader reader);

    /**
     * Returns a new {@link List} of {@Link Sentence}s which have word sense
     * labels for each noun found in {@code sentences}.  
     */
    Sentence disambiguate(Sentence sentences);

    /**
     * Disambiguates the {@link Annotation} at index {@code annotationIndex} in
     * {@code sentence} and returns a new disambiguated {@link Annotation}.
     */
    Annotation disambiguate(Sentence sentence, int annotationIndex);
}
