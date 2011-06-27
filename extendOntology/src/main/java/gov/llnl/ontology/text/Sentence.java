

package gov.llnl.ontology.text;

import gov.llnl.ontology.util.ArrayIterator;
import gov.llnl.ontology.util.StringPair;

import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepParentIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;

import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import java.io.Serializable;

import java.util.Iterator;


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
public class Sentence extends Annotation
                      implements Serializable, Iterable<Annotation> {

    /**
     * The start index of this sentence in a text document.
     */
    private final int start;

    /**
     * The end index of this sentence in a text document.
     */
    private final int end;

    /**
     * The array of {@link Annotation}s for each token in the {@link Sentence}.
     */
    private final Annotation[] tokenAnnotations;

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
     * Sets {@code annotation} as the data for the {@code i}th token in the
     * {@link Sentence}.
     */
    public void addAnnotation(int index, Annotation annotation) {
        tokenAnnotations[index] = annotation;
    }

    /**
     * Returns an {@link Iterator} over {@link DependencyTreeNode}s for each
     * token in the {@link Sentence}.
     */
    public DependencyTreeNode[] dependencyParseTree() {
        // Initialize the dependency tree nodes for each token with it's
        // term and part of speech.
        SimpleDependencyTreeNode[] nodes =
            new SimpleDependencyTreeNode[tokenAnnotations.length];
        for (int i = 0; i < nodes.length; ++i)
            nodes[i] = new SimpleDependencyTreeNode(
                    tokenAnnotations[i].get(TextAnnotation.class),
                    tokenAnnotations[i].get(PartOfSpeechAnnotation.class));

        // For each word, add a SimpleDependencyRelation to the tree
        // nodes that records the relation to it's parent.  Parent nodes
        // will always be the head node in the relation.
        for (int i = 0; i < nodes.length; ++i) {
            int parent = tokenAnnotations[i].get(
                    CoNLLDepParentIndexAnnotation.class);
            String relation = tokenAnnotations[i].get(
                    CoNLLDepTypeAnnotation.class);
            if (parent == 0)
                continue;
            nodes[i].addNeighbor(new SimpleDependencyRelation(
                    nodes[parent-1], relation, nodes[i]));
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
                    tokenAnnotations[i].get(TextAnnotation.class),
                    tokenAnnotations[i].get(PartOfSpeechAnnotation.class));
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
     * Returns the {@link IntPair} recording the span of this {@link Sentence}.
     */
    public IntPair span() {
        return new IntPair(start, end);
    }
}
