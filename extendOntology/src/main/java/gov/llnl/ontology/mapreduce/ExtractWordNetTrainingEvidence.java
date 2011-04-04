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

import gov.llnl.ontology.hbase.DocumentReader;
import gov.llnl.ontology.hbase.TrinidadDocumentReader;

import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;

import gov.llnl.ontology.table.WordNetEvidenceSchema;
import gov.llnl.ontology.table.SchemaUtil;

import com.google.common.collect.Maps;

import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyRelation;

import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

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

import org.apache.hadoop.io.Text;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This Map/Reduce execuitble takes a document, finds noun pairs, finds the
 * shortest dependency path between the two nouns, marks whether or not the two
 * nouns are connected in word net via an ISA link.  The output of this map
 * reduce can be used to train a logistic regression model that can later
 * determine whether or not a new dependency paths between two nouns is evidence
 * for a ISA link.
 *
 * @author Keith Stevens
 *
 */
public class ExtractWordNetTrainingEvidence extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(ExtractWordNetTrainingEvidence.class);

  /**
   * The separator used for separating two noun pairs from their class type
   * during the map phase.
   */
  private static final String SOURCE_SEPARATOR = ";;";

  /**
   * The HBase configuration for this map reduce job.
   */
  private HBaseConfiguration conf;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new ExtractWordNetTrainingEvidence(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public int run(String[] args) throws Exception {
    conf = new HBaseConfiguration();

    if (runExtractionJob()==1) return 1;
    if (runLabelingJob()==1) return 1;
    return 0;
  }

  /**
   * Runs the first map/reduce job.  This job will extract noun pairs from a
   * document table and store the noun pair, along with any observed dependency
   * paths into the {@link WordNetEvidenceSchema} table.
   */
  public int runExtractionJob() {
    try {
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Extract Hypernym training pairs");

      // Setup the mapper.
      job.setJarByClass(ExtractWordNetTrainingEvidence.class);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      DocumentReader reader = new TrinidadDocumentReader();
      reader.setupScan(scan);

      // Setup the mapper
      TableMapReduceUtil.initTableMapperJob(
          reader.getTableName(), scan, ExtractDependencyPathMapper.class, 
          Text.class, Text.class, job);

      // Setup the reducer.
      TableMapReduceUtil.initTableReducerJob(
          WordNetEvidenceSchema.tableName, ExtractDependencyPathReducer.class,
          job);
      job.setNumReduceTasks(24);

      // Run the first job.
      LOG.info("Extracting training instances from documents.");
      job.waitForCompletion(true);
      LOG.info("Dependency Path Extraction complete");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   * Runs the second map/reduce job.  This job will provide class labels for
   * each noun pair found in the {@link WordNetEvidenceSchema} table.
   */
  public int runLabelingJob() {
    try {
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Label Hypernym training pairs");

      // Setup the mapper.
      job.setJarByClass(ExtractWordNetTrainingEvidence.class);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      scan.addFamily(WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF.getBytes());

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(
          WordNetEvidenceSchema.tableName, scan, LabelEvidencePairsMapper.class,
          Text.class, Put.class, job);

      // Setup the reducer.
      TableMapReduceUtil.initTableReducerJob(
          WordNetEvidenceSchema.tableName, IdentityTableReducer.class, job);
      job.setNumReduceTasks(0);

      // Run the first job.
      LOG.info("Labeling noun pairs with relationship classes.");
      job.waitForCompletion(true);
      LOG.info("Done labeling noun pairs with relationship classes.");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   * This reducer generates the {@link Put} instances that will write the
   * dependency path feature vector and class type for a noun pair.  The
   * dependency path feature vector is stored {@link Map} from dependency path
   * strings to their occurrence counts. The class is stored as a serialized
   * {@link HypernymStatus} 
   */
  public static class ExtractDependencyPathReducer 
    extends TableReducer<Text, Text, Put> {

    /**
     * The table storing hypernym evidence.  Keys are noun pairs, column values
     * are dependency path counts and the class type for the evidence.
     */
    private HTable evidenceTable;

    /**
     * Initializes access to the Evidence Table.
     */
    @Override
    protected void setup(Context context) 
        throws IOException, InterruptedException {
      context.getCounter("populate table", "setup").increment(1);
      evidenceTable = WordNetEvidenceSchema.getTable();
    }

    /**
     * Determins the {@link HypernymStatus} and cousin relationship of each noun
     * pair and sums up the observed dependency paths.  
     */
    @Override
    protected void reduce(Text key, 
                          Iterable<Text> values, 
                          Context context) 
        throws IOException, InterruptedException {
      context.getCounter("count paths", "merge pair").increment(1);
      String[] wordPairAndSource = key.toString().split(
          ExtractDependencyPathMapper.SOURCE_SEPARATOR);
      if (wordPairAndSource.length != 2)
        return;

      // Count the number of times that each dependency path occurs with the
      // given key.
      Map<String, Integer> pathCounter = Maps.newHashMap();
      for (Text dependencyPath : values) {
        Integer pathCount = pathCounter.get(dependencyPath.toString());
        pathCounter.put(dependencyPath.toString(),
                        (pathCount == null) ? 1 : pathCount + 1);
      }

      // Create a new Put for the observed word pair.
      Put featurePut = new Put(wordPairAndSource[0].getBytes());
      LOG.info("Creating entry for: " + wordPairAndSource[0]);

      // Add the dependency features to the row in the feature table.
      SchemaUtil.add(featurePut, WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF, 
                     wordPairAndSource[1],
                     pathCounter);

      // Write the column values to the table for the given noun pair.
      evidenceTable.put(featurePut);
      context.getCounter("count paths", "data saved").increment(1);
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

  /**
   * This Mapper writes the class labels for each  noun pair found in the
   * {@link WordNetEvidenceSchema} table to the table. 
   */
  public static class LabelEvidencePairsMapper extends TableMapper<Text, Put> {

    /**
     * The table storing hypernym evidence.  Keys are noun pairs, column values
     * are dependency path counts and the class type for the evidence.
     */
    private HTable evidenceTable;

    /**
     * Initializes access to the Evidence Table and the word net reader.
     */
    @Override
    protected void setup(Context context) 
        throws IOException, InterruptedException {
      context.getCounter("populate table", "setup").increment(1);
      evidenceTable = WordNetEvidenceSchema.getTable();
      WordNetCorpusReader.initialize("/dict", true);
    }

    /**
     * Determins the {@link HypernymStatus} and cousin relationship of each noun
     * pair and sums up the observed dependency paths.  
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {

      context.getCounter("Label Pairs", "seen pair").increment(1);
      String keyStr = new String(key.get());
      System.out.println(keyStr);
      String[] wordPair = keyStr.split("\\|");
      if (wordPair.length != 2)
        return;

      // For each valid noun pair:
      // Emit the dependency path for the observed noun pair.
      HypernymStatus hypernymEvidenceStatus = 
            SynsetRelations.getHypernymStatus(wordPair[0], wordPair[1]);

      context.getCounter(
          "Label Pairs", "valid evidence").increment(1);

      // Determine the cousin path.  If it is a known hypernym, we know that
      // they are either 0-1 or 1-0 cousins.
      Pair<Integer> cousinPath = SynsetRelations.getCousinDistance(
          wordPair[0], wordPair[1], 7);
      if (cousinPath == null)
        return;

      // Transfor the path labeling such that the largest is always first,
      // merging m-n and n-m cousin labels.
      if (cousinPath.x < cousinPath.y)
        cousinPath = new Pair<Integer>(cousinPath.y, cousinPath.x);

      context.getCounter(
          "Label Pairs", "valid cousins").increment(1);

      Put featurePut = new Put(keyStr.getBytes());
      LOG.info("Creating entry for: " + keyStr);

      // Add the class labels for hypernym evidence and cousin path evidence.
      SchemaUtil.add(featurePut, WordNetEvidenceSchema.CLASS_CF, 
                     WordNetEvidenceSchema.HYPERNYM_EVIDENCE,
                     hypernymEvidenceStatus.toString());

      SchemaUtil.add(featurePut, WordNetEvidenceSchema.CLASS_CF, 
                     WordNetEvidenceSchema.COUSIN_EVIDENCE,
                     cousinPath.toString());
      evidenceTable.put(featurePut);
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
