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

import edu.ucla.sspace.util.MultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * This interface represents a single instance of a Synonym Set ({@link Synset}
 * for short) from the WordNet dictionary.  A {@link Synset} is composed of
 * several {@link Lemma}s that have a word sense, or meaning, that is the same.
 * Each {@link Synset} can be connected to other {@link Synset}s through a
 * {@link Relation}.  Different {@link PartsOfSpeech} utilize different sets of
 * relations. 
 *
 * </p>
 * 
 * </p>
 *
 * A {@link Synset} has several key attributes: 
 *
 * <ul>
 * <li> A name that specifies the first lemma for the {@link Synset}, it's
 * {@link PartsOfSpeech}, and the sense number for the first {@link Lemma}, for
 * example "cat.n.01"</li>
 * <li> A dictionary gloss that explicates the concept represented by the
 * {@link Synset}</li>
 * <li> A set of example sentences for the lemmas in the {@link Synset}</li>
 * <li> A set of relations to other {@link Synset}s, which are keyed by a
 * {@link Relation}</li>
 * <li> A set of parent {@link Synset}s.  These are called Hypernyms for nouns
 * and verbs.  For all parts of speech, these are accessed using the {@link
 * Relation#HYPERNYM} relation.  Parent {@link Synset}s are more concepts, for
 * example "building" is a parent concept of "home".  Calling {@link getParents}
 * is equivalent to calling {@link getRelations} with {@link Relation#HYPERNYM}
 * as the relation argument.</li>
 * <li> A set of child {@link Synset}s.  These are called Hyponyms for nouns
 * and Troponyms for verbs.  For all parts of speech, these are accessed using
 * the {@link Relation#HYPONYM} relation.  Child {@link Synset}s are more
 * specific concecpts, for example "cat" is a child concept of "animal".
 * Calling {@link getChildren} is equivalent to calling {@link getRelations}
 * with {@link Relation#HYPONYM} as the relation argument.</li>
 * </ul>
 *
 * </p>
 * All {@link Synset}s are stored in memory and are connected as a graph.  Any
 * traversal operations should not make the assumption that the graph is
 * acyclic.  
 *
 * @author Keith Stevens
 */
public interface Synset {

    /**
     * The set of valid parts of speech encoded by the WordNet dictionary.
     */
    enum PartsOfSpeech {
        NOUN("n"),
        VERB("v"),
        ADJECTIVE("a"),
        ADVERB("r"),
        ADJECTIVE_SAT("s");

        /**
         * The WordNet string given to this {@link Relation}
         */
        private final String label;

        /**
         * Creates a new {@link PartsOfSpeech} with a specified label.
         */
        PartsOfSpeech(String label) {
            this.label = label;
        }

        /**
         * The label or identifier for this {@link PartsOfSpeech}
         */
        public String toString() {
                return label;
        }

        /**
         * Returns the {@link PartsOfSpeech} form of the part of speech tag for
         * the given penn tree bank part of speech tag.
         */
        public static PartsOfSpeech fromPennTag(final String pos) {
            if (pos == null)
                return null;

            if (pos.startsWith("N"))
                return PartsOfSpeech.NOUN;
            if (pos.startsWith("V"))
                return PartsOfSpeech.VERB;
            if (pos.startsWith("J"))
                return PartsOfSpeech.ADJECTIVE;
            if (pos.startsWith("R"))
                return PartsOfSpeech.ADVERB;
            return null;
        }

        /**
         * Returns the {@link PartsOfSpeech} corresponding to one of the symbols
         * used in the WordNet dictionary files.
         */
        public static PartsOfSpeech fromId(final String label) {
            if (label == null)
                return null;

            for (PartsOfSpeech r : PartsOfSpeech.values())
                if (r.label.equals(label))
                    return r;
            return null;
        }
    };

    /**
     * The set of relations that can connect any two {@link Synset}s.  Each
     * relation is attributed with the character symbols used in the WordNet
     * dictionary files to identify instances of each relation.  Most relations
     * have a corresponding reflexive type, such as {@link Relation#HYPERNYM}is
     * the reflexive relation of {@link Relation:HYPONYM}.
     */
    enum Relation {
        /**
         * A parent of relation.  Hypernyms are a generalized, or parent, {@link
         * Synset}s of a given {@link Synset}.
         */
        HYPERNYM("@", "~"),

        /**
         * A parent of relation.  Instance hypernyms are the reflexive type of
         * an Instance Hyponym.  For example, "river" is an Instance Hypernym of
         * "Mississippi River".
         */
        INSTANCE_HYPERNYM("@i", "~i"),

        /**
         * A child of relation.  Hyponyms are subconcepts of a more general
         * concept.
         */
        HYPONYM("~", "@"),

        /**
         * A child of relation.  Instance hyponyms are the real world instances
         * of a generalized concept, for example, "Mississippi River" is an
         * instance hyponym of "river".
         */
        INSTANCE_HYPONYM("~i", "@i"),

        /**
         * An opposite of relation.  Antonyms are concepts that have an opposite
         * meaning, such as "hot" and "cold".  Conceptually, they may be
         * similar, but only differ on a few distinct features.
         */
        ANTONYM("!", "!"),

        /**
         * A part of relation.  Member holonyms are parts of another concept.
         * For example, "Saturn" is a member holonym of "solar sysetm".
         */
        MEMBER_HOLONYM("#m", "%m"),

        /**
         * A part of relation.  Substance holonyms are substances that compose
         * another object.  For example, "water" is a substancec holonym of
         * "ice".
         */
        SUBSTANCE_HOLONYM("#s", "%s"),

        /**
         * A part of relation.  Part holonyms are concepts that are components
         * of another concept.  For example "knob" is a part holonym of "hilt".
         */
        PART_HOLONYM("#p", "%p"),

        /**
         * A part of relation.  This is the reflexive relation for member
         * holonyms.
         */
        MEMBER_MERONYM("%m", "#m"),

        /**
         * A part of relation.  This is the reflexive relation for substance
         * holonyms.
         */
        SUBSTANCE_MERONYM("%s", "#s"),

        /**
         * A part of relation.  This is the reflexive relation for part
         * holonyms.
         */
        PART_MERONYM("%p", "%p"),

        /**
         * A relation signifying that the current {@link Synset} is a required
         * attribute to some other {@link Synset}.   For example, "accurate" is
         * an attribute of "truth".
         */
        ATTRIBUTE("=", null),

        /**
         * A relation for entailments.  A given {@link Synset} has an entailment
         * relation if it entails or implies another {@link Synset}.  For
         * example, "snore" entails "sleep".
         */
        ENTAILMENT("*", null),

        /**
         * A causal relation.  A given {@link Synset} has a cause relation if it
         * is a cause of another {@link Synset}.
         */
        CAUSE(">", null),

        /**
         * 
         */
        ALSO_SEE("^", "^"),

        /**
         *
         */
        VERB_GROUPS("$", null),

        /**
         *
         */
        SIMILAR_TO("&", "&"),

        /**
         *
         */
        DERIVATIONALLY_RELATED_FORMS("+", "+");

        /**
         * The WordNet string given to this {@link Relation}
         */
        private final String label;

        /**
         * The reflexive form of this {@link Relation} if it exists.
         */
        private final String reflexive;

        /**
         * Creates a new {@link Relation} with a specified label.
         */
        Relation(String label, String reflexiveLabel) {
            this.label = label;
            this.reflexive = reflexiveLabel;
        }

        /**
         * Returns the reflexive {@link Relation}.
         */
        public Relation reflexive() {
            return (reflexive == null) ? null : Relation.fromId(reflexive);
        }

        /**
         * The label or identifier for this {@link Relation}
         */
        public String toId() {
            return label;
        }

        public String toString() {
            return label;
        }

        /**
         * Returns the {@link Relation} corresponding to one of the symbols used
         * in the WordNet dictionary files.
         */
        public static Relation fromId(final String label) {
            for (Relation r : Relation.values())
                if (r.label.equals(label))
                    return r;
            return null;
        }
    }

    /**
     * Returns the integer identifier used to initially reference this synset
     * within the WordNet dictionary files.  Any {@link Synset}s that are added
     * to the dictionary during runtime may return an id of 0.  Note that this
     * value is only needed for corpus readers.
     */
    /* package private */ int getId();

    /**
     * Sets the id for a given {@link Synset}.  Only corpus readers need to set
     * this value.
     */
    /* package private */ void setId(int id);

    /* package private */ void addMorphyMapping(String original, String lemma);

    /**
     * Returns a unique string that identifies this {@link Synset}.  This name
     * should be based on one of the {@link Synset}'s lemmas, the part of
     * speech, and the sense number for this {@link Synset}.  For example,
     * "cat.n.01" signifies that the {@link Synset} is a noun and corresponds to
     * the first sense of the "cat" {@link Lemma}.
     */
    String getName();

    /**
     * Returns the first unique string that identifies this {@link Synset} based
     * on it's part of speech, lexicographer file assignment and index in the
     * lexicographer's file.  This is often the same as the lemma key of the
     * {@link Synset}'s first {@link Lemma}.
     */
    String getSenseKey();

    /**
     * Returns a unique string that identifies this {@link Synset} based on it's
     * part of speech, the {@code base} query term, the lexicographer file
     * assignment and index in the lexicographer's file.  This is often the same
     * as the lemma key of the {@link Synset}'s first {@link Lemma}.
     */
    String getSenseKey(String base);

    /**
     * Returns all sense keys associated with this {@link Synset}.
     */
    List<String> getSenseKeys();

    /**
     * Sets the unique sense key string.
     */
    void addSenseKey(String senseKey);

    /**
     * Returns the sense number for this {@link Synset}.  Sense numbers may be
     * modified whenever a {@link Synset} is added, removed, or merged.
     */
    int getSenseNumber();

    /**
     * Sets the sense number for this {@link Synset}.  Sense numbers may be
     * modified whenever a {@link Synset} is added, removed, or merged.
     */
    void setSenseNumber(int senseNumber);

    /**
     * Returns the set of example sentences which explain how this {@link
     * Synset} is used in regular text.
     */
    List<String> getExamples();

    /**
     * Adds an example sentence detailing how this lemmas for this {@link
     * Synset} are used in everday speech.
     */
    void addExample(String example);

    /**
     * Returns the dictionary definition of this {@link Synset}
     */
    String getDefinition();

    /**
     * Sets dictionary definition for this {@link Synset}.
     */
    void setDefinition(String gloss);

    /**
     * Returns the complete gloss, i.e., all definitions and examples.
     */
    String getGloss();

    /**
     * Returns the list of {@link Lemmas} that correspond to this {@link
     * Synset}.
     */
    List<Lemma> getLemmas();

    /**
     * Adds a {@link Lemma} to this {@link Synset}.    {@link Lemma}s may be
     * added in order according to their precedence for the {@link Synset}.
     */
    void addLemma(Lemma lemma);

    /**
     * Returns the frame indices for all known verb frames for this synset.
     */
    int[] getFrameIds();

    /**
     * Returns the lemma indices for all known verb frames for this synset.
     */
    int[] getLemmaIds();

    /**
     * Sets the indices for frames and lemmas for this synset.
     */
    void setFrameInfo(int[] frameIds, int[] lemmaIds);

    /**
     * Returns the {@link PartsOfSpeech} attributed to this {@link Synset}
     */
    PartsOfSpeech getPartOfSpeech();

    /**
     * Returns the set of relation keys that this {@link Synset} has with other
     * {@link Synset}s.  In general these keys will be string versions of {@link
     * Relation}s.
     */
    Set<String> getKnownRelationTypes();

    /**
     * Returns the set of all {@link Synset}s that are connected to this {@link
     * Synset}, regardless of the relation type.
     */
    Collection<Synset> allRelations();

    /**
     * Returns the set of {@link Synset}s that are connected to this {@link
     * Synset} through the specified {@code relation} string.
     */
    Set<Synset> getRelations(String relation);

    /**
     * Returns {@code true} if a new relation is added between {@code this} and
     * {@code related} by {@code relation}.  Returning {@code false} signifies
     * that the relation already exists.
     */
    boolean addRelation(Relation relation, Synset related);

    /**
     * Returns {@code true} if a new relation is added between {@code this} and
     * {@code related} by {@code relation}.  Returning {@code false} signifies
     * that the relation already exists.
     */
    boolean addRelation(String relation, Synset related);

    /**
     * Returns the set of {@link Synset}s that are connected to this {@link
     * Synset} through the specified {@link Relation}.
     */
    Set<Synset> getRelations(Relation relation);

    /**
     * Returns {@code true} if a relation between {@code this} and {@code
     * related} by the link {@code relation} was removed.  Returning {@code
     * false} signifies that there was no link to remove.
     */
    boolean removeRelation(Relation relation, Synset related);

    /**
     * Returns {@code true} if a relation between {@code this} and {@code
     * related} by the link {@code relation} was removed.  Returning {@code
     * false} signifies that there was no link to remove.
     */
    boolean removeRelation(String relation, Synset related);

    /**
     * Returns the total number of known relations for this synset.
     */
    int getNumRelations();

    /**
     * Returns a {@likn RelatedForm} if the provided {@link Synset} has a {@link
     * Lemma} which is a derivationally related form of a {@link Lemma} from
     * this {@link Synset}, or {@code null} if there is no relation.
     */
    RelatedForm getDerivationallyRelatedForm(Synset synset);

    /**
     * Adds a {@link RelatedForm} to this {@link Synset}, signifiying that the
     * two {@link Synset}s share a {@link Lemma} that are closely related.
     */
    void addDerivationallyRelatedForm(Synset related, RelatedForm form);

    /**
     * Returns the {@link Synset}s that are generalized concepts of this {@link
     * Synset}.  This is equivalent to calling {@link getRelations()} with
     * {@link Relation:HYPERNYM}.
     */
    Set<Synset> getParents();

    /**
     * Returns a list of all paths from the current {@link Synset} to the root
     * of the IS-A hierarchy.  Since every {@link Synset} can have multiple
     * parents, there may be multiple paths that have several commond {@link
     * Synset}s.    The parent paths will begin a the {@link Synset} that is
     * furthest from this {@link Synset} and end with this {@link Synset}.
     */
    List<List<Synset>> getParentPaths();

    /**
     * Returns the {@link Synset}s that are more specific concepts of this
     * {@link Synset}.  This is equivalent to calling {@link getRelations()}
     * with {@link Relation:HYPONYM}.
     */
    Set<Synset> getChildren();

    /**
     * Returns the maximum distance between this {@link Synset} and the root
     * {@link Synset}.
     */
    int getMaxDepth();

    /**
     * Returns the minimum distance between this {@link Synset} and the root
     * {@link Synset}.
     */
    int getMinDepth();

    /**
     * Sets {@code attribute} as an object that described this {@link Synset}
     * with more detail.  {@code attributeName} is a label for the provided
     * {@code attribute}.
     */
    void setAttribute(String attributeName, Attribute attribute);

    /**
     * Returns the {@link Attribute} associated with the label {@code
     * attributeName} applied to this {@link Synset}, or {@code null} if there
     * is no associated attribute.
     */
    Attribute getAttribute(String attributeName);

    /**
     * Returns the set of {@link Attribute} keys currently held by this {@link
     * Synset}.
     */
    Set<String> attributeLabels();

    /**
     * Merges this {@link Synset} with the provided {@link Synset}.
     */
    void merge(Synset other);
}
