package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.util.StringCounter;
import gov.llnl.ontology.util.StringPair;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

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
    public void reduce(StringPair key, Iterable<IntWritable> values, Context context)
            throws IOException, InterruptedException {
        int totalCount = 0;
        for (IntWritable value : values)
            totalCount += value.get();
        /*
        StringCounter occurrences = new StringCounter();
        for (StringCounter counter : values) {
            for (Map.Entry<String, Integer> e : counter)
                occurrences.count(e.getKey(), e.getValue());
        }
        */

        context.write(key, new IntWritable(totalCount));
    }
}
