/*
 * Copyright 2010 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.parser.MaltParser;
import edu.ucla.sspace.parser.Parser;
import edu.ucla.sspace.parser.StanfordParser;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

import java.util.Collections;
import java.util.Iterator;


/**
 * A map reduce version of the SemEval data set cleaner.  This class is intended
 * for parsing large SemEval data sets.  If you wish to simply remove XML
 * markup, please use the {@link SemEvalCleaner}.  This executable will create a
 * new map reduce job.  Each mapper will have an instance of a {@link Parser}.
 * The number of mappers generated depends on the number of xml files stored on
 * HDFS, where one mapper is created per file.  A SemEval iterator will be used
 * to extract contexts from the xml files, and each context will be parsed and
 * stored back on HDFS.
 *
 * @author Keith Stevens
 */
public class MRSemEval2010Cleaner extends Configured implements Tool {

  public enum CorpusType {
    SEMEVAL_2010_TRAIN,
    SEMEVAL_2010_TEST,
    SENSEEVAL_2007,
  }

  /**
   * The parser model to use for contexts.
   */
  private static String parserType;

  /**
   * Set to true if the xml files are in the SemEval2010 train format.
   */
  private static CorpusType corpusType;

  /**
   * Runs the map reduce job.
   */
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new MRSemEval2010Cleaner(), args);
  }

  public int run(String[] args) throws Exception {
      // Add and parser command line options.
      ArgOptions options = new ArgOptions();
      options.addOption('p', "parser",
                        "Specifie the parser that should be applied to " +
                        "each context. (Default: None)",
                         true, "malt|stanford", "Optional");
      options.addOption('t', "testOrTrain",
                        "Specifies whether or not the input xml files are " +
                        "in the SemEval2010 test format or the SemEval " +
                        "2010 train format.  (Default: train)",
                        true, "test|train", "Optional");
                        
      options.parseOptions(args);

      corpusType = CorpusType.valueOf(
          options.getStringOption('t').toUpperCase());

      // Determine the parser type.
      parserType = (options.hasOption('p'))
          ? options.getStringOption('p')
          : "stanford";

      // Create a new job and configuration.
      Configuration conf = new Configuration();
      Job job = new Job(conf, "Parse SemEval corpus");

      // Setup the mapper.
      job.setJarByClass(MRSemEval2010Cleaner.class);
      job.setMapperClass(SemEvalMapper.class);

      // Setup an identity reducer.
      job.setReducerClass(Reducer.class);
      job.setNumReduceTasks(24);

      // The input format will be based on a SemEval iterator.  The input
      // directory must be specified on the command line.
      job.setInputFormatClass(SemEvalInputFormat.class);
      FileInputFormat.addInputPath(job, new Path(
            options.getPositionalArg(0)));

      // The output directory must be specified on the command line.
      job.setOutputFormatClass(TextOutputFormat.class);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(Text.class);
      TextOutputFormat.setOutputPath(job, new Path(
            options.getPositionalArg(1)));

      // Run the first job.
      job.waitForCompletion(true);

      return 0;
  }

  /**
   * A simple mapper that reads in a SemEval context and parses it.
   */
  public static class SemEvalMapper
      extends Mapper<LongWritable, Text, Text, Text> {

    /**
     * The parser.
     */
    private Parser parser;

    /**
     * Creates a new instance of the parser.
     */
    protected void setup(Context context) 
        throws IOException, InterruptedException {
      parser = null;
      parser = new StanfordParser(StanfordParser.PARSER_MODEL, true);
    }

    /**
     * Parses the string context in {@code value}.
     */
    public void map(LongWritable key, Text value, Context context)
        throws IOException, InterruptedException {
        // Split the context, in case multiple contexts were generated from one
        // base SemEval context.  The header will always be the first line.
        String[] lines = value.toString().split("\n");
        String header = "\n" + lines[0] + "\n";

        // Parse each of the context generated.
        for (int i = 1; i < lines.length; ++i) {
          String text = (parser != null) 
              ? parser.parseText("", lines[i])
              : lines[i];
          context.write(new Text(header), new Text(text));
        }
    }
  }

  /**
   * A simple {@link TextInputFormat} that returns a {@link
   * SemEvalRecordReader}.
   */
  public static class SemEvalInputFormat extends TextInputFormat {

    /**
     * {@inheritDoc}
     */
    public RecordReader<LongWritable,Text> createRecordReader(
      InputSplit inputSplit,
      TaskAttemptContext context) {
      return new SemEvalRecordReader();
    }
  }
  
  /**
   * A {@link RecordReader} that returns string context extracted from xml
   * SemEval files.  Each record reader processes only one xml file.
   */
  public static class SemEvalRecordReader
      extends RecordReader<LongWritable,Text> {

    /**
     * The iterator over the SemEval xml file.
     */
    private Iterator<String> iter;

    /**
     * A count of how many context have been processed.  The context count
     * serves as the key.
     */
    private int count;

    /**
     * The current key value to be returned for a record.
     */
    private LongWritable key;

    /**
     * The current value to be returned for a record.
     */
    private Text value;

    /**
     * {@inheritDoc}
     */
    public void initialize(InputSplit isplit, TaskAttemptContext context) 
        throws IOException {
      Configuration conf = context.getConfiguration();
      FileSplit split = (FileSplit) isplit;
      Path file = split.getPath();

      FileSystem fs = file.getFileSystem(conf);

      System.out.println("Creating a new iterator");
      switch (corpusType) {
        case SEMEVAL_2010_TRAIN:
          iter = new SemEval2010TrainIterator(
              fs.open(file), true, "", new EnglishStemmer());
          break;
        case SEMEVAL_2010_TEST:
          iter = new SemEval2010TestIterator(
              fs.open(file), true, "", new EnglishStemmer());
          break;
        case SENSEEVAL_2007:
          iter = new SenseEval2007Iterator(
                fs.open(file), true, "");
          break;
        default:
          throw new IllegalArgumentException("Invalid corpus type");
      }

      count = 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean nextKeyValue() {
      if (iter.hasNext()) {
        key = new LongWritable(count++);
        value = new Text(iter.next());
        return true;
      } else {
        key = null;
        value = null;
        return false;
      }
    }
    
    /**
     * {@inheritDoc}
     */
    public LongWritable getCurrentKey() {
      return key;
    }

    /**
     * {@inheritDoc}
     */
    public Text getCurrentValue() {
      return value;
    }
    
    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
    }
    
    /**
     * {@inheritDoc}
     */
    public float getProgress() throws IOException {
      return (value == null) ? 1.0f : .5f;
    }
  }
}
