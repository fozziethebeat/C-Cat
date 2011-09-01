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

import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;
import gov.llnl.ontology.util.Counter;
import gov.llnl.ontology.util.StringPair;

import com.google.gson.reflect.TypeToken;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


/**
 * This class documents the schema of the WordNet Evidence table.  Only word
 * pairs where both terms exist in word net should be entered into the table.
 *
 * @author Keith Stevens
 */
public class WordNetEvidenceTable implements EvidenceTable {

    /**
     * A marker to request all corpora types when scanning.
     */
    public static final String ALL_CORPORA = "";

    /**
     * table name for this schema
     */
    public static final String TABLE_NAME = "WordNetEvidence";

    /**
     * The column family name for the noun pair for each row.  
     */
    public static final String NOUN_PAIR_CF =
            "nounPair";

    /**
     * The column name for the noun pair.
     */
    public static final String NOUN_PAIR_COLUMN = "key";

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
     * The column family name for any similarity measurements between two noun
     * pairs.
     */
    public static final String SIMILARITY_CF = "similarity";

    /**
     * The column family name for the cluster based similarity column family.
     * All values are stored as doubles.
     */
    public static final String CLUSTER_SIMILARITY = "cluster";

    /**
     * The column name for clusters of similiarity lists generated via Locality
     * Sensitive Hashing.
     */
    public static final String LSH_CLUSTER_SIMILARITY = "lsh";

    /**
     * The column family name for the cosine based similarity column family.
     * All values are stored as doubles.
     */
    public static final String COSINE_SIMILARITY = "cosine";

    /**
     * The column family name for the euclidean based similarity column family.
     * All values are stored as doubles.
     */
    public static final String EUCLIDEAN_SIMILARITY = "euclidean";

    /**
     * The column family name for the kl-divergence based similarity column
     * family.  All values are stored as doubles.  Note that this metric is not
     * symmetric.
     */
    public static final String KL_SIMILARITY = "kl_divergence";

    /**
     * The column family name for the Lin based similarity column family.  All
     * values are stored as doubles.
     */
    public static final String LIN_SIMILARITY = "lin";

    /**
     * The annotation name for dependency path counts.
     */
    public static final String DEPENDENCY_PATH_ANNOTATION_NAME =
        "DependencyPathCounts";

    /**
     * The underlying {@link HTable}.
     */
    private HTable table;

    /**
     * {@inheritDoc}
     */
    public String tableName() {
        return TABLE_NAME;
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
            HTableDescriptor desc = new HTableDescriptor(TABLE_NAME);
            SchemaUtil.addDefaultColumnFamily(desc, NOUN_PAIR_CF);
            SchemaUtil.addDefaultColumnFamily(desc, DEPENDENCY_FEATURE_CF);
            SchemaUtil.addDefaultColumnFamily(desc, SIMILARITY_CF);
            SchemaUtil.addDefaultColumnFamily(desc, CLASS_CF);
            admin.createTable(desc);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setupScan(Scan scan) {
        setupScan(scan, ALL_CORPORA);
    }

    /**
     * {@inheritDoc}
     */
    public void setupScan(Scan scan, String corpusName) {
        if (corpusName.equals(ALL_CORPORA))
            scan.addFamily(DEPENDENCY_FEATURE_CF.getBytes());
        else
            scan.addColumn(DEPENDENCY_FEATURE_CF.getBytes(),
                           corpusName.getBytes());
        scan.addFamily(CLASS_CF.getBytes());
        scan.addFamily(NOUN_PAIR_CF.getBytes());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Result> iterator(Scan scan) {
        try {
            return table.getScanner(scan).iterator();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public HTable table() {
        try {
            if (table == null)
                table = new HTable(TABLE_NAME);
            return table;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public StringPair nounPair(Result row) {
        String key = SchemaUtil.getColumn(row, NOUN_PAIR_CF, NOUN_PAIR_COLUMN);
        String[] nouns = key.split(":");
        return new StringPair(nouns[0], nouns[1]);
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

            if (qualifierValueMap.size() == 1)
                return sourceCounts;

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
        return SchemaUtil.getObjectColumn(
                row, DEPENDENCY_FEATURE_CF, source,
                new TypeToken<Counter<String>>(){}.getType());
    }

    /**
     * {@inheritDoc}
     */
    public void putDependencyPaths(String word1, String word2,
                                   String source,
                                   Counter<String> pathCounts) {
        String key = word1 + ":" + word2;
        Put put = new Put(DigestUtils.shaHex(key).getBytes());
        SchemaUtil.add(put, DEPENDENCY_FEATURE_CF, source, pathCounts);
        SchemaUtil.add(put, NOUN_PAIR_CF, NOUN_PAIR_COLUMN, key);
        put(put);
    }

    /**
     * {@inheritDoc}
     */
    public HypernymStatus getHypernymStatus(Result row) {
        return HypernymStatus.valueOf(
                SchemaUtil.getColumn(row, CLASS_CF, HYPERNYM_EVIDENCE));
    }

    /**
     * {@inheritDoc}
     */
    public void putHypernymStatus(ImmutableBytesWritable key, 
                                  HypernymStatus status) {
        Put put = new Put(key.get());
        SchemaUtil.add(put, CLASS_CF, HYPERNYM_EVIDENCE,
                       status.toString());
        put(put);
    }

    public void close() {
        try {
            table.flushCommits();
            table.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    private void put(Put put) {
        try {
            table.put(put);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
