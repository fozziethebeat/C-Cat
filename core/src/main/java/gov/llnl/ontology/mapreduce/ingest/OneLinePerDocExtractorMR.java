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
import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;

import com.google.common.collect.Sets;

import edu.stanford.nlp.pipeline.Annotation;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class OneLinePerDocExtractorMR extends CorpusTableMR {

    public static final String USE_HEADER =
        "gov.llnl.ontology.mapreduce.ingest.OneLinePerDocExtractorMR.header";

    public static final String ABOUT =
        "Extracts the raw tokenized text from a corpus and stores it to hdfs";

    public static final String MR_NAME = "OneLinePerDocExtractorMR";

    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(),
                       new OneLinePerDocExtractorMR(), args);
    }

    protected void addOptions(MRArgOptions options) {
        options.addOption('a', "acceptedWords",
                          "A file on hdfs that specifies words that are " +
                          "to be accepted",
                          true, "FILE", "Optional");
        options.addOption('h', "includeHeader",
                          "If set, the document key will be printed for " +
                          "each document",
                          false, null, "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", OneLinePerDocExtractorMR.class, 
                         1, 'C');
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "One line per doc extractor";
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return OneLinePerDocExtractorMRMapper.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperKeyClass() {
        return Text.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperValueClass() {
        return Text.class;
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
        try {
            if (options.hasOption('a'))
                DistributedCache.addCacheFile(
                        new URI(options.getStringOption('a')), conf);
            if (options.hasOption('h'))
                conf.set(USE_HEADER, "true");
        } catch (URISyntaxException use) {
            use.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Sets up the Reducer for this job.  
     */
    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) {
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
    }

    /**
     * The {@link TableMapper} responsible for the real work.
     */
    public static class OneLinePerDocExtractorMRMapper 
            extends CorpusTableMR.CorpusTableMapper<Text, Text> {

        private Set<String> wordList;

        private boolean useHeader;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context, Configuration conf)
                throws IOException, InterruptedException {
            useHeader = conf.get(USE_HEADER) != null;
            
            wordList = Sets.newHashSet();

            Path[] paths = DistributedCache.getLocalCacheFiles(conf);
            if (paths.length == 0)
                return;

            Path wordListPath = paths[0];
            BufferedReader br = new BufferedReader(
                    new FileReader(wordListPath.toString()));
            for (String line = null; (line = br.readLine()) != null; )
                wordList.add(line.trim());
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable rowKey,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            Document doc = table.document(row);
            StringBuilder sb = new StringBuilder();
            for (Sentence sentence : table.sentences(row))
                for (Annotation token : sentence) {
                    String term = AnnotationUtil.word(token);
                    if (wordList.isEmpty() || wordList.contains(term))
                        sb.append(term).append(" ");
                }
            if (useHeader)
                context.write(new Text(doc.key()), new Text(sb.toString()));
            else 
                context.write(new Text(), new Text(sb.toString()));
            context.getCounter(MR_NAME, "Document").increment(1);
        }
    }
}
