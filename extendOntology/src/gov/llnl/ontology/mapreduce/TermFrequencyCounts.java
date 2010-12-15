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

import edu.ucla.sspace.dependency.DependencyTreeNode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;

import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import trinidad.hbase.mapreduce.annotation.ReportProgressThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class TermFrequencyCounts extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(TermFrequencyCounts.class);

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
                     new TermFrequencyCounts(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println(
          "usage: TermFrequencyCounts source"); 
      return 1;
    }

    // Set the document source.
    System.getProperties().setProperty(
        TrinidadDocumentReader.SOURCE_PROPERTY, args[0]);

    conf = new HBaseConfiguration();

    if (formWordCounts() == 1) return 1;

    return 0;
  }

  /**
   * Creates the co-occurrence counts from the corpus.
   */
  public int formWordCounts() {
    try {
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Count term occurrences");

      // Setup the mapper.
      job.setJarByClass(TermFrequencyCounts.class);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      DocumentReader reader = new TrinidadDocumentReader();
      reader.setupScan(scan);

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(
          reader.getTableName(), scan, TermFrequencyMapper.class, 
          Text.class, IntWritable.class, job);

      // Setup the reducer.
      job.setReducerClass(IntSumReducer.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      TextOutputFormat.setOutputPath(job, new Path("/data/sourceTermCounts"));
      job.setNumReduceTasks(24);

      // Run the first job.
      LOG.info("Extracting Dependency Paths.");
      job.waitForCompletion(true);
      LOG.info("Dependency Path Extraction complete");
    } catch (Exception e) {
      e.printStackTrace();
      return 1;
    }
    return 0;
  }

  public static class TermFrequencyMapper
      extends TableMapper<Text, IntWritable> {

    private DocumentReader reader;

    public void setup(Context context) {
      try {
        super.setup(context);
        reader = new TrinidadDocumentReader();
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

    @Override
    protected void map(ImmutableBytesWritable key, Result row, Context context)
      throws IOException, InterruptedException {
      String source = reader.getTextSource(row);
      context.getCounter("count terms", source).increment(1);
      DependencyTreeNode[] dependencyTree = reader.getDependencyTree(row);
      if (dependencyTree == null)
        return;

      for (DependencyTreeNode treeNode : dependencyTree)
        context.write(new Text(treeNode.word()), new IntWritable(1));
    }
  }
}
