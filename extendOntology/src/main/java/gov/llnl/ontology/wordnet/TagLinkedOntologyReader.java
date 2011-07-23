package gov.llnl.ontology.wordnet;

import java.util.Map;


/**
 * This {@link OntologyReader} provides a mapping between a set of document tags
 * and a set of {@link Sysnet} names.  It maps maps tags to {@link Sysnet}s via
 * the {@link #getSynset} method which will interpret the given name as a tag
 * and return the {@link Synset} associated with that tag name.  All other
 * accessor methods to {@link Sysnet}s are unchanged and will not do the
 * mapping.
 *
 * @author Keith Stevens
 */
public class TagLinkedOntologyReader extends OntologyReaderAdaptor {

    /**
     * A mapping between tag names and {@link Sysnet} names.
     */
    private final Map<String, String> tagMap;

    /**
     * Conctructs a new {@link TagLinkedOntologyReader} that decorates the given
     * {@link OntologyReader} with a mapping from tags to {@link Synset} names
     * as detailed by {@code tagMap}.
     *
     * @param reader The {@link OntologyReader} to adapt
     * @param tagMap The mapping from tag names to {@link Sysnet} names.
     */
    public TagLinkedOntologyReader(OntologyReader reader,
                                   Map<String, String> tagMap) {
        super(reader);
        this.tagMap = tagMap;
    }

    /**
     * Returns the {@link Synset} linked to by the given {@code tagName}.  If
     * {@code tagName} does not correspond with any {@link Synset} name, this
     * returns {@code null}.
     */
    public Synset getSynset(String tagName) {
        String synsetName = tagMap.get(tagName);
        if (synsetName == null)
            return reader.getSynset(tagName);
        return reader.getSynset(synsetName);
    }
}
