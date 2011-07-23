package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class GenericMockReader extends OntologyReaderAdaptor {

    private Map<String, Synset> synsetMap;

    private String[][] synsetData;

    public GenericMockReader(String[][] synsetData) {
        super(new UnsupportedOntologyReader());
        synsetMap = Maps.newHashMap();
        for (String[] synsetAndGloss : synsetData)
            synsetMap.put(synsetAndGloss[0], makeSynset(synsetAndGloss));
    }

    public Set<String> wordnetTerms() {
        return synsetMap.keySet();
    }

    public Synset[] getSynsets(String lemma) {
        List<Synset> found = Lists.newArrayList();
        for (Map.Entry<String, Synset> e : synsetMap.entrySet())
            if (e.getKey().startsWith(lemma))
                found.add(e.getValue());
        return found.toArray(new Synset[0]);
    }

    public Synset[] getSynsets(String lemma, PartsOfSpeech pos) {
        return getSynsets(lemma + "." + pos.toString());
    }

    public Synset getSynset(String lemma) {
        for (Map.Entry<String, Synset> e : synsetMap.entrySet())
            if (e.getKey().equals(lemma))
                return e.getValue();
        return null;
    }

    private Synset makeSynset(String[] synsetAndGloss) {
        Synset synset = new BaseSynset(0, PartsOfSpeech.NOUN);
        String[] namePosId = synsetAndGloss[0].split("\\.");
        synset.setSenseNumber(Integer.parseInt(namePosId[2]));
        synset.addLemma(new BaseLemma
                (synset, namePosId[0], null, 0, 0, null));
        synset.setDefinition(synsetAndGloss[1]);
        return synset;
    }
}
