package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import edu.stanford.nlp.pipeline.Annotation;

import java.util.Set;


/**
 * @author Keith Stevens
 */
public class FirstSenseDisambiguation implements WordSenseDisambiguation {

    private OntologyReader reader;

    /**
     * {@inheritDoc}
     */
    public void setup(OntologyReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheritDoc}
     */
    public Sentence disambiguate(Sentence sentence) {
        return disambiguate(sentence, null);
    }

    /**
     * {@inheritDoc}
     */
    public Sentence disambiguate(Sentence sentence, Set<Integer> focusIndices) {
        Sentence resultSent = new Sentence(
                sentence.start(), sentence.end(), sentence.numTokens());

        int i = 0;
        for (Annotation annot : sentence) {
            Annotation result = new Annotation();
            AnnotationUtil.setSpan(result, AnnotationUtil.span(annot));
            if (focusIndices == null || 
                focusIndices.isEmpty() ||
                focusIndices.contains(i)) {
                Synset[] senses = getSynsets(annot);
                String word = AnnotationUtil.word(annot);
                if (senses != null && senses.length > 0)
                    AnnotationUtil.setWordSense(
                            result, senses[0].getSenseKey(word));
            }
            resultSent.addAnnotation(i++, result);
        }

        return resultSent;
    }

    private Synset[] getSynsets(Annotation annot) {
        String word = AnnotationUtil.word(annot);
        String pos = AnnotationUtil.pos(annot);
        return (pos == null)
            ? reader.getSynsets(word)
            : reader.getSynsets(word, PartsOfSpeech.fromPennTag(pos));
    }
}
