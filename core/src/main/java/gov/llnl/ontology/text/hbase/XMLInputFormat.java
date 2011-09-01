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

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;

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


    public static void setXMLTags(Job job, String delimiter) {
        Configuration conf = job.getConfiguration();
        conf.set(XMLRecordReader.DELIMITER_TAG, delimiter);
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
     * Generate the list of files and make them into FileSplits.
     */
    public List<InputSplit> getSplits(JobContext job) throws IOException {
        // generate splits
        List<InputSplit> splits = new ArrayList<InputSplit>();
        for (FileStatus file : listStatus(job))
            splits.add(new FileSplit(file.getPath(), 0, file.getLen(), null));
        return splits;
    }
}
