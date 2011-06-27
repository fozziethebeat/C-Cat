

package gov.llnl.ontology.util;

import java.util.Iterator;


/**
 * An {@link Iterator} for arrays of objects.
 *
 * @author Keith Stevens
 */
public class ArrayIterator<T> implements Iterator<T> {
    
    /**
     * The original array of objects.
     */
    private final T[] values;

    /**
     * The index of the next value to return.
     */
    private int index;

    /**
     * Creates a new {@link ArrayIterator}
     */
    public ArrayIterator(T[] values) {
        this.values = values;
        index = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return index < values.length;
    }

    /**
     * {@inheritDoc}
     */
    public T next() {
        return values[index++];
    }

    /**
     * throws a {@link UnsupportedOperationException}
     */
    public void remove() {
        throw new UnsupportedOperationException(
                "Values cannot be removed from an ArrayIterator");
    }
}
