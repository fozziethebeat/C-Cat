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

import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import gov.llnl.ontology.evidence.CousinTrainingInstanceBuilder;
import gov.llnl.ontology.evidence.HypernymLearningInstanceBuilder;
import gov.llnl.ontology.evidence.EvidenceInstanceBuilder;
import gov.llnl.ontology.evidence.AttributeMap;

import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.classifier.sgd.LogisticModelParameters;

import org.apache.mahout.math.AbstractVector;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.net.URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Keith Stevens
 *
 */
public class FindValidRelations extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(FindValidRelations.class);

  /**
   * The separator used for separating two noun pairs that function as a key.
   */
  private static final String NOUN_SEPARATOR = "|";

  public static final String SOURCE = "source";
  public static final String ATTRIBUTE_MAP = "attributeMap";
  public static final String HYPERNYM_CLASSIFIER = "hypernymClassifier";

  /**
   * The {@link Configuration} for HBase and Hadoop.
   */
  private HBaseConfiguration conf;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new FindValidRelations(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public int run(String[] args) throws Exception {
    if (args.length != 3)
      throw new IllegalArgumentException(
          "usage: FindValidRelations " +
          "source <attributeMap> <HypernymClassifier>");
    conf = new HBaseConfiguration();

    conf.set(SOURCE, args[0]);
    conf.set(ATTRIBUTE_MAP, args[1]);
    conf.set(HYPERNYM_CLASSIFIER, args[2]);

    try {
      // Run the first map/reduce job. 
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Finding optimal Hypernym relations");

      FileSystem fs = FileSystem.get(conf);
      AttributeMap attributeMap = SerializableUtil.load(
          fs.open(new Path(args[1])));
      EvidenceInstanceBuilder builder = new HypernymLearningInstanceBuilder(
          attributeMap, false, conf.get(SOURCE));
      //EvidenceInstanceBuilder cousinBuilder = new CousinTrainingInstanceBuilder(
      //    attributeMap, false, conf.get(SOURCE), 7);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      builder.addToScan(scan);
      //cousinBuilder.addToScan(scan);

      // Setup the mapper.
      job.setJarByClass(FindValidRelations.class);

      TableMapReduceUtil.initTableMapperJob(
          WordNetEvidenceSchema.tableName, scan, 
          NovelRelationEvidenceMapper.class,  Text.class, Text.class, job);

      // Setup the reducer.
      job.setReducerClass(NovelRelationEvidenceReducer.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      TextOutputFormat.setOutputPath(
          job, new Path("/data/newNounLocations"));
      job.setNumReduceTasks(24);

      // Run the first job.
      LOG.info("Computing novel relations.");
      job.waitForCompletion(true);
      LOG.info("Novel relations found.");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   */
  public static class NovelRelationEvidenceMapper 
      extends TableMapper<Text, Text> {

    private EvidenceInstanceBuilder hypernymBuilder;
    private EvidenceInstanceBuilder cousinBuilder;
    private OnlineLogisticRegression hypernymClassifier;
    private OnlineLogisticRegression cousinClassifier;

    @Override
    public void setup(Context context) {
      try {
        super.setup(context);

        // Get a connection to the file system to access the local files.
        Configuration conf = context.getConfiguration();
        FileSystem fs = FileSystem.get(conf);

        // Read the weka attribute map.
        ObjectInputStream ois = new ObjectInputStream(
              fs.open(new Path(conf.get(ATTRIBUTE_MAP))));
        AttributeMap attributeMap = (AttributeMap) ois.readObject();
        ois.close();

        // Create the builders.
        hypernymBuilder = new HypernymLearningInstanceBuilder(
            attributeMap, true, conf.get(SOURCE));
        //cousinBuilder = new CousinTrainingInstanceBuilder(
        //    attributeMap, false, conf.get(SOURCE), 7);

        // Load up the classifiers.
        LogisticModelParameters lmp = LogisticModelParameters.loadFrom(
            new InputStreamReader(fs.open(new Path(conf.get(HYPERNYM_CLASSIFIER)))));
        hypernymClassifier = lmp.createRegression();
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    /**
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("", "seen row").increment(1);

      String[] nounPair = key.toString().split(NOUN_SEPARATOR);

      DoubleVector hypernymEvidence = hypernymBuilder.getInstanceFrom(row);
      DoubleVector cousinEvidence = null;//cousinBuilder.getInstanceFrom(row);

      if (hypernymEvidence == null && cousinEvidence == null)
        return;

      double hypernymScore = 0;
      Vector cousinScore = new DenseVector(
          cousinBuilder.classValues().length-1);

      if (hypernymEvidence != null) {
        Vector dataPoint = new RandomAccessSparseVector(
            hypernymEvidence.length()-1);
        SparseDoubleVector sdv = (SparseDoubleVector) hypernymEvidence;
        int[] nz = sdv.getNonZeroIndices();
        for (int i = 0 ; i < nz.length-1; ++i)
          dataPoint.set(nz[i], sdv.get(nz[i]));
        hypernymScore = hypernymClassifier.classifyScalar(dataPoint);
      }

      if (cousinEvidence != null) {
        Vector dataPoint = new DenseVector(cousinEvidence.length()-1);
        for (int i = 0; i < cousinEvidence.length()-1; ++i)
          dataPoint.set(i, cousinEvidence.get(i));
        cousinScore = cousinClassifier.classify(dataPoint);
      }

      String output = nounPair[1] + NOUN_SEPARATOR + 
                      hypernymEvidence + NOUN_SEPARATOR +
                      cousinScore.asFormatString();

      context.write(new Text(nounPair[0].getBytes()),
                    new Text(output.getBytes()));
    }
  }

  /**
   */
  public static class NovelRelationEvidenceReducer 
    extends Reducer<Text, Text, Text, Text> {

    public AttributeMap attributeMap;

    /**
     * Initializes access to the Evidence Table.
     */
    @Override
    protected void setup(Context context) 
        throws IOException, InterruptedException {
      try {
        context.getCounter("populate table", "setup").increment(1);

        // Get a connection to the file system to access the local files.
        Configuration conf = context.getConfiguration();
        FileSystem fs = FileSystem.get(conf);

        ObjectInputStream ois = new ObjectInputStream(
              fs.open(new Path(conf.get(ATTRIBUTE_MAP))));
        attributeMap = (AttributeMap) ois.readObject();
        ois.close();

        WordNetCorpusReader.initialize("/dict", true);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    /**
     */
    @Override
    protected void reduce(Text key, 
                          Iterable<Text> values, 
                          Context context) 
        throws IOException, InterruptedException {
      int numValues = 0;
      for (Text value : values)
        numValues++;

      String[] attachmentLocations = new String[numValues];
      double[] attachmentScores = new double[numValues];
      List<Map<String, Double>> cousinScores =
        new ArrayList<Map<String, Double>>(numValues);

      String[] classValues = attributeMap.classValues();
      int i = 0;
      for (Text term : values) {
        String[] tokens = term.toString().split("\\|");
        attachmentLocations[i] = tokens[0];
        attachmentScores[i] = Double.parseDouble(tokens[1]);

        Vector scoreVector = AbstractVector.decodeVector(tokens[2]);
        Map<String, Double> scores = new HashMap<String, Double>();
        /*
        for (int j = 0; j < scores.size(); ++j) 
          scores.put(classValues[j], scores.get(j));
          */
        cousinScores.add(scores);
      }

      Synset bestLocation = SynsetRelations.bestAttachmentPoint(
          attachmentLocations, attachmentScores, cousinScores, .95);

      context.write(new Text(bestLocation.getSenseKey()), key);
    }
  }
}

