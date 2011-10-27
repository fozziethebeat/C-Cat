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

import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.IntWritable;

import java.io.IOException;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class WordCountSumReducer
        extends Reducer<StringPair, IntWritable, StringPair, IntWritable> {

    /**
     * Writes the co-occurrence counts as stored in a map of {@link Counter}s to
     * {@code context}.  This is simply a helper function that formats each
     * written co-occurrence in the format used by this {@link Reducer}.
     */
    public static void emitCounts(Map<String, StringCounter> wocCounts,
                                  Mapper.Context context) 
            throws IOException, InterruptedException {
        for (Map.Entry<String, StringCounter> e : wocCounts.entrySet()) {
            String focusWord = e.getKey();
            for (Map.Entry<String, Integer> f : e.getValue())
                context.write(new StringPair(focusWord, f.getKey()),
                              new IntWritable(f.getValue()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reduce(StringPair key,
                       Iterable<IntWritable> values,
                       Context context)
            throws IOException, InterruptedException {
        int totalCount = 0;
        for (IntWritable value : values)
            totalCount += value.get();
        context.write(key, new IntWritable(totalCount));
    }
}
