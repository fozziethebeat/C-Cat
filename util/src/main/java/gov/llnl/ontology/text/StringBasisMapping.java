package gov.llnl.ontology.text;


/**
 * @author Keith Stevens
 */
public class StringBasisMapping extends AbstractBasisMapping<String, String> {

    /**
     * {@inheritDoc}
     */
    public int getDimension(String key) {
        return getDimensionInternal(key);
    }
}
