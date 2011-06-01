/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gov.llnl.ontology.table;

import static org.apache.hadoop.hbase.HColumnDescriptor.DEFAULT_BLOCKCACHE;
import static org.apache.hadoop.hbase.HColumnDescriptor.DEFAULT_BLOOMFILTER;
import static org.apache.hadoop.hbase.HColumnDescriptor.DEFAULT_IN_MEMORY;
import static org.apache.hadoop.hbase.HColumnDescriptor.DEFAULT_TTL;
import static org.apache.hadoop.hbase.HColumnDescriptor.DEFAULT_VERSIONS;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;

import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;

import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.HashMap;
import java.util.Map;


/**
 * This class documents the schema of the WordNet Evidence table.  Only word
 * pairs where both terms exist in word net should be entered into the table.
 *
 * </br>
 *
 * Table name:
 * <ul>
 * <li>WordNetEvidence</li>
 * </ul>
 * 
 * Key format:
 * <ul>
 * <li>term|term</li>
 * <ul>
 *
 * Column Families:
 * <ul>
 * <li>features</li>
 * <li>class</li>
 * <li>similarity_cluster</li>
 * <li>similarity_cosine</li>
 * <li>similarity_euclidean</li>
 * <li>similarity_kl_divergence</li>
 * <li>similarity_lin</li>
 * </ul>
 *
 * Column identifiers: (in the format CF:id)
 * <ul>
 * <li>features:DependencyFeatures</li>
 * <li>class:hypernymStatusEvidence</li>
 * <li>class:cousinEvidence</li>
 * <li>similarity_cluster:lsh</li>
 * <li>similarity_cosine:hal</li>
 * </ul>
 *
 * All similarity scores are stored as doubles if the word pair exists in a
 * semantic space algorithm, or as an {@code NaN} if the word pair does not
 * exist.
 *
 * @author Keith Stevens
 */
public class WordNetEvidenceSchema {

    /**
     * The table name attributed to this schema.
     */
    public static final String tableName = "WordNetEvidence";

    /**
     * table name for this schema
     */
    public static final String TABLE_NAME = "WordNetEvidence";

    /**
     * The column family name for the dependency features.  Column names will be
     * based on the source of the corpus for each dependency feature.
     */
    public static final String DEPENDENCY_FEATURE_CF =
            "dependencyFeatures";

    /**
     * The column family name for the class family.
     */
    public static final String CLASS_CF = "class";

    /**
     * The column name for the hyernym evidence class.  Positive values are
     * marked as "KNOWN_HYPERNYM" and negative values are marked as
     * "KNOWN_NON_HYPERNYM".  Pairs that serve as potential additions to wordnet
     * are marked as NOVEL_{HYPERNYM|HYPONYM}.  Use {@link
     * WordNetEvidence#HypernymStatus} to read covert values in this column to
     * the appropriate enum.
     */
    public static final String HYPERNYM_EVIDENCE = "hypernymEvidenceStatus";

    /**
     * The column name for the coordinate evidence class.  Values are stored as
     * a pair of integers separated by a hyphen, such as "m-n".  Cousins share
     * some ancestor in the wordnet hierarchy, where m specifies the distance
     * between the first term and the common ancestor and n specifies the
     * distance between the second term and the common ancestor.  A distance of
     * {@link Integer#MAX_VALUE} signifies that the common ancesstor is beyond a
     * particular depth, most likely 7.
     */
    public static final String COUSIN_EVIDENCE = "cousinEvidence";

    /**
     * The column family name for the cluster based similarity column family.
     * All values are stored as doubles.
     */
    public static final String SIMILARITY_CLUSTER_CF = "similarity_cluster";

    /**
     * The column name for clusters of similiarity lists generated via Locality
     * Sensitive Hashing.
     */
    public static final String LSH_CLUSTER_SIMILARITY = "lsh";

    /**
     * The column family name for the cosine based similarity column family.
     * All values are stored as doubles.
     */
    public static final String SIMILARITY_COSINE_CF = "similarity_cosine";

    /**
     * The column family name for the euclidean based similarity column family.
     * All values are stored as doubles.
     */
    public static final String SIMILARITY_EUCLIDEAN_CF = "similarity_euclidean";

    /**
     * The column family name for the kl-divergence based similarity column
     * family.  All values are stored as doubles.  Note that this metric is not
     * symmetric.
     */
    public static final String SIMILARITY_KL_CF = "similarity_kl_divergence";

    /**
     * The column family name for the Lin based similarity column family.  All
     * values are stored as doubles.
     */
    public static final String SIMILARITY_LIN_CF = "similarity_lin";

    /**
     * The annotation name for dependency path counts.
     */
    public static final String DEPENDENCY_PATH_ANNOTATION_NAME =
        "DependencyPathCounts";

    /**
     * Creates a new instance of the {@link WordNetEvidenceSchema}.
     */
    public static void createTable() throws IOException {
        HBaseConfiguration conf = new HBaseConfiguration();
        HConnection connector = HConnectionManager.getConnection(conf);
        createTable(connector);
    }

    /**
     * Creates the new instance of the table.
     */
    public static void createTable(HConnection connector) throws IOException {
        // Do nothing if the table already exists.
        if (connector.tableExists(tableName.getBytes())) 
            return;

        // Create configuration and admin classes.
        HBaseConfiguration config = new HBaseConfiguration();
        HBaseAdmin admin = new HBaseAdmin(config);

        // Add the column families to the table.
        HTableDescriptor evidenceDesc = new HTableDescriptor(
                tableName.getBytes());
        addDefaultColumnFamily(evidenceDesc,DEPENDENCY_FEATURE_CF);
        addDefaultColumnFamily(evidenceDesc,SIMILARITY_COSINE_CF);
        addDefaultColumnFamily(evidenceDesc,SIMILARITY_EUCLIDEAN_CF);
        addDefaultColumnFamily(evidenceDesc,SIMILARITY_KL_CF);
        addDefaultColumnFamily(evidenceDesc,SIMILARITY_LIN_CF);
        addDefaultColumnFamily(evidenceDesc,CLASS_CF);
        admin.createTable(evidenceDesc);
    }

    /**
     * Returns access to the created table.
     */
    public static HTable getTable() throws IOException {
        Configuration config = HBaseConfiguration.create();
        return new HTable(config, tableName);
    }

    /**
     * Instantiates a new table.
     */
    public static void main(String[] args) {
        try {
            createTable();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a new map that contains all of the dependency
     * path counts, regardless of their source.
     */
    public static Counter<String> getDependencyPaths(Result row) {
        Counter<String> pathCounts = new Counter<String>();
        Map<byte[], byte[]> qualifierValueMap = 
                row.getFamilyMap(DEPENDENCY_FEATURE_CF.getBytes());
        for (byte[] bytes : qualifierValueMap.values()) {
            Map<String, Integer> sourceCounts = getDependencyPaths(
                    row, new String(bytes));
            if (sourceCounts == null)
                continue;

            for (Map.Entry<String, Integer> newCount : sourceCounts.entrySet()) 
                pathCounts.count(newCount.getKey(), newCount.getValue());
        }

        return pathCounts;
    }

    /**
     * Returns a map that contains all of the dependency paths
     * associated with a single noun pair.
     */
    public static Map<String, Integer> getDependencyPaths(Result row, 
                                                          String source) {
        return SchemaUtil.getObjectColumn(row, DEPENDENCY_FEATURE_CF, source);
    }
}
