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

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.InputSplit;
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
 * internal file containing data for a single document.  This assumes that the
 * file, or files, being processed are in raw text format and contain one file
 * path per line of gzipped tarballs.  Each entry in the gzipped tarball will be
 * considered a single document.
 *
 * @author Keith Stevens
 */
public class GzipTarInputFormat
        extends FileInputFormat<ImmutableBytesWritable, Text> {

    /**
     * Returns a {@link GzipTarRecordReader}.  The record reader will return
     * each tarred file.
     */
    public RecordReader createRecordReader(InputSplit split,
                                           TaskAttemptContext context)
            throws IOException, InterruptedException {
        return new GzipTarRecordReader();
    }

    /**
     * Returns a {@link List} of {@link FileSplit}s.  Each {@link FileSplit}
     * will be a gzipped tarball of xml documents.  Each tarred file should
     * contain a single document.
     */
    public List<InputSplit> getSplits(JobContext context) throws IOException {
        List<InputSplit> splits = new ArrayList<InputSplit>();

        // Get the list of zipped files to be processed and add each zipped file
        // as an InputSplit.
        FileSystem fs = FileSystem.get(context.getConfiguration());
        for (Path file : getInputPaths(context)) {
            // Check that the list of files exists.  Throw an exception if it
            // does not.
            if (fs.isDirectory(file) || !fs.exists(file))
                throw new IOException("File does not exist: " + file);

            // Read the contents of the file list and add each line as a
            // FileSplit.
            BufferedReader br = new BufferedReader(new InputStreamReader(
                        fs.open(file)));
            for (String line = null; (line = br.readLine()) != null; )
                splits.add(new FileSplit(
                            new Path(line), 0, Integer.MAX_VALUE, null));
        }
        return splits;
    }

    /**
     * A {@link RecordReader} for processing gzipped tarballs of document files.
     * It is assumed that each tarballed file is a single document, or will be
     * processed further by other stages.
     */
    public class GzipTarRecordReader
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
         * The {@link TarInputStream} used to read files.
         */
        private TarInputStream tarStream;

        /**
         * Contains the parent path of the gzipped tarball being processed by
         * this {@link GzipTarRecordReader}.
         */
        private String parentName;

        /**
         * Extract the {@link Path} for the file to be processed by this {@link
         * GzipTarRecordReader}.
         */
        public void initialize(InputSplit split, TaskAttemptContext context) 
                throws IOException, InterruptedException {
            // Get the file Path for this input split.
            Configuration config = context.getConfiguration();
            FileSystem fs = FileSystem.get(config);
            FileSplit fileSplit = (FileSplit) split;
            Path filePath = fileSplit.getPath();
            parentName = filePath.getParent().getName();
            InputStream is = fs.open(filePath);

            System.err.println(filePath.toString());
            // Unzip the file and get a tarball reader.
            GZIPInputStream gis = new GZIPInputStream(is);
            tarStream = new TarInputStream(gis);
        }

        /**
         * Advances the reader one step to point to the next tarball file.  It
         * returns {@code null} when there are no more files in the tarball.
         */
        public boolean nextKeyValue() throws IOException {
            TarEntry tarEntry = null;

            // Iterate through the tar entries until a true file or the end of
            // the tarball is found.
            while ((tarEntry = tarStream.getNextEntry()) != null &&
                   tarEntry.isDirectory())
                ;

            // Return false when there are no more entries in the tarball.
            if (tarEntry == null)
                return false;

            // Set the current key.
            String key = parentName + tarEntry.getName();
            currentKey = new ImmutableBytesWritable(
                    DigestUtils.shaHex(key).getBytes());

            // Set the current document.
            String document = FileUtils.readFile(new BufferedReader(
                        new InputStreamReader(tarStream)));
            currentDocument = new Text(document);

            // successfully advanced.
            return true;
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
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
        }
    }
}
