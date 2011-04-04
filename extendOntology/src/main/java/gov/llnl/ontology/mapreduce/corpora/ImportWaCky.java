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

package gov.llnl.ontology.corpora;

import edu.ucla.sspace.util.Pair;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import trinidad.hbase.mapreduce.annotation.AnnotationUtils;
import trinidad.hbase.mapreduce.annotation.ReportProgressThread;

import trinidad.hbase.table.DocSchema;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.List;


/**
 * This Map/Reduce imports the dependency parsed forms of the <a
 * href="http://wacky.sslmit.unibo.it/doku.php?id=start">WaCky coprus</a> into
 * the {@link DocSchema} HBase table.  The parsed WaCky documents are stored in
 * large XML files where documents are embedded in "<text>" "</text>" tags.  All
 * of the sentences between the "text" are stored as a single row in the
 * document table.  Since the parsed forms contain tokenization, part of speech,
 * sentence, and dependency parse information, {@link Annotation}s for these
 * data sets will be created for each document.  Unfortunately, no paragraph
 * information is provided.
 * 
 * @author Keith Stevens
 */
public class ImportWaCky extends Configured implements Tool {

  /**
   * Initialize the logger for this map reduce job.
   */
  static final Log LOG = LogFactory.getLog(ImportWaCky.class);

  /**
   * The source of the WaCky documents.  Since there are several parsed WaCky
   * corpora, this should reflect the type of corpus being imported, suck as
   * ukWac or WaCkypedia.
   */
  public static final String SOURCE = "WaCkySource";

  /**
   * The configuration for the HBase tables and Hadoop.
   */
  private HBaseConfiguration conf;

  /**
   * Runs the map reduce job.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(), new ImportWaCky(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Setup and run the map reduce job.
   */
  public int run(String[] args) {
    // Check for proper arguments and emit a usage if needed.
    if (args.length != 2) {
      System.out.println("usage: java ImportWaCky <wackyDir> wackySource\n" +
          "  Imports a properly formatted WaCky corpus into the DocSchema " + 
          "table.  <wackyDir> should be an hdfs directory with gziped WaCky " +
          " files.  wackySouce should be a source name for the corpus.");
      System.exit(1);
    }


    // Get the configuration
    conf = new HBaseConfiguration();

    // Get the hdfs directory for the corpus and the corpus source name.
    String inputPath = args[0];
    conf.set(SOURCE, args[1]);

    // Important to switch spec exec off.  We don't want to have something
    // duplicated.
    conf.set("mapred.map.tasks.speculative.execution", "false");

    try {
      // Setup the job.
      Job job = null;
      LOG.info("Before map/reduce startup");
      job = new Job(conf, "Import WaCky");

      // Setup the mapper, jar, and import format class.
      job.setJarByClass(ImportWaCky.class);
      job.setMapperClass(ImportWaCkyMapper.class);
      job.setInputFormatClass(XmlInputFormat.class);

      // Setup the xml tags which limit the record.
      conf.set(XmlInputFormat.START_TAG_KEY, "<text");
      conf.set(XmlInputFormat.END_TAG_KEY, "<\\text>");

      // Set the path for the input file which contains a list of files to
      // operate over
      FileInputFormat.addInputPath(job, new Path(inputPath));

      // Setup the table reducer.
      TableMapReduceUtil.initTableReducerJob(
          DocSchema.tableName, IdentityTableReducer.class, job);
      job.setNumReduceTasks(0);

      // Run the job.
      LOG.info("Started " + DocSchema.tableName);
      job.waitForCompletion(true);
      LOG.info("After map/reduce completion");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   * The mapper for this job.  This mapper will read a WaCky document and
   * generate various {@link Annotation}s for it and store the entire content
   * into the {@link DocSchema} HBase table.
   */
  public static class ImportWaCkyMapper
      extends Mapper<LongWritable, Text, Text, Text> {

    /**
     * The format for storing the date at which a document is written to the
     * table.
     */
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The file type of the wacky corpus.
     */
    private static final String XML_MIME_TYPE = "text/xml";

    /**
     * A pointer to the {@link DocSchema} table.
     */
    private HTable docTable;

    /**
     * The source name of the corpus.
     */
    private String source;

    /**
     * The tag specifying the id attribute of each document.
     */
    private String idTag;


    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(Context context) {
      try {
        super.setup(context);

        // These shoould probably be read from the configuration.
        source = "WaCky";
        idTag = "id";

        docTable = DocSchema.getTable();

        context.getCounter("ImportWaCky", "setup").increment(1L);
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
     * {@inheritDoc}
     */
    @Override
    protected void cleanup(Context context)
        throws IOException, InterruptedException {
      if (docTable != null) {
        docTable.flushCommits();
        docTable.close();
      }
    }

    /**
     * Processes a single document and stores the raw text, part of speech
     * annotations, token annotations, sentence annotations, and dependency
     * parse tree annotations into the {@link DocSchema} table.
     *
     * @param mapKey The byte offset at which the record was read
     * @param document The raw xml text of the document
     * @param conext The current context of this map call.
     */
    @Override
    public void map(LongWritable mapKey, Text document, Context context)
        throws IOException, InterruptedException {
      // Get the ingest date for this document.
      long time = System.currentTimeMillis();
      String ingestDate = format.format(time);

      context.getCounter("ImportWaCky", "map").increment(1L);

      // Tokenize the document based on newlines.
      String[] lines = document.toString().split("\\n");

      // Analyze the first line to get the document id, i.e., it's url.
      String firstLine = lines[0];
      int idIndex = firstLine.indexOf(idTag);
      int quoteStartIndex = firstLine.indexOf("\"", idIndex) + 1;
      int quoteEndIndex = firstLine.indexOf("\"", quoteStartIndex);
      String documentId = firstLine.substring(quoteStartIndex, quoteEndIndex);

      // Setup the annotations.
      AnnotationSet posAnnots = new AnnotationSet("pos");
      AnnotationSet sentAnnots = new AnnotationSet("sentence");
      AnnotationSet depAnnots = new AnnotationSet("dep");
      AnnotationSet tokAnnots = new AnnotationSet("token");

      // Setup lists that are needed to properly process the dependency parse
      // annotations.  Since the annotations need a forward pointer specifying
      // the offset of it's parent, we have to store the dependency parse
      // information in these lists and then empty the lists into the annotation
      // once the sentence is complete.
      List<Pair<Integer>> termOffsets = new ArrayList<Pair<Integer>>();
      List<Integer> termParents = new ArrayList<Integer>();
      List<String> termRelations = new ArrayList<String>();

      // Initialize character offsets into the document.
      int offset = 0;
      int prevSentOffset = 0;

      // Setup the builder for the raw text.
      StringBuilder rawText = new StringBuilder();

      // Process each content line.  The first line contains the <text tag and
      // attribute information, and the last line contains the end </text> tag.
      for (int i = 1; i < lines.length - 1; ++i) {
        String line = lines[0];

        // If the line is the beginning of a sentence, do nothing.
        if (line.startsWith("<s>"))
          continue;
        else if (line.startsWith("</s>")) {
          // If the line ends the sentence, add the sentence annotation and the
          // dependency parse annotations for the finished sentence.
          sentAnnots.add(new Annotation(
                sentAnnots.size(), prevSentOffset, offset, "sentence_split"));
          prevSentOffset = offset;

          // Analyze each parent offset from the finished sentence to create an
          // annotation.
          for (int j = 0; j < termOffsets.size(); ++j) {
            if (termParents.get(i) == -1)
              continue;

            // Create the annotation.
            Annotation depAnnot = new Annotation(depAnnots.size(),
                termOffsets.get(j).x, termOffsets.get(j).y, termRelations.get(j));

            // Get the parent id and it's character offsets.
            int parentId = termParents.get(j);
            Pair<Integer> parentOffsetPair = termOffsets.get(parentId);
            String parentOffset = parentOffsetPair.x + "," + parentOffsetPair.y;

            // Set the parent link.
            depAnnot.setAttribute("GOV", parentOffset);

            // Add the annotation.
            depAnnots.add(depAnnot);
          }

          // Clear the lists so that the next sentence has fresh lists.
          termOffsets.clear();
          termParents.clear();
          termRelations.clear();

          continue;
        }

        // Tokenize the line.
        String[] tokens = line.split("\\s+");

        // The raw term is the first token.
        rawText.append(tokens[0]).append(" ");

        // Get the start offset for the token.
        int tokStart = offset;

        // Add in the length of the token itself.
        offset += tokens[0].length();

        // Get the end offset for the token.
        int tokEnd = offset;

        // One for the space.
        offset++;

        // Add token and part of speech tag annotations for the current token.
        posAnnots.add(new Annotation(
              posAnnots.size(), tokStart, tokEnd, tokens[2]));
        tokAnnots.add(new Annotation(
              posAnnots.size(), tokStart, tokEnd, "token"));


        // Add the relevant dependency parse information into the lists.
        termOffsets.add(new Pair<Integer>(tokStart, tokEnd));
        termParents.add(Integer.parseInt(tokens[4]) - 1);
        termRelations.add(tokens[5]);
      }

      String docText = rawText.toString();

      // Create the put.
      Put put = new Put(DigestUtils.shaHex(documentId).getBytes());

      // Put the meta data into the table.
      DocSchema.add(put, DocSchema.srcCF, DocSchema.srcName, source);
      DocSchema.add(put, DocSchema.srcCF, DocSchema.srcId, documentId);
      DocSchema.add(put, DocSchema.textCF, DocSchema.textType, XML_MIME_TYPE);
      DocSchema.add(put, DocSchema.textCF, DocSchema.textOrig, docText);
      DocSchema.add(put, DocSchema.metaCF, "ingestDate", ingestDate);

      // Put the annotations into the table.
      DocSchema.add(put, DocSchema.annotationsCF, DocSchema.annotationsSentence,
                    AnnotationUtils.getAnnotationStr(sentAnnots));
      DocSchema.add(put, DocSchema.annotationsCF, DocSchema.annotationsPOS,
                    AnnotationUtils.getAnnotationStr(posAnnots));
      DocSchema.add(put, DocSchema.annotationsCF, DocSchema.annotationsToken,
                    AnnotationUtils.getAnnotationStr(tokAnnots));
      DocSchema.add(put, DocSchema.annotationsCF, DocSchema.annotationsDep,
                    AnnotationUtils.getAnnotationStr(depAnnots));

      // Finalize the put.
      docTable.put(put);
      context.getCounter("ImportWaCky", "add").increment(1L);
    }
  }
}
