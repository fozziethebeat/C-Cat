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

package gov.llnl.ontology.text.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * A {@link FileInputFormat} that returns each line in a text file as a complete
 * record.
 *
 * @author Keith Stevens
 */
public class LineDocInputFormat 
        extends FileInputFormat<LongWritable, Text> {

    /**
     * {@inheritDoc}
     */
    public RecordReader createRecordReader(InputSplit split,
                                           TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new LineDocReader();
    }

    /**
     * The actualy class that does the heavy lfiting.
     */
    public class LineDocReader 
            extends RecordReader<LongWritable, Text> {

        /**
         * The length into the file that's been read so far.  This serves as
         * they key.
         */
        private long position;

        /**
         * The text of the current record.
         */
        private Text value;

        /**
         * The {@link BufferedReader} used to read each document line.
         */
        private BufferedReader br;

        /**
         * {@inheritDoc}
         */
        public void initialize(InputSplit isplit, TaskAttemptContext context) 
                throws IOException, InterruptedException {
            position = 0;

            FileSplit split = (FileSplit) isplit;
            Path file = split.getPath();
            Configuration config = context.getConfiguration();
            FileSystem fs = file.getFileSystem(config);
            br = new BufferedReader(new InputStreamReader(
                        fs.open(split.getPath())));
        }

        /**
         * {@inheritDoc}
         */
        public boolean nextKeyValue() throws IOException {
            position++;
            String line = br.readLine();
            if (line == null)
                return false;
            value = new Text(line);
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public LongWritable getCurrentKey() {
            return new LongWritable(position);
        }

        /**
         * {@inheritDoc}
         */
        public Text getCurrentValue() {
            return value;
        }

        /**
         * Returns a junk value.
         */
        public float getProgress() throws IOException, InterruptedException {
            return .5f;
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            br.close();
        }
    }
}

