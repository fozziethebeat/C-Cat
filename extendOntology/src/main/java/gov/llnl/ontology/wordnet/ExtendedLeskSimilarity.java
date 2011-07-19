package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.util.StringUtils;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class ExtendedLeskSimilarity implements SynsetSimilarity {

    /**
     * {@inheritDoc}
     */
    public double similarity(Synset synset1, Synset synset2) {
        Set<Synset> synsets1 = new HashSet<Synset>();
        synsets1.addAll(synset1.getParents());
        synsets1.addAll(synset1.getChildren());
        synsets1.add(synset1);

        Set<Synset> synsets2 = new HashSet<Synset>();
        synsets2.addAll(synset2.getParents());
        synsets2.addAll(synset2.getChildren());
        synsets2.add(synset2);

        double score = 0;
        for (Synset s1 : synsets1) 
            for (Synset s2 : synsets2)
                score += score(s1.getGloss(), s2.getGloss());
        return score;
    }

    private static double score(String gloss1, String gloss2) {
        String[] gTokens1 = gloss1.split("\\s+");
        String[] gTokens2 = gloss2.split("\\s+");
        return StringUtils.tokenOverlapExp(gTokens1, gTokens2);
    }
}
