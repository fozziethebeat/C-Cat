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

import gov.llnl.ontology.util.Counter;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;
import java.util.Map;


/**
 * This {@link Reducer} counts the number of times that they {@code key} occures
 * with each of it's {@code value}s.  It emits each a tuple of the form: 
 *   (key,other_item), co-occurrence_count
 * Where the first item is emited as a single {@link Text} object and the count
 * is a {@link IntWritable} object.
 *
 * @author Keith Stevens
 */
public class WordSumReducer extends Reducer<Text, Text, Text, Text> {

    /**
     * {@inheritDoc}
     */
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        Counter<String> occurrences = new Counter<String>();
        for (Text item : values) {
            String[] parts = item.toString().split("\\|[0-9]*");
            int count = 1;
            if (parts.length == 2)
                count = Integer.parseInt(parts[1]);

            occurrences.count(parts[0], count);
        }

        for (Map.Entry<String, Integer> e : occurrences)
            context.write(new Text(key.toString()),
                          new Text(e.getKey() + "|" + e.getValue()));
    }
}
