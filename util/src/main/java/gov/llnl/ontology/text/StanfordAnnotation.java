package gov.llnl.ontology.text;

import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepParentIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SpanAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.WordSenseAnnotation;
import edu.stanford.nlp.util.IntPair;


/**
 * @author Keith Stevens
 */
public class StanfordAnnotation extends edu.stanford.nlp.pipeline.Annotation 
                                implements Annotation {

    public StanfordAnnotation() {
    }

    public StanfordAnnotation(StanfordAnnotation other) {
        super(other);
    }

    public StanfordAnnotation(String word) {
        setWord(word);
    }

    public StanfordAnnotation(String word, String pos) {
        setWord(word);
        setPos(pos);
    }

    public StanfordAnnotation(String word, String pos, int start, int end) {
        setWord(word);
        setPos(pos);
        setSpan(start, end);
    }

    public boolean hasDependencyParent() {
        return has(CoNLLDepParentIndexAnnotation.class);
    }

    public int dependencyParent() {
        return get(CoNLLDepParentIndexAnnotation.class);
    }

    public void setDependencyParent(int parent) {
        set(CoNLLDepParentIndexAnnotation.class, parent);
    }

    public boolean hasDependencyRelation() {
        return has(CoNLLDepTypeAnnotation.class);
    }

    public String dependencyRelation() {
        return get(CoNLLDepTypeAnnotation.class);
    }

    public void setDependencyRelation(String relation) {
        set(CoNLLDepTypeAnnotation.class, relation);
    }

    public boolean hasWord() {
        return has(TextAnnotation.class);
    }

    public String word() {
        return get(TextAnnotation.class);
    }

    public void setWord(String word) {
        set(TextAnnotation.class, word);
    }

    public boolean hasLemma() {
        return has(StemAnnotation.class);
    }

    public String lemma() {
        return get(StemAnnotation.class);
    }

    public void setLemma(String lemma) {
        set(StemAnnotation.class, lemma);
    }

    public boolean hasPos() {
        return has(PartOfSpeechAnnotation.class);
    }

    public String pos() {
        return get(PartOfSpeechAnnotation.class);
    }

    public void setPos(String pos) {
        set(PartOfSpeechAnnotation.class, pos);
    }

    public boolean hasSense() {
        return has(WordSenseAnnotation.class);
    }

    public String sense() {
        return get(WordSenseAnnotation.class);
    }

    public void setSense(String sense) {
        set(WordSenseAnnotation.class, sense);
    }

    public boolean hasSpan() {
        return has(SpanAnnotation.class);
    }

    public int end() {
        IntPair span = get(SpanAnnotation.class);
        return span.getTarget();
    }

    public int start() {
        IntPair span = get(SpanAnnotation.class);
        return span.getSource();
    }

    public void setSpan(int start, int end) {
        set(SpanAnnotation.class, new IntPair(start, end));
    }
}
