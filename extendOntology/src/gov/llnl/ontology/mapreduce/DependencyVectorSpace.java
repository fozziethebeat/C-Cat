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

import com.google.common.collect.Maps;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;
import edu.ucla.sspace.common.VectorMapSemanticSpace;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.PointWiseMutualInformationTransform;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This Map/Reduce creates a {@link SemanticSpace} from dependency parsed
 * documents.  Word co-occurrences, based on paths in the dependency parses, are
 * done with a Map/Reduce.  The co-occurrence counts are then read into memory
 * and transformed using a {@link MatrixTransform}.  Finally, the semantic space
 * is serialized in a standard format.
 *
 * @author Keith Stevens
 */
public class DependencyVectorSpace extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(DependencyVectorSpace.class);

  private static final String WORD_LIST = "wordList";

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
                     new DependencyVectorSpace(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  @Override
  public int run(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println(
          "usage: DependencyVectorSpace " +
          "<termCount> <dvSpaceName.sspace> source"); 
      return 1;
    }

    // Set the document source.
    System.getProperties().setProperty(
        TrinidadDocumentReader.SOURCE_PROPERTY, args[2]);

    conf = new HBaseConfiguration();
    conf.set(WORD_LIST, args[0]);

    //if (formWordCounts() == 1) return 1;
    if (buildSSpace(args[1]) == 1) return 1;

    return 0;
  }

  /**
   * Creates the co-occurrence counts from the corpus.
   */
  public int formWordCounts() {
    try {
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Build Dependency Vector Space");

      // Setup the mapper.
      job.setJarByClass(DependencyVectorSpace.class);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      DocumentReader reader = new TrinidadDocumentReader();
      reader.setupScan(scan);

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(
          reader.getTableName(), scan, ExtractDependencyPathScoreMapper.class, 
          Text.class, Text.class, job);

      // Setup the reducer.
      job.setReducerClass(ExtractDependencyPathReducer.class);
      job.setOutputFormatClass(TextOutputFormat.class);
      TextOutputFormat.setOutputPath(job, new Path("/data/dvSpace"));
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

  /**
   * Creates the {@link SemanticSpace} from the computed co-occurrence counts.
   */
  public int buildSSpace(String sspaceName) {
    try {
      LOG.info("Building word space matrix");

      // Create the basis mapping for the word space.
      BasisMapping<String, String> basis = new StringBasisMapping();
      Map<String, SparseDoubleVector> wordSpace =
        new HashMap<String, SparseDoubleVector>();

      FileSystem fs = FileSystem.get(conf);

      Set<String> wordList = new HashSet<String>();
      BufferedReader br = new BufferedReader(new InputStreamReader(
            fs.open(new Path(conf.get(WORD_LIST)))));
      for (String line = null; (line = br.readLine()) != null; )
        wordList.add(line.trim());

      // Iterate through each file containing the co-occurrence counts and add a
      // semantic vector for each root term in the space.
      for (FileStatus file : fs.listStatus(new Path("/data/dvSpace"))) {
        // Skip any files that are actually directories.
        if (file.isDir())
          continue;

        // Open a reader for the file.
        br = new BufferedReader(new InputStreamReader(
              fs.open(file.getPath())));
        System.out.println("handling: " + file.getPath());
        // Create the feature vector for each word in the occurrence files.
        // Each word is given a dedicated line with the focus term as the first
        // token and each co-occurrence|score as proceeding tokens.
        for (String line = null; (line = br.readLine()) != null; ) {
          String[] tokens = line.split(";");
          String focusTerm = tokens[0].trim();
          if (!wordList.contains(focusTerm))
            continue;

          SparseDoubleVector vector = new CompactSparseVector();

          // Split each proceeding token.
          for (int i = 1; i < tokens.length; ++i) {
            // Skip tokens that have pipes in them, there shouldn't be any
            // but it's always possible.
            // TODO: Fix the trinidad pre-procesing so that tokens never have
            // extra white space.  This involves doing a lot more token
            // normalization such as stripping punctuation and possibly
            // stemming.
            String[] termAndScore = tokens[i].split("\\|");
            termAndScore[0] = termAndScore[0].trim();
            if (termAndScore.length != 2 || !wordList.contains(termAndScore[0]))
              continue;

            // Add the feature.
            double score = Double.parseDouble(termAndScore[1]);
            int index = basis.getDimension(termAndScore[0]);
            vector.set(index, score);
          }

          wordSpace.put(focusTerm, vector);
        }
      }

      LOG.info("Resizing word space matrix");
      // Since the number of dimensions is unknown during the first pass, we
      // have to create a subview of each vector that is capped at the known
      // number of dimensions. 
      List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
      for (Map.Entry<String, SparseDoubleVector> entry : wordSpace.entrySet()) {
        SparseDoubleVector vector = Vectors.subview(
            entry.getValue(), 0, basis.numDimensions());
        entry.setValue(vector);
        vectors.add(vector);
      }

      LOG.info("Transforming word space matrix");
      // Transform the matrix using the method of choice.
      Transform transform = new PointWiseMutualInformationTransform();
      transform.transform(Matrices.asSparseMatrix(vectors));

      LOG.info("Saving word space matrix");
      // Create a SemanticSpace from the transformed feature vectors and basis
      // mapping.  Then serialize it.
      SemanticSpace sspace = new VectorMapSemanticSpace<SparseDoubleVector>(
          wordSpace, "DependencyVectorSpace", basis.numDimensions());
      SemanticSpaceIO.save(sspace, new File(sspaceName),
                           SSpaceFormat.SPARSE_BINARY);

      LOG.info("Word space matrix Saved");
    } catch (Exception e) {
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
    extends Reducer<Text, Text, Text, Text> {

    /**
     * Initializes access to the Evidence Table.
     */
    @Override
    protected void setup(Context context) 
        throws IOException, InterruptedException {
      context.getCounter("populate table", "setup").increment(1);
    }

    /**
     * Determins the {@link HypernymStatus} and cousin relationship of each noun
     * pair and sums up the observed dependency paths.  
     */
    @Override
    protected void reduce(Text key,  Iterable<Text> values,  Context context) 
        throws IOException, InterruptedException {
      context.getCounter("count features", "row vector").increment(1);
      String term = key.toString();

      // Count the number of times that each dependency path occurs with the
      // given key.
      Map<String, Double> termScores = Maps.newHashMap();
      for (Text occurrence : values) {
        String[] termAndCount = occurrence .toString().split("\\|");

        // Ignore any occurrences that lack a valid count.
        if (termAndCount.length != 2 || termAndCount[1].length() == 0)
          continue;

        // Add the total score for this co-occurrence.
        double score = Double.parseDouble(termAndCount[1]);
        Double termScore = termScores.get(termAndCount[0]);
        termScores.put(termAndCount[0],
                       (termScore == null) ? score : termScore + score);
      }

      // Emit the total co-occurrence counts for the current term.
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, Double> entry : termScores.entrySet()) {
        sb.append(";").append(entry.getKey());
        sb.append("|").append(entry.getValue());
      }
      context.write(key, new Text(sb.toString()));
    }
  }
}
