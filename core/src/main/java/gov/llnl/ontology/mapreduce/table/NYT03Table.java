package gov.llnl.ontology.mapreduce.table;


/**
 * A subclass of {@link TrinidadTable} which simply has the name "nyt03".
 *
 * @author Keith Stevens
 */
public class NYT03Table extends TrinidadTable {

    /**
     * Returns {@code nyt03}.
     */
    public String tableName() {
        return "nyt03";
    }
}

