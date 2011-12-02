package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.text.Annotation;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.SimpleAnnotation;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import java.util.Set;


/**
 * @author Keith Stevens
 */
public class MostFrequentSenseDisambiguation implements WordSenseDisambiguation {

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
            Annotation result = new SimpleAnnotation("");
            result.setSpan(annot.start(), annot.end());
            if (focusIndices == null || 
                focusIndices.isEmpty() ||
                focusIndices.contains(i)) {
                Synset[] senses = getSynsets(reader, annot);
                String word = annot.word();
                if (senses != null && senses.length > 0)
                    result.setSense(senses[0].getSenseKey(word));
            }
            resultSent.addAnnotation(i++, result);
        }

        return resultSent;
    }

    public String toString() {
        return "fsd";
    }

    /**
     * Returns all of the {@link Synset}s found given the word and part of
     * speech information, if present, in {@code annot}.  If the part of speech
     * is available, but provides no synsets, all possible synsets are returned
     * for the word, under the assumption that the tag may be incorrect.
     */
    protected Synset[] getSynsets(OntologyReader reader, Annotation annot) {
        String word = annot.word();
        String pos = annot.pos();
        if (pos == null) 
            return reader.getSynsets(word);

        Synset[] synsets = reader.getSynsets(
                word, PartsOfSpeech.fromPennTag(pos));
        if (synsets == null || synsets.length == 0)
            return reader.getSynsets(word);
        return synsets;
    }
}
