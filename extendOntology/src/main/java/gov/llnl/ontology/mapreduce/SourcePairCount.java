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

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

import java.util.Set;


/**
 * This Map/Reduce executable counts the number of unique noun pairs associated
 * with each document source.
 *
 * @author Keith Stevens
 */
public class SourcePairCount extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(SourcePairCount.class);

  /**
   * The {@link Configuration} for hadoop and hbase.
   */
  private HBaseConfiguration conf;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new SourcePairCount(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    // Vadliate the command line arguments.
    if (args.length != 0) {
      System.out.println("usage: java SourcePairCount");
      return 1;
    }

    conf = new HBaseConfiguration();

    try {
      // Run the first map/reduce job.
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Source Pair Counts");

      // Setup the mapper.
      job.setJarByClass(SourcePairCount.class);

      // Add a scanner that requests the requested similarity column.
      Scan scan = new Scan();
      scan.addFamily(WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF.getBytes());

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(WordNetEvidenceSchema.tableName, 
                                            scan,
                                            SourcePairCountMap.class, 
                                            Text.class,
                                            IntWritable.class, 
                                            job);

      // Setup the reducer.
      job.setCombinerClass(IntSumReducer.class);
      job.setReducerClass(IntSumReducer.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      TextOutputFormat.setOutputPath(
          job, new Path("/data/sourcePairCount"));
      job.setNumReduceTasks(24);

      // Run the first job.
      LOG.info("Counting noun pairs.");
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
   * This mapper emits {@code 1} evertime a noun pair has evidence for a given
   * document source.
   */
  public static class SourcePairCountMap
      extends TableMapper<Text, IntWritable> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("Counting paths", "seen row").increment(1);

      // Get the set of document sources associated with this noun pair.
      Set<byte[]> sources = row.getFamilyMap(
          WordNetEvidenceSchema.DEPENDENCY_FEATURE_CF.getBytes()).keySet();
      for (byte[] source : sources) {
        // Skip any sources lacking evidence.
        if (WordNetEvidenceSchema.getDependencyPaths(
              row, new String(source)) == null)
          return;

        // Emit the noun pair's occurrence.
        context.write(new Text(source), new IntWritable(1));
      }
    }
  }
}