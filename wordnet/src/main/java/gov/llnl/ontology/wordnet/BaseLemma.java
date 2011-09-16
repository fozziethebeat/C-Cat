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

import java.util.ArrayList;
import java.util.List;


/**
 * A base implementation of a {@link Lemma}.  This implementation allows for
 * easy bean like access to all the requisit data members.
 *
 * @author Keith Stevens.
 */
public class BaseLemma implements Lemma {

    /**
     * The key for this {@link Lemma}
     */
    private String key;

    /**
     * The {@link Synset} for this {@link Lemma}
     */
    private Synset synset;

    /**
     * The name for this {@link Lemma}
     */
    private String lemmaName;

    /**
     * The lexigrapher file name for this {@link Lemma}
     */
    private String lexName; 

    /**
     * The lexicographer file index for this {@link Lemma}
     */
    private int lexNameIndex;

    /**
     * The lexical id for this {@link Lemma}
     */
    private int lexId;

    /**
     * The syntactic marker for for this {@link Lemma}
     */
    private String syntacticMarker;

    /**
     * The valid verb frames for this {@link Lemma}
     */
    private List<String> frameStrings;

    /**
     * Creates a new {@link Lemma}.
     */
    public BaseLemma(Synset synset, String lemmaName, String syntacticMarker) {
        this(synset, lemmaName, "", 0, 0, syntacticMarker);
    }

    /**
     * Creates a new {@link Lemma}.
     */
    public BaseLemma(Synset synset, String lemmaName, String lexName, 
                     int lexNameIndex, int lexId, String syntacticMarker) {
        this.synset = synset;
        this.lemmaName = lemmaName;
        this.lexName = lexName;
        this.lexNameIndex = lexNameIndex;
        this.lexId = lexId;
        this.syntacticMarker = syntacticMarker;
        frameStrings = new ArrayList<String>();
    }

    /**
     * {@inheritDoc}
     */
    public Synset getSynset() {
        return synset;
    }

    /**
     * {@inheritDoc}
     */
    public String getLemmaName() {
        return lemmaName;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getLexicographerName() {
        return lexName;
    }

    /**
     * {@inheritDoc}
     */
    public int getLexNameIndex() {
        return lexNameIndex;
    }

    /**
     * {@inheritDoc}
     */
    public int getLexicalId() {
        return lexId;
    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key for this {@link Lemma}.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Adds a verb frame to this {@link Lemma}.
     */
    public void addFrameString(String frameString) {
        frameStrings.add(frameString);
    }
}
