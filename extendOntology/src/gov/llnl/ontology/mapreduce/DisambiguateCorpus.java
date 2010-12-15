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

import gov.llnl.ontology.pagerank.ExtendedList;
import gov.llnl.ontology.pagerank.ExtendedMap;
import gov.llnl.ontology.pagerank.SynsetPagerank;

import gov.llnl.ontology.wordnet.Attribute;
import gov.llnl.ontology.wordnet.BaseSynset;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import edu.ucla.sspace.util.Pair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;

import org.apache.hadoop.hbase.HBaseConfiguration;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import reconcile.data.Annotation;
import reconcile.data.AnnotationSet;

import trinidad.hbase.mapreduce.annotation.AnnotationUtils;

import trinidad.hbase.table.DocSchema;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This Map/Reduce diambiguates a corpus with respect to a particular word net
 * version.  If a corpus source is specified, this will only disambiguate
 * documents from that source, otherwise all documents will be diambiguated.
 * Words are disambiguated by using the "Personalizing PageRank for Word Sense
 * Disambiguation" algorithm, which is done by using the {@link SynsetPagerank}
 * implementation.  For a simple example of this algorithm, see {@link
 * gov.llnl.ontology.pagerank.PersonalizedPageRankWSD}.  
 *
 * </p>
 *
 * Briefly, the algorithm utilizes the entire structure of the WordNet graph,
 * including all relations and parts of speech.  Given a context that needs to
 * be disambiguated, a node is created for each context word that has a valid
 * mapping in the {@link WordNetCorpusReader}.  These new context nodes are
 * connected to each of the possible {@link Synset}s for the context word.  The
 * teleportation probabilities, i.e., the initial page rank scores, for each
 * original WordNet {@link Synset} is set to 0, and the probabilities for the
 * new context nodes are given equal non-zero probabilities.  This
 * "personalizes" the page rank computation such that the context nodes act as
 * sources for the {@link Synset} graph, and viable senses for all words that
 * are highly connected to each other will be given a higher page rank score.
 * For each context word, the sense with the highest page rank score is chosen.
 *
 * </p>
 *
 * This Map/Reduce stores all word senses as {@link Annotation}s in the {@link
 * DocSchema} HBase table.  These are stored under the "annotations" column
 * family.  Column names will begin with "wsdtags" and be followed by the
 * WordNet version used to perform the disambiguation.
 *  
 * @author Keith Stevens
 */
public class DisambiguateCorpus extends Configured implements Tool {

  /**
   *The logger for this class.
   */
  private static final Log LOG =
    LogFactory.getLog(DisambiguateCorpus.class);

  /**
   * The configuration that specifies how HBase and Hadoop are set up.
   */
  private HBaseConfiguration conf;

  /**
   * The string describing the word net version used to perform disambiguation.
   */
  private static String wsdQualifier;

  /**
   * Runs the map reducer.
   */
  public static void main(String[] args) {
    try {
      ToolRunner.run(new Configuration(),
                     new DisambiguateCorpus(), args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println(
          "usage: DisambiguateCorpus wordNetVersion [source]\n"+ "  Performs " +
          "Personalized PageRank over all documents in the specified source, " +
          "or all documents in the DocSchema table.  Word senses will be " +
          "stored under the annotations:wsdtag:wordNetVersion column.  The " +
          "wordnet dictionary file is assumed to be located within the jar " +
          "for this map reduce under the directory /dict");
      return 1;
    }

    // Setup the configuration.
    conf = new HBaseConfiguration();

    // Get the version and source for this map reduce job.
    wsdQualifier = "wsdtags:" + args[0];
    String source = (args.length == 2) ? args[1] : null;

    try {
      LOG.info("Preparing map/reduce system.");
      Job job = new Job(conf, "Disambiguate terms in a corpus.");

      // Setup the mapper.
      job.setJarByClass(DisambiguateCorpus.class);

      // Add a scanner that requests the text and annotation column families.
      Scan scan = new Scan();
      scan.addFamily(DocSchema.textCF.getBytes());
      scan.addColumn(DocSchema.annotationsCF.getBytes(),
                     DocSchema.annotationsPOS.getBytes());

      // Set a filter, if a source is specified, so that only the requested
      // source is specified;
      scan.addColumn(DocSchema.srcCF.getBytes(), DocSchema.srcName.getBytes());
      if (source != null)
        scan.setFilter(new SingleColumnValueFilter(
            DocSchema.srcCF.getBytes(), DocSchema.srcName.getBytes(),
            CompareOp.EQUAL, source.getBytes()));

      // Setup the mapper.
      TableMapReduceUtil.initTableMapperJob(DocSchema.tableName, scan,
                                            DisambiguationMapper.class, 
                                            Text.class,
                                            Put.class,
                                            job);

      // Setup the reducer.
      job.setNumReduceTasks(0);

      // Run the first job.
      LOG.info("Disambiguating All words in a corpus");
      job.waitForCompletion(true);
      LOG.info("Disambiguation complete");
    }
    catch (Exception e) {
      e.printStackTrace();
      return 1;
    }

    return 0;
  }

  /**
   * The Mapper that disambiguates a document from the {@link DocSchema} table.
   */
  public static class DisambiguationMapper extends TableMapper<Text, Put> {

    /**
     * The relation name for the connection between term {@link Synset}s and
     * their potential word senses.
     */
    private static final String RELATED = "related";

    /**
     * A pointer to the {@link DocSchema} HBase table.
     */
    private HTable docTable;

    /**
     * The reader for word net.
     */
    private WordNetCorpusReader wordnet;

    /**
     * The list of {@link Synset}s that are originally in the word net
     * dictionary.
     */
    private List<Synset> synsetList;

    /**
     * The mapping from {@link Synset}s to their indices in {@code synsetList}.
     */
    private Map<Synset, Integer> synsetMap;

    /**
      * Initialize the mapper.  The {@link WordNetCorpusReader} is setup, along
      * with the list of {@link Sysnet} and the mapping from {@link Synset}s to
      * their indices.
      */
    @Override
    public void setup(Context context) {
      try {
        super.setup(context);
        docTable = DocSchema.getTable();
        wordnet = WordNetCorpusReader.initialize("/dict", true);

        // Setup the list of synsets and the mapping from each synset to their
        // index.
        synsetList = new ArrayList<Synset>();
        synsetMap = new HashMap<Synset, Integer>();
        int synsetIndex = 0;
        for (String lemma : wordnet.wordnetTerms()) {
            Synset[] synsets = wordnet.getSynsets(lemma);
          for (int s = 0; s < synsets.length; ++s,++synsetIndex) {
            synsetList.add(synsets[s]);
            synsetMap.put(synsets[s], synsetIndex);
          }
        }

        // Create a transition attribute for each synset from the original
        // graph.  We only have to do this once for the core synsets since other
        // graphs will simply extend this network and only read from core
        // synsets.
        SynsetPagerank.setupTransitionAttributes(synsetList, synsetMap);
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
     * Disambiguates a single document from the {@link DocSchema} table.  The
     * terms in the document are disambiguated through a sliding window of 10
     * words.  10 words in the document are disamgibuated at a time.  Since the
     * Personalized PageRank algorithm benefits from larger contexts, the 10
     * words before and after the focus context are included in the page rank
     * graph.  This sliding window should provide a nice balance between having
     * a suitably sized context and not redoing too much work.
     * 
     * </p>
     *
     * Each context is disambiguated by creating a new {@link Synset} for each
     * context word mapped by WordNet.  The new {@link Synset}s will point to
     * their possible {@link Sysnet}s in the real WordNet dictionary.  All of
     * the initial page rank weights will be evenly distributed to these new
     * context {@link Synset}s.  The correct sense for each context word is
     * assumed to be the word sense that has the highest final page rank score.
     * These sense tags will be stored as an {@link AnnotationSet} in the same
     * table.
     *
     * @param key The unique key for the document being disambiguated
     * @param row The current row in the HBase table containing a document and
     *        part of speech annotations
     * @param context The context defining this map call.
     */
    @Override
    public void map(ImmutableBytesWritable key, Result row, Context context)
        throws IOException, InterruptedException {
      context.getCounter("disambiguating", "seen row").increment(1);

      // Get the required annotations and document text.
      AnnotationSet posAnnots = DocSchema.getAnnotationSet(
          row, DocSchema.annotationsPOS);
      String docText = DocSchema.getRawText(row);

      // Create the annotation set used to store the sense tags.
      AnnotationSet tagAnnots = new AnnotationSet("WordSenseTags");

      // For each window of 10 words, disambiguate all content words.
      int startIndex = synsetList.size();
      for (int i = 0; i < posAnnots.size(); i += 10) {

        // Create the extended list and map so that the core sysnets are
        // unmodified.
        List<Synset> contextList = new ExtendedList<Synset>(synsetList);
        Map<Synset, Integer> contextMap =
          new ExtendedMap<Synset, Integer>(synsetMap);

        // Add the 10 previous context words to the graph, or as many prior 
        // words exist.
        for (int j = Math.max(0, i-10); j < i; ++j)
          addSynset(docText, posAnnots.get(j), contextList, contextMap);

        // Add the 10 context words that will be disambiguated in this
        // iteration.
        int toBeTaggedStart = contextList.size();
        for (int k = i; k < 10 && k < posAnnots.size(); ++k)
          addSynset(docText, posAnnots.get(k), contextList, contextMap);
        int toBeTaggedEnd = contextList.size();

        // Add the 10 proceeding context words to the graph, or a many further
        // words exist.
        int endStart = Math.min(i+10, posAnnots.size());
        for (int l = endStart; l < 10 && l < posAnnots.size(); ++l)
          addSynset(docText, posAnnots.get(l), contextList, contextMap);

        // Create the initial page rank weights for the context nodes.
        double numTerms = contextList.size() - startIndex;
        SparseDoubleVector sourceWeights = new CompactSparseVector();
        for (int j = startIndex; j < contextList.size(); ++j)
          sourceWeights.set(j, 1d/numTerms);

        // Run the page rank algorithm.
        SparseDoubleVector pageRanks = SynsetPagerank.computePageRank(
            contextList, sourceWeights, .85);

        // For each context term that is to be disambiguated in this iteration,
        // get the highest ranked sense.
        for (int k = toBeTaggedStart; k < toBeTaggedEnd; ++k)  {
          Synset termSynset = contextList.get(k);
          double bestScore = 0;
          Synset bestSense = null;

          // Get the potential senses.
          for (Synset sense : termSynset.getRelations(RELATED)) {
            // Store the best one.
            int index = contextMap.get(sense);
            double score = pageRanks.get(index);
            if (score >= bestScore) {
              bestScore = score;
              bestSense = sense;
            }
          }

          // Create an annotation for the sense.
          Pair<Integer> offset =
            (Pair<Integer>) termSynset.getAttribute("offset").object();
          tagAnnots.add(new Annotation(tagAnnots.size(), offset.x, offset.y, 
                bestSense.getName()));
        }
        context.getCounter("disambiguating", "handled window").increment(1);
      }

      // Store the annotation into the DocSchema table.
      Put put = new Put(key.get());
      DocSchema.add(put, DocSchema.annotationsCF, wsdQualifier, 
                    AnnotationUtils.getAnnotationStr(tagAnnots));
      docTable.put(put);
    }

    /**
     * Adds a word based {@link Synset} to the {@link Sysnet} graph.  The new
     * {@link Synset} will be placed at the end of {@code contextList} and given
     * the corresponding index for the {@code contextMap}.  
     *
     * @param docText The raw text of the document
     * @param posAnnot The annotation specifying the token of interest begins
     *        and ends, along with it's part of speech tag.
     * @param contextList The list of {@link Synset}s that form the WordNet
     *        graph
     * @param contextMap The mapping from {@link Synset}s in the WordNet graph
     *        to their indices in {@code contextList}
     */
    private void addSynset(String docText,
                           Annotation posAnnot,
                           List<Synset> contextList, 
                           Map<Synset, Integer> contextMap) {
      // Get the token of interest.
      String token = cleanToken(docText.substring(
          posAnnot.getStartOffset(), posAnnot.getEndOffset()));

      // Get the synsets for the token.  Limite the results to the known part of
      // speech for the token.  If the part of speech is one not handled by
      // WordNet just return.
      Synset[] synsets = null;
      if (posAnnot.getType().startsWith("NN"))
        synsets = wordnet.getSynsets(token, PartsOfSpeech.NOUN);
      else if (posAnnot.getType().startsWith("VB"))
        synsets = wordnet.getSynsets(token, PartsOfSpeech.VERB);
      else if (posAnnot.getType().startsWith("JJ"))
        synsets = wordnet.getSynsets(token, PartsOfSpeech.ADJECTIVE);
      else if (posAnnot.getType().startsWith("RB"))
        synsets = wordnet.getSynsets(token, PartsOfSpeech.ADVERB);
      else
        return;

      // Add the possible word senses to the term synset.
      BaseSynset termSynset = new BaseSynset(PartsOfSpeech.NOUN);
      for (Synset possibleSense : synsets)
        termSynset.addRelation(RELATED, possibleSense);

      // Add in an attribute the records the string offsets for the token
      // represented by this synset.  This is needed later on so that a correct
      // annotation can be created for the term, with the same offsets as the
      // pos tags.
      termSynset.setAttribute("offset", new OffsetAttribute(
          posAnnot.getStartOffset(), posAnnot.getEndOffset()));

      // Add the term synset to the list and map.
      synsetMap.put(termSynset, synsetList.size());
      synsetList.add(termSynset);

      // Setup the transition attribute for the page rank computation for this
      // synset.
      SynsetPagerank.setTransitionAttribute(termSynset, contextMap);
    }

    /**
     * Returns a cleaned version of a token.  This is needed since the
     * tokenzation of some documents includes random non alpha numeric
     * characters.
     */
    private String cleanToken(String term) {
      term = term.replaceAll("\"", " ");
      term = term.replaceAll("\'", " ");
      term = term.replaceAll("\\[", " ");
      term = term.replaceAll("\\]", " ");
      term = term.replaceAll("\\?", " ");
      term = term.replaceAll("\\.", " ");
      term = term.replaceAll("\\(", " ");
      term = term.replaceAll("\\)", " ");
      term = term.replaceAll("\\$", " ");
      term = term.replaceAll("\\^", " ");
      term = term.replaceAll("\\+", " ");
      term = term.replaceAll("%", " ");
      term = term.replaceAll(",", " ");
      term = term.replaceAll(":", " ");
      term = term.replaceAll("!", " ");
      term = term.replaceAll("=", " ");
      return term.trim();
    }

    /**
     * A simple attribute that stores the offsets at which a token is found in
     * the raw document text.
     */
    private static class OffsetAttribute implements Attribute<Pair<Integer>> {

      /**
       * The start offset for the token.
       */
      private int start;

      /**
       * The end offset for the token.
       */
      private int end;

      /**
       * Creates a new {@link OffsetAttribute} with the given start and end
       * offsets.
       */
      public OffsetAttribute(int start, int end) {
        this.start = start;
        this.end = end;
      }

      /**
       * A noop
       */
      public void merge(Attribute<Pair<Integer>> other) {
      }

      /**
       * {@inheritDoc}
       */
      public Pair<Integer> object() {
        return new Pair<Integer>(start, end);
      }
    }
  }
}
