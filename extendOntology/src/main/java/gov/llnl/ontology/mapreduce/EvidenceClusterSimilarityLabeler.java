/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.mapreduce;

import gov.llnl.ontology.clustering.ClusterSimilarity;

import gov.llnl.ontology.table.SchemaUtil;
import gov.llnl.ontology.table.WordNetEvidenceSchema;

import edu.ucla.sspace.util.SerializableUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.filecache.DistributedCache;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;

import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;

import java.util.Map;
import java.util.Set;


/**
 * This Map/Reduce executible stores similarity scores for noun pairs in a
 * table defined by a {@link WordNetEvidenceSchema} based on cluster assignemnts
 * and their distributional similarity, as defined by a particular {@link
 * SemanticSpace} algorithm.  Each run of this class computes a similarity score
 * for a given {@link SemanticSpace} using a {@link ClusterSimilarity} method.
 * The similarity scores are stored back into the table defined by a {@link
 * WordNetEvidenceSchema} under one of the cluster similarity  column families.
 *
 * @author Keith Stevens
 */
public class EvidenceClusterSimilarityLabeler extends Configured 
                                              implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(EvidenceClusterSimilarityLabeler.class);

  /**
   * The separator used for separating two noun pairs that function as a key.
   */
  private static final String NOUN_SEPARATOR = "|";

  /**
   * The {@link Configuration} for hadoop and hbase.
   */
  private HBaseConfiguration conf;

  /**
   * The path specifying a serialized {@link ClusterSimilarity}.
   */
  private String clusterSimilarityFile;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new EvidenceClusterSimilarityLabeler(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    // Validate the command line arguments.
    if (args.length != 1) {
      System.out.println(
          "usage: java EvidenceClusterSimilarityLabeler <clusterSimilarity>");
      return 1;
    }

    conf = new HBaseConfiguration();
    clusterSimilarityFile = args[0];

    try {
      // Run the first map/reduce job.
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Applying Cluster Similarity");

      // Setup the mapper.
      job.setJarByClass(EvidenceClusterSimilarityLabeler.class);

      // Add a scanner that requests the requested similarity column.
      Scan scan = new Scan();
      scan.addFamily(WordNetEvidenceSchema.SIMILARITY_CLUSTER_CF.getBytes());

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(
          WordNetEvidenceSchema.tableName, scan,
          EvidenceClusterSimilarityMapper.class, ImmutableBytesWritable.class,
          Put.class, job);

      DistributedCache.addCacheFile(new URI(clusterSimilarityFile), conf);

      // Run the first job.
      LOG.info("Computing term similarity scores.");
      job.waitForCompletion(true);
      LOG.info("Computing completed.");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   * This mapper creates a word similarity scores based on distributional
   * similarity for each noun pair found.  It first loads a {@link
   * ClusterSimilarity} instance into memory and then uses this to determine the
   * similarity between any two noun pairs.
   */
  public class EvidenceClusterSimilarityMapper 
      extends TableMapper<ImmutableBytesWritable, Put> {

    /**
     * The {@link HTable} that will be updated with term similarity scores.
     */
    private HTable evidenceTable;

    /**
     * The {@link ClusterSimilarity} instance to be used for this mapper.
     */
    private ClusterSimilarity clusterSimilarity;

    /**
     * The column name to be used, based on the {@link ClusterSimilarity}
     * instance being used.
     */
    private String columnName;

    /**
      * Initialize the mapper by creating a connection to the {@link
      * WordNetEvidenceSchema} table and loading the {@link ClusterSimilarity}
      * instance.
      */
    @Override
    public void setup(Context context) {
      try {
        super.setup(context);
        evidenceTable = WordNetEvidenceSchema.getTable();

        Path[] localFiles = DistributedCache.getLocalCacheFiles(
            context.getConfiguration());
        clusterSimilarity = SerializableUtil.load(new File(
              localFiles[0].toString()));
        columnName = clusterSimilarity.toString();
      }
      catch (FileNotFoundException e) {
        e.printStackTrace();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    /**
     * Adds {@link ClusterSimilarity} values for each noun pair in the {@link
     * WordNetEvidenceSchema} table. 
     *
     * {@inheritDoc}
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("Generate similarity scores", "seen row").increment(1);

      // Get the document id and it's raw text.
      String[] wordPair = key.toString().split(NOUN_SEPARATOR);

      // Check to see if we have already labeled this row.  If so, skip it.
      Double score = WordNetEvidenceSchema.getSimilarity(
          row, WordNetEvidenceSchema.SIMILARITY_CLUSTER_CF, columnName);
      if (score != null)
        return;

      // Get the term similarity from the cluster instance.
      score = clusterSimilarity.getTermSimilarity(wordPair[0], wordPair[1]);

      // Store the similarity score for the row.
      Put put = new Put(key.get());
      SchemaUtil.add(put, WordNetEvidenceSchema.SIMILARITY_CLUSTER_CF,
                     columnName, score);
      evidenceTable.put(put);
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup(Context context) 
        throws IOException, InterruptedException {
      super.cleanup(context);
      evidenceTable.flushCommits();
      evidenceTable.close();
    }
  }
}
