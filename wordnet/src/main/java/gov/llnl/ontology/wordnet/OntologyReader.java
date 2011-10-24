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

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import java.util.Iterator;
import java.util.Set;


/**
 * An interface for various Ontology Readers.  Given an {@link OntologyReader},
 * one can get morphological simplifications of a term, get {@link Synset}s
 * based on lemmas and parts of speech, add {@link Synset}s, and inspect basic
 * properties of the tree.
 *
 * @author Keith Stevens
 */
public interface OntologyReader {

    /**
     * Returns an {@link Iterator} over the possible morphological variations of
     * the given word {@code form} for all {@link PartsOfSpeech}.  For each part
     * of speech, if there are any known exceptions for the form, they will be
     * returned before the part of speech specific replacement rules.  For
     * example, if "geese" is given, "goose" will be returned first.
     * Afterwords, no other variations would be returned.  If "explodes" is
     * given, the variants would be "explode", "explode", and "explod", based on
     * the rules specified in {@link MORPHOLOGICAL_SUBSTITUTIONS}.
     */
    public Iterator<String> morphy(String form);

    /**
     * Returns an {@link Iterator} over the possible morphological variations of
     * the given word {@code form} for a given {@link PartsOfSpeech}.  If there
     * are any known exceptions for the form, they will be returned before the
     * part of speech specific replacement rules.  For example, if "geese" is
     * given, "goose" will be returned first.  Afterwords, no other variations
     * would be returned.  If "explodes" is given, the variants would be
     * "explode", "explode", and "explod", based on the rules specified in
     * {@link MORPHOLOGICAL_SUBSTITUTIONS}.
     */
    public Iterator<String> morphy(String form, PartsOfSpeech pos);

    /**
     * Removes {@code synset} from the {@link OntologyReader}.  A mapping from
     * each {@link Lemma} linked to by {@code synset} will be removed from
     * {@code synset}.  
     */
    public void removeSynset(Synset synset);

    /**
     * Adds {@code synset} to the {@link OntologyReader}.  A mapping from each
     * {@link Lemma} linked to by {@code synset} will be made to {@code synset}.
     * {@code synset} will be set as the last {@link Synset} for each {@link
     * Lemma} mapping.
     */
    public void addSynset(Synset synset);

    /**
     * Adds {@code synset} to the {@link OntologyReader}.  A mapping from each
     * {@link Lemma} linked to by {@code synset} will be made to {@code synset}.
     * {@code synset} will be set at index {@code index} for each {@link Lemma}
     * mapping, or as the last entry if {@code index} is too large for any
     * particular {@link Lemma} mapping.
     */
    public void addSynset(Synset synset, int index);

    /**
     * Removes the {@link Synset} from the known hierarchy.  All mappings from
     * lemmas to this {@link Synset} will be removed, along with any stored
     * details about this particular {@link Synset}.
     */
    public void replaceSynset(Synset synset, Synset replacement);

    /**
     * Returns a {@link Set} of lemmas that serve as keys in this {@link
     * OntologyReader}.
     */
    public Set<String> wordnetTerms();

    /**
     * Returns a {@link Set} of all {@link Synset}s maintained by this {@link
     * OntologyReader}.
     */
    public Set<Synset> allSynsets();

    /**
     * Returns a {@link Set} of all {@link Synset}s for the given {@link
     * PartsOfSpeech} maintained by this {@link OntologyReader}.
     */
    public Set<Synset> allSynsets(PartsOfSpeech pos);

    /**
     * Returns a {@link Set} of lemmas that the current word net instance is
     * aware of for a particular {@link PartsOfSpeech}.
     */
    public Set<String> wordnetTerms(PartsOfSpeech pos);

    /**
     * Returns all {@link Synset}s that match the given lemma name.
     */
    public Synset[] getSynsets(String lemma);

    /**
     * Returns all {@link Synset}s that match the given lemma name and part of
     * speech.  If there is no known mapping for the given word, the {@link
     * Synset}s for all it's part of speech specific morphological variations
     * will be returned.
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos);

    /**
     * Returns all {@link Synset}s that match the given lemma name and part of
     * speech.  If there is no known mapping for the given word and {@code
     * useMorphy} is true, the {@link
     * Synset}s for all it's part of speech specific morphological variations
     * will be returned.
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos, boolean useMorphy);

    /**
     * Returns the {@link Synset} specified by the full synset name.  The name
     * should be of the following format:
     *   lemma.pos.senseNum
     */
    public Synset getSynset(String fullSynsetName);

    /**
     * Returns the single {@link Synset} specified by the given lemma name, part
     * of speech tag, and sense number.  Sense numbers start at 1.
     */
    public Synset getSynset(String lemma, PartsOfSpeech pos, int senseNum);

    /**
     * Returns the maximum depth of any {@link Synset} chain in this {@link
     * OntologyReader}.
     */
    public int getMaxDepth(PartsOfSpeech pos);
}
