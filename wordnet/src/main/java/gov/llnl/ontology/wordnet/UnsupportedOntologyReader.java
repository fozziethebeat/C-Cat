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

package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import java.util.Iterator;
import java.util.Set;


/**
 * An {@link OntologyReader} that throws {@link UnsupportedOperationException}
 * for every method call.  This is used primarily for unit testing the the
 * {@link OntologyReaderAdaptor} as it lets unit tests define which methods are
 * expected to be called and which should not be called by a class without too
 * much effort.
 *
 * @author Keith Stevens
 */
public class UnsupportedOntologyReader implements OntologyReader {
    
    /**
     * @throws UnsupportedOperationException
     */
    public Iterator<String> morphy(String form) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Iterator<String> morphy(String form, PartsOfSpeech pos) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void removeSynset(Synset synset) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void addSynset(Synset synset) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void addSynset(Synset synset, int index) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void replaceSynset(Synset synset, Synset replacement) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Set<String> wordnetTerms() {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Set<String> wordnetTerms(PartsOfSpeech pos) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Set<Synset> allSynsets() {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Set<Synset> allSynsets(PartsOfSpeech pos) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Synset[] getSynsets(String lemma) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos, boolean useMorphy) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Synset getSynset(String fullSynsetName) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public Synset getSynset(String lemma, PartsOfSpeech pos, int senseNum) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }

    /**
     * @throws UnsupportedOperationException
     */
    public int getMaxDepth(PartsOfSpeech pos) {
        throw new UnsupportedOperationException("Cannot perform this action");
    }
}
