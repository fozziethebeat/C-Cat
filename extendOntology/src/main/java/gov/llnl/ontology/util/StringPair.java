

package gov.llnl.ontology.util;

import edu.ucla.sspace.util.Pair;


/**
 * A subclass of {@link Pair} for {@link String}s that allows for arrays of
 * these pairs.
 *
 * @author Keith Stevens
 */
public class StringPair extends Pair<String> implements Comparable {

    /**
     * Constructs a new {@link StringPair}.
     */
    public StringPair(String x, String y) {
        super(x, y);
    }

    /**
     * Compares this {@link StringPair} to another {@link StringPair}.  Ordering
     * is based first on the {@code x} value and then the {@code y} if there is
     * a tie.
     */
    public int compareTo(Object o) {
        StringPair other = (StringPair) o;
        int diff = this.x.compareTo(other.x);
        return (diff == 0) ? this.y.compareTo(other.y) : diff;
    }
}
