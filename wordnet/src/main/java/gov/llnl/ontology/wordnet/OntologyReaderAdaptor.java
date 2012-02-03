package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import java.util.Iterator;
import java.util.Set;


/**
 * This {@link OntologyReader} adaptor class allows sub classes to easily
 * override specific functionality of a {@link OntologyReader}.  To adapt the
 * functionality of an existing {@link OntologyReader}, extend this class and
 * provide at least a one argument constructor that takes in an existing {@link
 * OntologyReader} instance.  Then override any desired methods.
 *
 * @author Keith Stevens
 */
public class OntologyReaderAdaptor implements OntologyReader {

    /**
     * The original {@link OntologyReader} that is to be decorated.
     */
    protected final OntologyReader reader;

    /**
     * Constructs a new {@link OntologyReaderAdaptor} over the given {@link
     * OntologyReader}.
     */
    public OntologyReaderAdaptor(OntologyReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheriDoc}
     */
    public Iterator<String> morphy(String form) {
        return reader.morphy(form);
    }

    /**
     * {@inheriDoc}
     */
    public Iterator<String> morphy(String form, PartsOfSpeech pos) {
        return reader.morphy(form, pos);
    }

    /**
     * {@inheriDoc}
     */
    public void removeSynset(Synset synset) {
        reader.removeSynset(synset);
    }

    /**
     * {@inheriDoc}
     */
    public void addSynset(Synset synset) {
        reader.addSynset(synset);
    }

    /**
     * {@inheriDoc}
     */
    public void addSynset(Synset synset, int index) {
        reader.addSynset(synset, index);
    }

    /**
     * {@inheriDoc}
     */
    public void replaceSynset(Synset synset, Synset replacement) {
        reader.replaceSynset(synset, replacement);
    }

    /**
     * {@inheriDoc}
     */
    public Set<String> wordnetTerms() {
        return reader.wordnetTerms();
    }

    /**
     * {@inheriDoc}
     */
    public Set<String> wordnetTerms(PartsOfSpeech pos) {
        return reader.wordnetTerms(pos);
    }

    /**
     * {@inheriDoc}
     */
    public Set<Synset> allSynsets() {
        return reader.allSynsets();
    }

    /**
     * {@inheriDoc}
     */
    public Set<Synset> allSynsets(PartsOfSpeech pos) {
        return reader.allSynsets(pos);
    }

    /**
     * {@inheriDoc}
     */
    public Synset[] getSynsets(String lemma) {
        return reader.getSynsets(lemma);
    }

    /**
     * {@inheriDoc}
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        return reader.getSynsets(lemma, pos);
    }

    /**
     * {@inheritDoc}
     */
    public Synset[] getSynsets(String lemma, PartsOfSpeech pos, boolean useMorphy) {
        return reader.getSynsets(lemma, pos, useMorphy);
    }

    /**
     * {@inheriDoc}
     */
    public Synset getSynset(String fullSynsetName) {
        return reader.getSynset(fullSynsetName);
    }

    /**
     * {@inheriDoc}
     */
    public Synset getSynset(String lemma, PartsOfSpeech pos, int senseNum) {
        return reader.getSynset(lemma, pos, senseNum);
    }

    /**
     * {@inheriDoc}
     */
    public int getMaxDepth(PartsOfSpeech pos) {
        return reader.getMaxDepth(pos);
    }
}
