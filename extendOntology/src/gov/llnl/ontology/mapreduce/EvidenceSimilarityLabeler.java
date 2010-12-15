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

import gov.llnl.ontology.table.WordNetEvidenceSchema;
import gov.llnl.ontology.table.SchemaUtil;

import com.google.common.collect.Maps;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.filecache.DistributedCache;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.HConstants;
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

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapred.JobConf;

import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URI;

import java.util.Map;
import java.util.Set;


/**
 * This Map/Reduce executible computes similarity scores for noun pairs in a
 * table defined by a {@link WordNetEvidenceSchema} based on their
 * distributional similarity, as defined by a particular {@link SemanticSpace}
 * algorithm.  Each run of this class computes a similarity score for a given
 * {@link SemanticSpace} using a list of specified similarity metric.  The
 * similarity scores are stored back into the table defined by a {@link
 * WordNetEvidenceSchema} under one of the similarity column families.
 *
 * @author Keith Stevens
 */
public class EvidenceSimilarityLabeler extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(EvidenceSimilarityLabeler.class);

  /**
   * The {@link Similarity#SimType}s of interest for this mapper.
   */
  private static final SimType[] SIMILARITY_TYPES = {
    SimType.COSINE, SimType.EUCLIDEAN, SimType.KL_DIVERGENCE, SimType.LIN
  };

  private static final double[] MIN_VALUES = {
    -1, Double.MAX_VALUE, Double.MAX_VALUE, 0
  };

  /**
   * The corresponding column family names for each similarity type.
   */
  private static final String[] SIM_FAMILY_NAMES =  {
    WordNetEvidenceSchema.SIMILARITY_COSINE_CF,
    WordNetEvidenceSchema.SIMILARITY_EUCLIDEAN_CF,
    WordNetEvidenceSchema.SIMILARITY_KL_CF,
    WordNetEvidenceSchema.SIMILARITY_LIN_CF
  };

  /**
   * The separator used for separating two noun pairs that function as a key.
   */
  private static final String NOUN_SEPARATOR = "\\|";

  /**
   * The {@link Configuration} for hadoop and hbase.
   */
  private HBaseConfiguration conf;

  /**
   * The path specifying a serialized {@link SemanticSpace}.
   */
  private String sspaceFile;

  /**
   * The column name, which is based on the similarity type and the word
   * space algorithm.
   */
  private static String similarityQualifier = "ri-nyt";

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    similarityQualifier = args[0];
    try {
      ToolRunner.run(new Configuration(),
                     new EvidenceSimilarityLabeler(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    // Validate the command line arguments.
    if (args.length != 2) {
      System.out.println(
          "usage: java EvidenceSimilarityLabeler " +
          "wordSpaceType <wordspace>");
      return 1;
    }

    conf = new HBaseConfiguration();

    // Setup the semantic space name and the column name based on the semantic
    // space type.
    sspaceFile = args[1];

    try {
      // Run the first map/reduce job.
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Applying Semantic Similarity Scores");

      // Setup the mapper.
      job.setJarByClass(EvidenceSimilarityLabeler.class);

      // Add a scanner that requests the requested similarity column.
      Scan scan = new Scan();
      for (String columnFamily : SIM_FAMILY_NAMES)
        scan.addFamily(columnFamily.getBytes());
      scan.addFamily(WordNetEvidenceSchema.CLASS_CF.getBytes());

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(
          WordNetEvidenceSchema.tableName, scan, EvidenceSimilarityMapper.class,
          Text.class, Put.class, job);

      // Setup the reducer.
      TableMapReduceUtil.initTableReducerJob(
          WordNetEvidenceSchema.tableName, IdentityTableReducer.class, job);
      job.setNumReduceTasks(24);

      Configuration jobConf = job.getConfiguration();
      DistributedCache.addCacheFile(new URI(sspaceFile), jobConf);

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
   * This Mapper traverses each row in a {@link WordNetEvidenceSchema} based
   * table and adds similarity scores for a specific similarity metric and
   * wordspace algorithm.
   */
  public static class EvidenceSimilarityMapper extends TableMapper<Text, Put> {

    /**
     * The {@link HTable} that will be updated with term similarity scores.
     */
    private HTable evidenceTable;

    /**
     * The {@link SemanticSpace} algorithm that will be use to determine word
     * similarity.
     */
    private SemanticSpace sspace;

    /**
     * The set of known words in the word space.
     */
    private Set<String> sspaceWords;

    /**
     * {@inheritDoc}
      */
    @Override
    public void setup(Context context) {
      try {
        super.setup(context);
        evidenceTable = WordNetEvidenceSchema.getTable();

        Path[] localFiles = DistributedCache.getLocalCacheFiles(
            context.getConfiguration());
        sspace = new StaticSemanticSpace(localFiles[0].toString());
        sspaceWords = sspace.getWords();
        System.out.println("word: " + sspaceWords.contains("cat"));
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
     * Adds similarity scores for each row in the {@link WordNetEvidenceSchema}
     * table.  This only adds the similarity score for a single word space type.
     *
     * {@inheritDoc}
     */
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("Generate similarity scores", "seen row").increment(1);

      // Get the document id and it's raw text.
      String keyStr = new String(key.get());
      String[] wordPair = keyStr.split(NOUN_SEPARATOR);
      wordPair[0] = wordPair[0].toLowerCase().trim();
      wordPair[1] = wordPair[1].toLowerCase().trim();

      for (int s = 0; s < SIM_FAMILY_NAMES.length; ++s) {
        // Check to see if we have already labeled this row.  If so, skip it.
        Double score = WordNetEvidenceSchema.getSimilarity(
            row, SIM_FAMILY_NAMES[s], similarityQualifier);

        // Check to see if the two terms exist in the word space.  If they do
        // not, then save their similarity scores as 0.
        if (!sspaceWords.contains(wordPair[0]) ||
            !sspaceWords.contains(wordPair[1]))
          score = MIN_VALUES[s];
        // Otherwise extract the similarity score from the word space.
        else
          score = Similarity.getSimilarity(SIMILARITY_TYPES[s],
                                           sspace.getVector(wordPair[0]),
                                           sspace.getVector(wordPair[1]));

        System.out.println(keyStr + ": " + score);

        // Store the similarity score for the row.
        Put put = new Put(key.get());
        SchemaUtil.add(put, SIM_FAMILY_NAMES[s],
                       similarityQualifier, score); 
        evidenceTable.put(put);
      }
    }

    /**
     * {@inheritDoc}
     */
    protected void cleanup(Context context) 
        throws IOException, InterruptedException {
      super.cleanup(context);
      evidenceTable.flushCommits();
      evidenceTable.close();
    }
  }
}
