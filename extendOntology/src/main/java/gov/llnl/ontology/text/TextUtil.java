

package gov.llnl.ontology.text;


/**
 * @author Keith Stevens
 */
public class TextUtil {

    public static String cleanTerm(String term) {
        while (term.length() > 0 && term.startsWith("-"))
                term = term.substring(1, term.length());
        while (term.length() > 0 && term.endsWith("-"))
            term = term.substring(0, term.length()-1);
        term = term.replaceAll("\"", "");
        term = term.replaceAll("\'", "");
        term = term.replaceAll("\\[", "");
        term = term.replaceAll("\\]", "");
        term = term.replaceAll("\\?", "");
        term = term.replaceAll("\\*", "");
        term = term.replaceAll("\\(", "");
        term = term.replaceAll("\\)", "");
        term = term.replaceAll("\\^", "");
        term = term.replaceAll("\\+", "");
        term = term.replaceAll("//", "");
        term = term.replaceAll(";", "");
        term = term.replaceAll("%", "");
        term = term.replaceAll(",", "");
        term = term.replaceAll("!", "");
        return term.trim();
    }
}
