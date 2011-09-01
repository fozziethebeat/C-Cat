package gov.llnl.ontology.text;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.util.BiMap;
import edu.ucla.sspace.util.HashBiMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public abstract class AbstractBasisMapping<T, K> implements BasisMapping<T, K>,
                                                            Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The mapping from keys to dimension indices.
     */
    private BiMap<K, Integer> mapping;

    /**
     * Set to {@code true} when the {@link BasisMapping} should not create new
     * dimensions for unseen keys.
     */
    private boolean readOnly;

    /**
     * Creates a new {@link AbstractBasisMapping}.
     */
    public AbstractBasisMapping() {
        mapping = new HashBiMap<K, Integer>();
        readOnly = false;
    }

    /**
     * {@inheritDoc}
     */
    public K getDimensionDescription(int dimension) {
        return mapping.inverse().get(dimension);
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return mapping.keySet();
    }

    /**
     * Returns an integer corresponding to {@code key}.  If in read only mode,
     * -1 is returned for unseen keys.  Otherwise, unseen keys are assigned a
     *  new dimension.
     */
    protected int getDimensionInternal(K key) {
        Integer index = mapping.get(key);
        if (readOnly)
            return (index == null) ? -1: index;

        if (index == null) {     
            synchronized(this) {
                // Recheck to see if the key was added while blocking.
                index = mapping.get(key);

                // If another thread has not already added this key while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = mapping.size();
                    mapping.put(key, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the internal mapping from keys to indices.
     */
    protected Map<K, Integer> getMapping() {
        return mapping;
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() {
        return mapping.size();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return readOnly;
    }
}

