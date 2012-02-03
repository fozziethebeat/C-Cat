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

package gov.llnl.ontology.text;

import gov.llnl.ontology.util.ArrayIterator;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Lists;

import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.io.Serializable;

import java.util.Iterator;
import java.util.List;


/**
 * An {@link Annotation} specificly designed for sentences.  It represents a
 * sentence as an array of other {@link Annotation}s, each of which represents
 * an {@link Annotation} for each word in the sentence.  These word level {@link
 * Annotation}s can contain values for dependency parsing features, token
 * features, and part of speech tags.  If these features are available, the
 * {@link Sentence} can be viewed as a series {@link DependencyTreeNode}s or a
 * series of {@link StringPair}s.
 *
 * @author Keith Stevens
 */
public class Sentence implements Serializable, Iterable<Annotation> {

    // Git Commit hash id on 7/23
    static final long serialVersionId = 0x331cf4a;

    /**
     * "|"s and ";" are used to separate sentence level and token level
     * information.  Since a token may contain any printable character, we need
     * a more complicated regular expression to safely split the serialized form
     * of a {@link Sentence}.  These two expressions split whenever a "|" and
     * ";" exist outside of quotes.
     */
    private static final String TOK_SEPARATOR = "\\|";

    private static final String ANNOT_SEPARATOR = ";";

    /**
     * The start index of this sentence in a text document.
     */
    private int start = -1;

    /**
     * The end index of this sentence in a text document.
     */
    private int end = -1;

    /**
     * The array of {@link Annotation}s for each token in the {@link Sentence}.
     */
    private Annotation[] tokenAnnotations = new Annotation[0];

    /**
     * The document text associated with this sentence.  This data will not be
     * saved during serialization.
     */
    private transient String text;

    /**
     * A private no-arg constructor for deserialization with Gson.
     */
    private Sentence() {
    }

    /**
     * Creates a new {@link Sentence} with the following initial attributes.
     * {@code numTokens} dictates how many tokens were found in this sentence
     * and cannot be modified.
     */
    public Sentence(int start, int end, int numTokens) {
        this.start = start;
        this.end = end;
        this.tokenAnnotations = new Annotation[numTokens];
    }

    /**
     * Sets the raw document text associated with this {@link Sentence}.  This
     * must be called before calling {@link #sentenceText}.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Returns the raw text from the original document text for just the
     * characters spanned by this {@link Sentence}.
     */
    public String sentenceText() {
        return (text == null) ? null : text.substring(start, end);
    }

    /**
     * Sets {@code annotation} as the data for the {@code i}th token in the
     * {@link Sentence}.
     */
    public void addAnnotation(int index, Annotation annotation) {
        tokenAnnotations[index] = annotation;
    }

    /**
     * Returns the {@link Annotation} stored at {@code index}
     */
    public Annotation getAnnotation(int index) {
        return tokenAnnotations[index];
    }

    /**
     * Returns an {@link Iterator} over {@link DependencyTreeNode}s for each
     * token in the {@link Sentence}.
     */
    public DependencyTreeNode[] dependencyParseTree() {
        if (tokenAnnotations.length == 0 ||
            !tokenAnnotations[0].hasDependencyParent())
            return new DependencyTreeNode[0];

        // Initialize the dependency tree nodes for each token with it's
        // term and part of speech.
        SimpleDependencyTreeNode[] nodes =
            new SimpleDependencyTreeNode[tokenAnnotations.length];
        for (int i = 0; i < nodes.length; ++i)
            nodes[i] = new SimpleDependencyTreeNode(
                    tokenAnnotations[i].word(),
                    tokenAnnotations[i].pos(),
                    i);

        // For each word, add a SimpleDependencyRelation to the tree
        // nodes that records the relation to it's parent.  Parent nodes
        // will always be the head node in the relation.
        for (int i = 0; i < nodes.length; ++i) {
            int parent = tokenAnnotations[i].dependencyParent();
            String relation = tokenAnnotations[i].dependencyRelation();
            if (parent == 0)
                continue;
            DependencyRelation r = new SimpleDependencyRelation(
                    nodes[parent-1], relation, nodes[i]);
            nodes[i].addNeighbor(r);
            nodes[parent-1].addNeighbor(r);
        }

        return nodes;
    }

    /**
     * Returns an {@link Iterator} over {@link StringPair}s for each
     * token in the {@link Sentence}.
     */
    public StringPair[] taggedTokens() {
        StringPair[] taggedTokens = new StringPair[tokenAnnotations.length];
        for (int i = 0; i < taggedTokens.length; ++i) 
            taggedTokens[i] = new StringPair(
                    tokenAnnotations[i].word(), tokenAnnotations[i].pos());
        return taggedTokens;
    }

    /**
     * Returns an {@link Iterator} over {@link Annotation}s for each
     * token in the {@link Sentence}.
     */
    public Iterator<Annotation> iterator() {
        return new ArrayIterator<Annotation>(tokenAnnotations);
    }

    /**
     * Returns the start index of this sentence.
     */
    public int start() {
        return start;
    }

    /**
     * Returns the end index of this sentence.
     */
    public int end() {
        return end;
    }

    public int numTokens() {
        return tokenAnnotations.length;
    }

    /**
     * Reads a list of {@link Sentence}s from the serialzied form of the {@link
     * Sentence} meta-data and the {@link Annotation} meta-data for each token
     * in the sentence.  This will leave {@code text} unset.  Only output from
     * {@link #writeSentence} is valid when calling {@link #readSentences}.
     */
    public static List<Sentence> readSentences(String sentenceText, 
                                               String tokenText) {
        // Return an empty list if no valid text was provided.
        if (sentenceText == null || tokenText == null)
            return Lists.newArrayList();

        // First parse the meta data for the sentences themselves.
        List<Sentence> sentences = Lists.newArrayList();
        for (String sentence : sentenceText.split("\\|")) {
            String[] parts = sentence.split("\\s+");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            int numTokens = Integer.parseInt(parts[2]);
            sentences.add(new Sentence(start, end, numTokens));
        }
 
        // Next parse the meta-data for each token in each sentence and assign
        // that token's Annotation to the correct sentence.
        for (String token : tokenText.split(TOK_SEPARATOR)) {
            // Split up the annotation data.
            String[] parts = token.split(ANNOT_SEPARATOR);
            String[] sentTokIndex = parts[0].split("\\s+");

            // Determine the sentence index and the token index.
            int sentIndex = Integer.parseInt(sentTokIndex[0]);
            int tokIndex = Integer.parseInt(sentTokIndex[1]);

            // For each annotation data pair, determine which type of data it is
            // and add the cleaned value to annotation.  When initially creating
            // the annotation, give it empty strings for default values so that
            // there are no null values.
            Annotation annotation = new StanfordAnnotation("", "");
            for (int i = 1; i < parts.length; ++i) {
                // Always split on the first ":" and only that ":" so that if a
                // ":" appears in the word or text, it won't cause any problems
                // and will be treated as the literatl token.
                String[] keyValue = parts[i].split(":", 2);

                if (keyValue[0].equals("word")) {
                    keyValue[1] = keyValue[1].replaceAll("&quot,", "\"");
                    keyValue[1] = keyValue[1].replaceAll("&pipe,", "|");
                    keyValue[1] = keyValue[1].replaceAll("&semi,", ";");
                    annotation.setWord(keyValue[1]);
                }
                if (keyValue[0].equals("pos"))
                    annotation.setPos(keyValue[1]);
                if (keyValue[0].equals("sense"))
                    annotation.setSense(keyValue[1]);
                if (keyValue[0].equals("dep-rel"))
                    annotation.setDependencyRelation(keyValue[1]);
                if (keyValue[0].equals("dep-index"))
                    annotation.setDependencyParent(Integer.parseInt(keyValue[1]));
                if (keyValue[0].equals("span")) {
                    String[] startEnd = keyValue[1].split(",");
                    annotation.setSpan(Integer.parseInt(startEnd[0]),
                                       Integer.parseInt(startEnd[1]));
                }
            }
            
            // Add annotation to the correct sentence at the correct index.
            sentences.get(sentIndex).addAnnotation(tokIndex, annotation);
        }
        return sentences;
    }

    public static StringPair writeSentences(List<Sentence> sentences) {
        // Currently both Java Serialization and Gson serialization mechanisms
        // are failing horribly.  So, sentences are not being annotated by hand.
        // All of the sentence meta data will be collected into one string that
        // is delimiited by "|"s and will contain the start index, end index,
        // and number of tokens.  All of the token meta data will be delimited
        // by "|"s and will contain a series of annotType:value pairs delimited
        // by ";"s with the first value being a sentence identifier.
        StringBuilder sentenceAnnot = new StringBuilder();
        StringBuilder tokenAnnot = new StringBuilder();
        int s = 0;
        for (Sentence sentence : sentences) {
            sentenceAnnot.append(sentence.start()).append(" ");
            sentenceAnnot.append(sentence.end()).append(" ");
            sentenceAnnot.append(sentence.numTokens());
            sentenceAnnot.append("|");

            int a = 0;
            for (Annotation token : sentence) {
                tokenAnnot.append(s).append(" ").append(a++).append(";");

                // Handle the pos.
                if (token.hasPos())
                    tokenAnnot.append("pos").append(":").append(token.pos()).append(";");

                // Handle the word.
                if (token.hasWord()) {
                    String value = token.word().replaceAll("\"", "&quot,")
                                               .replaceAll("\\|", "&pipe,")
                                               .replaceAll(";", "&semi,");
                    tokenAnnot.append("word").append(":").append(value).append(";");
                }

                // Handle the word sense.
                if (token.hasSense())
                    tokenAnnot.append("sense").append(":").append(token.sense()).append(";");

                // Handle the dependency relation.
                if (token.hasDependencyRelation())
                    tokenAnnot.append("dep-rel").append(":").append(token.dependencyRelation()).append(";");

                // Handle the dependency relation.
                if (token.hasDependencyParent())
                    tokenAnnot.append("dep-index").append(":").append(token.dependencyParent()).append(";");

                // Handle the word span.
                if (token.hasSpan())
                    tokenAnnot.append("span").append(":")
                              .append(token.start()).append(",")
                              .append(token.end()).append(";");

                tokenAnnot.append("|");
            }
            s++;
        }
        return new StringPair(sentenceAnnot.toString(), tokenAnnot.toString());
    }
}
