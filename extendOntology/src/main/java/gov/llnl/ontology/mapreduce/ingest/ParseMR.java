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

import gov.llnl.ontology.text.parse.Parser;
import gov.llnl.ontology.text.Sentence;

import gov.llnl.ontology.util.MRArgOptions;

import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CoNLLDepParentIndexAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;

import java.io.IOException;

import java.util.Iterator;
import java.util.List;


/**
 * This Map Reduce job iterates over rows in a {@link CorpusTable} and
 * produces a dependency parse tree for each sentence in each document.  These
 * parse trees are then storred as an annotation in the {@link CorpusTable}.
 *
 * </p> 
 *
 * This class requires that the following types of objects be specified by the
 * command line:
 * <ul>
 *   <li>{@link CorpusTable}: Controls access to the document table.</li>
 *   <li>{@link Parser}: Parses sentences in the {@link CorpusTable}.</li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class ParseMR extends CorpusTableMR {

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(ParseMR.class);

    /**
     * The configuration key for setting the {@link Tokenizer}.
     */
    public static String PARSER =
        CONF_PREFIX + ".parser";

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new ParseMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('p', "parser",
                          "Specifies the Parser to use for " +
                          "dependency parsing sentences.",
                          true, "CLASSNAME", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate("", "", ParseMR.class, 0, 'p');
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options, 
                                      Configuration conf) {
        conf.set(PARSER, options.getStringOption('p'));
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return ParseMapper.class;
    }

    /**
     * This {@link TableMapper} does all of the work.
     */
    public static class ParseMapper 
            extends TableMapper<ImmutableBytesWritable, Put> {

        /**
         * The {@link CorpusTable} that dictates the structure of the table
         * containing a corpus.
         */
        private CorpusTable table;

        /**
         * The {@link Parser} responsible for dependency parsing sentences.
         */
        private Parser parser;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            conf.set("mapred.map.child.java.opts", "-Xmx8g");
            conf.set("mapred.tasktracker.map.tasks.maximum", "1");
            table = ReflectionUtil.getObjectInstance(conf.get(TABLE));
            table.table();
            parser = ReflectionUtil.getObjectInstance(conf.get(PARSER));
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

            // Iterate over each sentence in the document for this row.  Add the
            // dependency parse annotations to each token in the sentence
            // annotation.
            List<Sentence> sentences = table.sentences(row);

            // Skip any documents without sentences.
            if (sentences == null)
                return;

            for (Sentence sentence : sentences) {
                // Skip any sentences which have already been parsed.  We can
                // detect this simply by trying to build a dependency parse tree
                // for each sentence and checking the length.  The non parsed
                // sentences always have a tree length of 0.
                if (sentence.dependencyParseTree().length > 0)
                    continue;

                LOG.info("Parseing sentence of length: " + 
                         sentence.taggedTokens().length);
                // Get the dependency parse tree.
                String parsedSentence = parser.parseText(
                        null, sentence.taggedTokens());
                context.getCounter("ParseMR", "Parsed Sentence").increment(1);
                context.setStatus("DON'T KILL MEEEEEEEEE");

                // Split the parse tree into each line for each token and add
                // the parent node index and the relationship as an annotation
                // to the relevant token annotation.
                Iterator<Annotation> tokens = sentence.iterator();
                for (String line : parsedSentence.split("\\n+")) {
                    Annotation token = tokens.next();
                    String[] toks = line.split("\\s+");

                    token.set(CoNLLDepParentIndexAnnotation.class,
                              Integer.parseInt(toks[6]));
                    token.set(CoNLLDepTypeAnnotation.class, toks[7]);
                }
            }

            // Add the list of Sentence annotations.
            table.put(key, sentences);
            context.getCounter("ParseMR", "Annotation").increment(1);
        }

        /**
         * {@inheritDoc}
         */
        protected void cleanup(Context context) {
            table.close();
        }
    }
}
