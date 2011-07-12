package gov.llnl.ontology.wordnet.wsd;

import gov.llnl.ontology.wordnet.ExtendedLeskSimilarity;
import gov.llnl.ontology.wordnet.OntologyReader;


/**
 * A {@link WordSenseDisambiguation} implementation using the {@link
 * ExtendedLeskSimilarity} measure.
 *
 * </p>
 *
 * This class <b>is</b> thread safe.
 *
 * @author Keith Stevens
 */
public class ExtendedLeskWordSenseDisambiguation 
        extends LeskWordSenseDisambiguation {

    public void setup(OntologyReader reader) {
        this.reader = reader;
        this.sim = new ExtendedLeskSimilarity();
    }
}
