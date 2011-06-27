

package gov.llnl.ontology.mapreduce.table;

import gov.llnl.ontology.util.Counter;

import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;


/**
 * An interface for accessing a {@link HTable} that will store syntactic
 * patterns between noun pairs.  Many of these functions are to allow for a
 * different underlying structure of the table.  Each table schema can have it's
 * own column family names, column names, and internal structure for each
 * feature type.
 *
 * Schemas must permit at least the following behaviours:
 * <ul>
 *   <li>Dependency path counts can be stored based on their source corpus</li>
 *   <li>Dependency path counts can be accessed based on their source
 *   coprus</li>
 *   <li>Hypernym class labels and cousin class labels must be storable and
 *   accessible</li>
 *   <li>The table must be createable</li>
 * </ul>
 *
 * @author Keith Stevens
 */
public interface EvidenceTable {

    /**
     * Returns a new map that contains all of the dependency
     * path counts, regardless of their source.
     */
    Counter<String> getDependencyPaths(Result row);

    /**
     * Stores the dependency path counts gathred from the {@link source} corpus
     * using the provided {@link Put} object.
     */
    void storeDependencyPaths(Put put,
                              String source,
                              Counter<String> pathCounts);

    /**
     * Returns a map that contains all of the dependency paths
     * associated with a single noun pair.
     */
    Counter<String> getDependencyPaths(Result row, String source);

    /**
     * Returns the string name of the HBase table.
     */
    String tableName();

    /**
     * Returns the name of the HBase table as a byte array.
     */
    byte[] tableNameBytes();

    /**
     * Returns the string name of the dependency path column family.
     */
    String dependencyColumnFamily();

    /**
     * Returns the name of the dependency path column family as a byte array.
     */
    byte[] dependencyColumnFamilyBytes();

    /**
     * Returns the string name of the class column family.
     */
    String classColumnFamily();

    /**
     * Returns the name of the class column family as a byte array.
     */
    byte[] classColumnFamilyBytes();

    /**
     * Returns the column name for hypernym class labels.
     */
    String hypernymColumn();

    /**
     * Returns the column name for hypernym class labels as a byte array.
     */
    byte[] hypernymColumnBytes();

    /**
     * Returns the column name for cousin class labels.
     */
    String cousinColumn();

    /**
     * Returns the column name for cousin class labels as a byte array.
     */
    byte[] cousinColumnBytes();

    /**
     * Creates a new {@link HTable} connection.
     */
    HTable table();

    /**
     * Creates a new instance of this {@link EvidenceTable} using the default
     * HBase connection.
     */
    void createTable();

    /**
     * Creates a new instance of this {@link EvidenceTable} using the given
     * {@link HConnection}.
     */
    void createTable(HConnection connector);
}
