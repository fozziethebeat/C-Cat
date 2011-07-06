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

import gov.llnl.text.util.FileUtils;

import org.apache.commons.codec.digest.DigestUtils;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.GZIPInputStream;


/**
 * A {@link FileInputFormat} for handling gzipped tarball files with each
 * internal file containing data for a single document.
 *
 * @author Keith Stevens
 */
public class XMLInputFormat
        extends FileInputFormat<ImmutableBytesWritable, Text> {

    public static final String CONF_PREFIX =
        "gov.llnl.ontology.text.hbase.XMLInputFormat";

    public static final String DELIMITER_TAG =
        CONF_PREFIX + ".tag";

    public static void setXMLTags(Job job, String delimiter) {
        Configuration conf = job.getConfiguration();
        conf.set(DELIMITER_TAG, delimiter);
    }

    /**
     * Returns a {@link XMLRecordReader}.  The record reader will return
     * each tarred file.
     */
    public RecordReader createRecordReader(InputSplit split,
                                           TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new XMLRecordReader();
    }

    /**
     * Returns a {@link List} of {@link FileSplit}s.  Each {@link FileSplit}
     * will be a gzipped tarball of xml documents.  Each tarred file should
     * contain a single document.
    public List<InputSplit> getSplits(JobContext context) throws IOException {
        List<InputSplit> splits = new ArrayList<InputSplit>();

        // Get the list of xml files to be processed and add each file as an
        // InputSplit.
        FileSystem fs = FileSystem.get(context.getConfiguration());
        for (Path file : getInputPaths(context)) {
            // Check that the file exists.  Throw an exception if it does not.
            if (fs.isDirectory(file) || !fs.exists(file))
                throw new IOException("File does not exist: " + file);

            // Get the length of the file and then add a new FileSplit.
            long length = fs.getFileStatus(file).getLen();
            splits.add(new FileSplit(file, 0, length, null));
        }
        return splits;
    }
     */

    /**
     * A {@link RecordReader} for processing gzipped tarballs of document files.
     * It is assumed that each tarballed file is a single document, or will be
     * processed further by other stages.
     */
    public class XMLRecordReader
            extends RecordReader<ImmutableBytesWritable, Text> {

        /**
         * The current {@link ImmutableBytesWritable} key read.
         */
        private ImmutableBytesWritable currentKey;

        /**
         * The current {@link Text} document.
         */
        private Text currentDocument;

        /**
         * The tag that begins a single XML document.
         */
        private byte[] startTag;

        /**
         * The tag that ends a single XML document.
         */
        private byte[] endTag;

        /**
         * A file stream for reading xml data that needs to be partitioned.
         */
        private InputStream fsin;

        /**
         * The start byte position of the current record.
         */
        private long start;

        /**
         * The end byte position of the current record.
         */
        private long end;

        /**
         * The current position in the file stream.
         */
        private long pos;

        /**
         * An output buffer for storing characters that will compose a single
         * record.
         */
        private final DataOutputBuffer buffer = new DataOutputBuffer();
    
        /**
         * Extract the {@link Path} for the file to be processed by this {@link
         * XMLRecordReader}.
         */
        public void initialize(InputSplit isplit, TaskAttemptContext context) 
                throws IOException, InterruptedException {
            // Get the xml document delmiters for this xml file.
            Configuration config = context.getConfiguration();
            startTag = ("<" + config.get(DELIMITER_TAG)).getBytes();
            endTag = ("</" + config.get(DELIMITER_TAG) + ">").getBytes();

            // Get the file stream for the xml file.
            FileSplit split = (FileSplit) isplit;
            Path file = split.getPath();
            FileSystem fs = file.getFileSystem(config);
            fsin = fs.open(split.getPath());

            // Setup the limits of the xml file.
            start = split.getStart();
            end = start + split.getLength();
            pos = 0;
        }

        /**
         * Advances the reader one step to point to the next tarball file.  It
         * returns {@code null} when there are no more files in the tarball.
         */
        public boolean nextKeyValue() throws IOException {
            currentKey = new ImmutableBytesWritable();
            currentDocument = new Text();
            buffer.reset();

            if (pos < end) {
                if (readUntilMatch(startTag, false)) {
                    // Write the start tag.
                    buffer.write(startTag);

                    // Read the record into the buffer.
                    if (readUntilMatch(endTag, true)) {
                        // Write the key and value for this record.
                        currentKey.set(Long.toString(pos).getBytes());
                        currentDocument.set(
                                buffer.getData(), 0, buffer.getLength());
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public ImmutableBytesWritable getCurrentKey() {
            return currentKey;
        }

        /**
         * {@inheritDoc}
         */
        public Text getCurrentValue() {
            return currentDocument;
        }
        
        /**
         * {@inheritDoc}
         */
        public float getProgress() throws IOException, InterruptedException {
            return (pos - start) / (float) (end - start);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            fsin.close();
        }

        /**
         * Reads characters from the file stream until a set of characters match
         * the text in {@code match}.  Returns true if a valid match was found
         * and false if the end of file was reached but no valid match was
         * found.  If {@code withinBlock} is true, characters read will be
         * stored in {@code buffer}.
         */
        private boolean readUntilMatch(byte[] match, boolean withinBlock)
                throws IOException {
            int i = 0;
            while (true) {
                // Read the next byte.
                int b = fsin.read();
                pos++;

                // Check for end of file.
                if (b == -1)
                    return false;
            // Save to the buffer.
            if (withinBlock)
                buffer.write(b);
            
            // Check if we're matching:
            if (b == match[i]) {
                i++;
                if (i >= match.length)
                    return true;
            } else
                i = 0;

            // Return false if we're still reading a record but the file has
            // ended.
            if (!withinBlock && i == 0 && pos >= end)
                return false;
            }
        }
    }
}
