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

package gov.llnl.ontology.mapreduce.table;

import gov.llnl.ontology.util.Counter;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;

import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import java.io.IOError;
import java.io.IOException;

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
public class WordNetEvidenceTable implements EvidenceTable {

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
     * {@inheritDoc}
     */
    public String tableName() {
        return TABLE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] tableNameBytes() {
        return TABLE_NAME.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public String classColumnFamily() {
        return CLASS_CF;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] classColumnFamilyBytes() {
        return CLASS_CF.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public String dependencyColumnFamily() {
        return DEPENDENCY_FEATURE_CF;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] dependencyColumnFamilyBytes() {
        return DEPENDENCY_FEATURE_CF.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public String hypernymColumn() {
        return HYPERNYM_EVIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] hypernymColumnBytes() {
        return HYPERNYM_EVIDENCE.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public String cousinColumn() {
        return COUSIN_EVIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] cousinColumnBytes() {
        return COUSIN_EVIDENCE.getBytes();
    }

    /**
     * {@inheritDoc}
     */
    public void createTable() {
        try {
            Configuration conf = HBaseConfiguration.create();
            HConnection connector = HConnectionManager.getConnection(conf);
            createTable(connector);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createTable(HConnection connector) {
        try {
            // Create configuration and admin classes.
            Configuration config = HBaseConfiguration.create();
            HBaseAdmin admin = new HBaseAdmin(config);

            // Do nothing if the table already exists.
            if (admin.tableExists(TABLE_NAME)) 
                return;

            // Add the column families to the table.
            HTableDescriptor evidenceDesc = new HTableDescriptor(TABLE_NAME);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,DEPENDENCY_FEATURE_CF);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,SIMILARITY_COSINE_CF);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,SIMILARITY_EUCLIDEAN_CF);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,SIMILARITY_KL_CF);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,SIMILARITY_LIN_CF);
            SchemaUtil.addDefaultColumnFamily(evidenceDesc,CLASS_CF);
            admin.createTable(evidenceDesc);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public HTable table() {
        try {
            return new HTable(TABLE_NAME);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Instantiates a new table.
     */
    public static void main(String[] args) {
        WordNetEvidenceTable table = new WordNetEvidenceTable();
        table.createTable();
    }

    /**
     * {@inheritDoc}
     */
    public Counter<String> getDependencyPaths(Result row) {
        Counter<String> pathCounts = new Counter<String>();
        Map<byte[], byte[]> qualifierValueMap = 
                row.getFamilyMap(DEPENDENCY_FEATURE_CF.getBytes());
        for (byte[] bytes : qualifierValueMap.values()) {
            Counter<String> sourceCounts = getDependencyPaths(
                    row, new String(bytes));
            if (sourceCounts == null)
                continue;

            for (Map.Entry<String, Integer> newCount : sourceCounts) 
                pathCounts.count(newCount.getKey(), newCount.getValue());
        }

        return pathCounts;
    }

    /**
     * {@inheritDoc}
     */
    public Counter<String> getDependencyPaths(Result row, 
                                              String source) {
        return SchemaUtil.getObjectColumn(row, DEPENDENCY_FEATURE_CF, source);
    }

    /**
     * {@inheritDoc}
     */
    public void storeDependencyPaths(Put put,
                                     String source,
                                     Counter<String> pathCounts) {
        SchemaUtil.add(put, DEPENDENCY_FEATURE_CF, source, pathCounts);
    }
}
