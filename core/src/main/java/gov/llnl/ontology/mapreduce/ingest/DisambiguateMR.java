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
import gov.llnl.ontology.mapreduce.MRArgOptions;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.wsd.WordSenseDisambiguation;

import com.google.common.collect.Lists;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class DisambiguateMR extends CorpusTableMR{

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(DisambiguateMR.class);

    /**
     * The configuration key for setting the {@link WordSenseDisambiguation}
     * algorithm.
     */
    public static String WSD_ALG = CONF_PREFIX + ".wsdAlg";

    /**
     * The configuration key for setting the wordnet directory.
     */
    public static String WORDNET_DIR = CONF_PREFIX + ".wordnetDir";

    /**
     * Runs the {@link DisambiguateMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new DisambiguateMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('w', "wsdAlgorithm",
                          "Specifies the WordSenseDisambiguation algorithm " +
                          "to use for sentences",
                          true, "CLASSNAME", "Required");
        options.addOption('d', "wordnetDir",
                          "Specifies the directory in the existing " +
                          "classpath for wordnet dictionary files.",
                          true, "PATH", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate("", "", DisambiguateMR.class, 0, 'C', 'w', 'd');
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "DisambiguateMR";
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options, 
                                      Configuration conf) {
        conf.set(WSD_ALG, options.getStringOption('w'));
        conf.set(WORDNET_DIR, options.getStringOption('d'));
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return DisambiguateMapper.class;
    }

    /**
     * This {@link TableMapper} does all of the work.
     */
    public static class DisambiguateMapper 
            extends CorpusTableMR.CorpusTableMapper<ImmutableBytesWritable, Put> {

        /**
         * The {@link Disambiguater} responsible for dependency parsing
         * sentences.
         */
        private WordSenseDisambiguation wsdAlg;

        /**
         * The {@link OntologyReader} need to perform word sense disambiguation.
         */
        private OntologyReader wordnet;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf) {
            wordnet = WordNetCorpusReader.initialize(
                    conf.get(WORDNET_DIR), true);
            wsdAlg = ReflectionUtil.getObjectInstance(conf.get(WSD_ALG));
            wsdAlg.setup(wordnet);
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context) {
            context.setStatus("Disambiguating");
            // Reject any rows that should not be processed.
            if (!table.shouldProcessRow(row))
                return;

            List<Sentence> sentences = table.sentences(row);
            // Skip any documents without sentences.
            if (sentences == null)
                return;

            // Disambiguate every sentence in the document and store these word
            // senses in a hbase column based on the WSD algorithm's name.
            List<Sentence> disambiguatedSentence = Lists.newArrayList();
            for (Sentence sentence : sentences) {
                disambiguatedSentence.add(wsdAlg.disambiguate(sentence));
                context.getCounter("DisambiguateMR", "Sentence").increment(1);
            }

            table.putSenses(key, disambiguatedSentence, wsdAlg.toString());
            context.getCounter("DisambiguateMR", "Documents").increment(1);
        }

        /**
         * {@inheritDoc}
         */
        protected void cleanup(Context context) {
            table.close();
        }
    }
}
