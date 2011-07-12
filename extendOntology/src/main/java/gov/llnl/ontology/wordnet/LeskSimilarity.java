package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.util.StringUtils;


/**
 * @author Keith Stevens
 */
public class LeskSimilarity implements SynsetSimilarity {

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        // Get the glosses for both sysnets and tokenize them.  Then store the
        // second gloss in a hashset for easy lookup of words.
        String[] gloss1 = synset1.getGloss().split("\\s+");
        String[] gloss2 = synset2.getGloss().split("\\s+");
        return StringUtils.tokenOverlap(gloss1, gloss2);
    }
}
