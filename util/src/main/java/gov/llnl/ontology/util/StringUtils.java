package gov.llnl.ontology.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A collection of string utilities.
 *
 * @author Keith Stevens
 */
public class StringUtils {

    /**
     * Returns the number of tokens that overlap between {@code tokens1} and
     * {@code tokens2}
     */
    public static int tokenOverlap(String[] tokens1, String[] tokens2) {
        Set<String> tokenSet2 = new HashSet<String>(Arrays.asList(tokens2));

        int score = 0;
        for (String word : tokens1)
            if (tokenSet2.contains(word))
                score++;
        return score;
    }

    /**
     * Returns the number of tokens that overlap between {@code tokens1} and
     * {@code tokens2}, but whenever a sequence of tokens overlap, their score
     * is the sequence length to the power of two.
     */
    public static int tokenOverlapExp(String[] tokens1, String[] tokens2) {
        int index1 = 0;
        int index2 = 0;

        int score = 0;
        for (int i = 0; i < tokens1.length; ++i)
            for (int j = 0; j < tokens2.length; ++j)
                if (tokens1[i].equals(tokens2[j]))
                    score += Math.pow(findOverlap(tokens1, i, tokens2, j), 2);
        return score;
    }

    /**
     * Determines the length of a sequence overlap in {@code tokens1}
     */
    public static int findOverlap(String[] tokens1, int i, 
                                  String[] tokens2, int j) {
        int overlap = 1;
        while (++i < tokens1.length &&
               ++j < tokens2.length &&
               tokens1[i].equals(tokens2[j]))
            overlap++;
        return overlap;
    }
}
