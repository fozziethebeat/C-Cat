/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
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

package gov.llnl.ontology.mapreduce.ingest;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;

import gov.llnl.ontology.text.Sentence;

import gov.llnl.ontology.util.MRArgOptions;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SpanAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.IntPair;

import edu.ucla.sspace.util.ReflectionUtil;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.util.Span;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;


/**
 * This Map Reduce job iterates over rows in a {@link CorpusTable} and
 * applies sentence spans, token spans, and part of speech tags to every
 * element in the raw text document.
 *
 * </p> 
 *
 * This class requires that the following types of objects be specified by the
 * command line:
 * <ul>
 *   <li>{@link CorpusTable}: Controls access to the document table.</li>
 *   <li>{@link SentenceDetector}: Splits documents up in to a series of
 *       sentences.</li>
 *   <li>{@link Tokenizer}: Tokenizes words in a sentence.</li>
 *   <li>{@link POSTagger}: Applies Part of Speech tags to words in a
 *       sentence.</li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class IngestCorpusMR extends CorpusTableMR {

    /**
     * The configuration key for setting the {@link Tokenizer}.
     */
    public static String TAGGER =
        CONF_PREFIX + ".tagger";

    /**
     * The configuration key for setting the {@link SentenceDetector}.
     */
    public static String SENTENCE_DETECTOR =
        CONF_PREFIX + ".sentencer";

    /**
     * The configuration key for setting the {@link Tokenizer}.
     */
    public static String TOKENIZER =
        CONF_PREFIX + ".tokenizer";

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new IngestCorpusMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('s', "sentenceDetector",
                          "Specifies the SentenceDetector to use for " +
                          "splitting sentences.",
                          true, "CLASSNAME", "Required");
        options.addOption('t', "tokenizer",
                          "Specifies the Tokenizer to use for " +
                          "splitting tokens.",
                          true, "CLASSNAME", "Required");
        options.addOption('p', "posTagger",
                          "Specifies the POSTagger to use for " +
                          "tagging tokens with pos.",
                          true, "CLASSNAME", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate("", "", IngestCorpusMR.class,
                         0, 's', 't', 'p');
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options, 
                                      Configuration conf) {
        conf.set(TOKENIZER, options.getStringOption('t'));
        conf.set(TAGGER, options.getStringOption('p'));
        conf.set(SENTENCE_DETECTOR, options.getStringOption('s'));
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return IngestCorpusMapper.class;
    }

    /**
     * This {@link TableMapper} iterates over rows in a {@link CorpusTable} and
     * applies sentence spans, token spans, and part of speech tags to every
     * element in the raw text document.
     */
    public static class IngestCorpusMapper
            extends TableMapper<ImmutableBytesWritable, Put> {

        /**
         * The {@link CorpusTable} that dictates the structure of the table
         * containing a corpus.
         */
        private CorpusTable table;

        /**
         * The {@link SentenceDetector} responsible for splitting sentences in a
         * document.
         */
        private SentenceDetector sentenceDetector;

        /**
         * The {@link Tokenizer} responsible for splitting tokens in a sentence.
         */
        private Tokenizer tokenizer;

        /**
         * The {@link POSTagger} responsible for applying part of speech tags to
         * each token.
         */
        private POSTagger tagger;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            sentenceDetector = ReflectionUtil.getObjectInstance(
                    conf.get(SENTENCE_DETECTOR));;
            tokenizer = ReflectionUtil.getObjectInstance(conf.get(TOKENIZER));
            tagger = ReflectionUtil.getObjectInstance(conf.get(TAGGER));
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context) {
            // Reject any rows that should not be processed.
            if (!table.shouldProcessRow(row))
                return;

            // Extract the document text for easeier use.
            String docText = table.text(row);

            // Skip any rows that have no raw text.
            if (docText == null)
                return;

            // Extract the sentence spans found within this document.  For each
            // sentence, we will add a Sentence annotation and then add token
            // level annotations.
            List<Sentence> sentenceAnnotations = new ArrayList<Sentence>();
            for (Span sentSpan : sentenceDetector.sentPosDetect(docText)) {
                // Get the indices for easy use.
                int start = sentSpan.getStart();
                int end = sentSpan.getEnd();
                String sentence = docText.substring(start, end);

                // Extract the token spans found within this sentence and do
                // part of speech tagging for each word in the sentence.
                Span[] tokSpans = tokenizer.tokenizePos(sentence);
                String[] tokens = tokenizer.tokenize(sentence);
                String[] pos = tagger.tag(tokens);

                // Create the sentence level annotation.
                Sentence sentAnnotation = new Sentence(start, end, pos.length);

                // Iterate through each word and create a single annotation for
                // this object.
                for (int i = 0; i < tokSpans.length; ++i) {
                    // Extract the start and end indices for easier use.
                    start = tokSpans[i].getStart();
                    end = tokSpans[i].getEnd();

                    // Create the token annotation and add it to the sentence.
                    Annotation wordAnnotation = new Annotation(tokens[i]);
                    wordAnnotation.set(PartOfSpeechAnnotation.class, pos[i]);
                    wordAnnotation.set(SpanAnnotation.class,
                                       new IntPair(start, end));
                    sentAnnotation.addAnnotation(i, wordAnnotation);
                }
                sentenceAnnotations.add(sentAnnotation);

                context.getCounter("IngestCorpusMR", "Sentences").increment(1);
            }

            // Add the list of Sentence annotations.
            table.put(key, sentenceAnnotations);
            context.getCounter("IngestCorpusMR", "Annotations").increment(1);
        }

        /**
         * {@inheritDoc}
         */
        protected void cleanup(Context context) {
            table.close();
        }
    }
}
