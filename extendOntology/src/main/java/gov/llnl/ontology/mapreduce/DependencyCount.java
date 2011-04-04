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

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;


/**
 * This map/reduce job counts the number of times each depedency path occurs for
 * a particular document source.  The resulting counts are stored on HDFS as
 * flat text files.  Each line in the resulting output will be keyed by a string
 * form of the dependency path and the path's frequently will be the value.
 *
 * @author Keith Stevens
 */
public class DependencyCount extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(DependencyCount.class);

  private static String source;

  /**
   * A {@link Configuration} for the Hadoop and HBase setup.
   */
  private HBaseConfiguration conf;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new DependencyCount(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("usage: java DependencyCount source");
      return 1;
    }

    conf = new HBaseConfiguration();

    source = args[0];
    try {
      // Run the first map/reduce job.
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Count Dependency Paths");

      // Setup the mapper.
      job.setJarByClass(DependencyCount.class);

      // Add a scanner that requests the requested dependency count column.
      Scan scan = new Scan();
      scan.addColumn(WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF.getBytes(),
                     source.getBytes());

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(WordNetEvidenceSchema.tableName, 
                                            scan,
                                            DependencyCountMap.class, 
                                            Text.class,
                                            IntWritable.class, 
                                            job);

      // Setup the reducer.
      job.setReducerClass(DependencyCountReducer.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      TextOutputFormat.setOutputPath(job, new Path("/data/dependencyCount"));
      job.setNumReduceTasks(24);

      Configuration jobConf = job.getConfiguration();

      // Run the first job.
      LOG.info("Counting dependnecy paths.");
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
   * This {@link TableMapper} emits the number of times each dependency path
   * occurs for a each noun pair.
   */
  public static class DependencyCountMap
      extends TableMapper<Text, IntWritable> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("Counting paths", "seen row").increment(1);

      // Get the dependency path counts for this row.  Skip rows lacking counts.
      Map<String, Integer> pathCounts = 
        WordNetEvidenceSchema.getDependencyPaths(row, "NYT");
      if (pathCounts == null || pathCounts.size() == 0) {
        context.getCounter("Counting paths", "Skipping row").increment(1);
        return;
      }

      // Iterate through each of the paths for this noun pair and store the
      // number of occurances into the feature vector.
      for (Map.Entry<String, Integer> pathAndCount : pathCounts.entrySet()) {
        context.write(
            new Text(pathAndCount.getKey()),
            new IntWritable(pathAndCount.getValue()));
      }
    }
  }

  /**
   * This {@link Reducer} emits a line for each dependency path, with the number
   * of times that the path occurs as the value.
   */
  public static class DependencyCountReducer 
      extends Reducer<Text, IntWritable, Text, IntWritable> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void reduce(Text key, Iterable<IntWritable> values, Context context)
        throws IOException, InterruptedException {
      int pathCount = 0;
      for (IntWritable value : values)
        pathCount += value.get();
      context.write(key, new IntWritable(pathCount));
    }
  }
}
