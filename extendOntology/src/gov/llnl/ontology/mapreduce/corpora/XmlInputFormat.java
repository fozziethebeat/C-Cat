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

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package gov.llnl.ontology.corpora;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
* Reads records that are delimited by a specifc begin/end tag.
*/
public class XmlInputFormat extends TextInputFormat {
  
  /**
   * The {@link Configuration} setting name for the start xml tag.
   */
  public static final String START_TAG_KEY = "xmlinput.start";

  /**
   * The {@link Configuration} setting name for the end xml tag.
   */
  public static final String END_TAG_KEY = "xmlinput.end";
  
  /**
   * {@inheritDoc}
   */
  public RecordReader<LongWritable,Text> createRecordReader(
    InputSplit inputSplit,
    TaskAttemptContext context) {
    return new XmlRecordReader();
  }
  
  /**
   * XMLRecordReader class to read through a given xml document to output xml
   * blocks as records as specified by the start tag and end tag
   */
  public class XmlRecordReader extends RecordReader<LongWritable,Text> {

    /**
     * The xml tag that specifies when a record begins.
     */
    private byte[] startTag;

    /**
     * The xml tag that specifies when a record ends.
     */
    private byte[] endTag;

    /**
     * The first byte offset at which this reader reads from in the original
     * file.
     */
    private long start;

    /**
     * The last byte offset at which this reader reads from in the original
     * file.
     */
    private long end;

    /**
     * A file stream for reading xml data that needs to be partitioned.
     */
    private InputStream fsin;

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
     * The current key value to be returned for a record.
     */
    private LongWritable key;

    /**
     * The current value to be returned for a record.
     */
    private Text value;

    /**
     * Creates a new {@link XmlRecordReader}
     */
    public XmlRecordReader() {
    }
    
    /**
     * {@inheritDoc}
     */
    public void initialize(InputSplit isplit, TaskAttemptContext context) 
        throws IOException {
      Configuration conf = context.getConfiguration();
      FileSplit split = (FileSplit) isplit;

      // Currently getting the tags from the configuration does not work.
      // TODO: figure out why.
      startTag = "<text".getBytes();
     // conf.get(START_TAG_KEY).getBytes("utf-8");
      endTag = "</text>".getBytes();
      //conf.get(END_TAG_KEY).getBytes("utf-8");
      
      // Open the file
      start = split.getStart();
      end = start + split.getLength();
      Path file = split.getPath();

      // Check to see if there is an appropriate compression codec for the file
      // based on it's file name.
      CompressionCodecFactory compressionCodecs =
        new CompressionCodecFactory(conf);
      final CompressionCodec codec = compressionCodecs.getCodec(file);

      // Create a file stream for the file based on whether or not there is a
      // decoder for it.
      FileSystem fs = file.getFileSystem(conf);
      FSDataInputStream fileIn = fs.open(split.getPath());
      fsin = (codec != null)
        ? codec.createInputStream(fileIn) : fileIn;
      pos = 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean nextKeyValue() {
      key = new LongWritable();
      value = new Text();
      buffer.reset();

      try {
        if (pos < end) {
          if (readUntilMatch(startTag, false)) {
            // Write the start tag.
            buffer.write(startTag);

            // Read the record into the buffer.
            if (readUntilMatch(endTag, true)) {
              // Write the key and value for this record.
              key.set(pos);
              value.set(buffer.getData(), 0, buffer.getLength());
              return true;
            }
          }
        }
        return false;
      } catch (IOException ioe) {
        ioe.printStackTrace();
        throw new IOError(ioe);
      }
    }
    
    /**
     * {@inheritDoc}
     */
    public LongWritable getCurrentKey() {
      return key;
    }

    /**
     * {@inheritDoc}
     */
    public Text getCurrentValue() {
      return value;
    }
    
    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
      fsin.close();
    }
    
    /**
     * {@inheritDoc}
     */
    public float getProgress() throws IOException {
      return (pos - start) / (float) (end - start);
    }
    
    /**
     * Reads characters from the file stream until a set of characters match
     * the text in {@code match}.  Returns true if a valid match was found and
     * false if the end of file was reached but no valid match was found.  If
     * {@code withinBlock} is true, characters read will be stored in {@code
     * buffer}.
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

        // Return false if we're still reading a record but the file has ended.
        if (!withinBlock && i == 0 && pos >= end)
          return false;
      }
    }
  }
}
