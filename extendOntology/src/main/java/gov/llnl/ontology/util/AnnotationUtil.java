package gov.llnl.ontology.util;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SpanAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.WordSenseAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;


/**
 * @author Keith Stevens
 */
public class AnnotationUtil {

    public static String pos(Annotation annot) {
        return annot.get(PartOfSpeechAnnotation.class);
    }

    public static PartsOfSpeech synsetPos(Annotation annot) {
        String pos = pos(annot);
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

    public static void setPos(Annotation annot, String pos) {
        annot.set(PartOfSpeechAnnotation.class, pos);
    }

    public static String word(Annotation annot) {
        return annot.get(TextAnnotation.class);
    }

    public static void setWord(Annotation annot, String word) {
        annot.set(TextAnnotation.class, word);
    }

    public static String wordSense(Annotation annot) {
        return annot.get(WordSenseAnnotation.class);
    }

    public static void setWordSense(Annotation annot, String sense) {
        annot.set(WordSenseAnnotation.class, sense);
    }

    public static IntPair span(Annotation annot) {
        return annot.get(SpanAnnotation.class);
    }

    public static void setSpan(Annotation annot, IntPair span) {
        annot.set(SpanAnnotation.class, span);
    }
}

