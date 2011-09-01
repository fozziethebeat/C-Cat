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

package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.mapreduce.CorpusTableMR;
import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.text.Document;
import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.util.MRArgOptions;

import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class SemEvalPrinter extends CorpusTableMR {

    public static final String ABOUT =
        "Extracts the dependency parsed data for SemEval corpora and " +
        "stores it to hdfs.";

    /**
     * Runs the {@link TokenCountMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new SemEvalPrinter(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void validateOptions(MRArgOptions options) {
        options.validate(ABOUT, "<outdir>", SemEvalPrinter.class, 1, 'C');
    }

    /**
     * {@inheritDoc}
     */
    protected String jobName() {
        return "SemEval Printer";
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return SemEvalPrinterMapper.class;
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
    public static class SemEvalPrinterMapper 
            extends CorpusTableMR.CorpusTableMapper<Text, Text> {

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable rowKey,
                        Result row, 
                        Context context)
                throws IOException, InterruptedException {
            Document doc = table.document(row);
            List<Sentence> sentences = table.sentences(row);
            StringBuilder sb = new StringBuilder();
            String key = doc.key();
            String keyWord = doc.title();
            int index = (int) doc.id();

            for (Sentence sentence : sentences) {
                int i = 0;
                for (Annotation token : sentence) {
                    sb.append(i).append("\t");
                    if (index == 0)
                        sb.append(keyWord).append("\t").append(key).append("\t");
                    else {
                        String word = AnnotationUtil.word(token);
                        sb.append(word).append("\t").append("_").append("\t");
                    }

                    String pos = AnnotationUtil.pos(token);
                    sb.append(pos).append("\t").append(pos).append("\t");

                    sb.append("_\t");
                    sb.append(AnnotationUtil.dependencyParent(token)).append("\t");
                    sb.append(AnnotationUtil.dependencyRelation(token)).append("\t");

                    sb.append("||");

                    ++i;
                    --index;
                }
            }
            context.write(new Text(key), new Text(sb.toString()));
        }
    }
}

