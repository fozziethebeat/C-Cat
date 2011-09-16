package gov.llnl.ontology.mapreduce.stats;

import gov.llnl.ontology.text.corpora.NYTDocumentReader;
import gov.llnl.ontology.text.corpora.NYTCorpusDocument;
import gov.llnl.ontology.text.hbase.GzipTarInputFormat;
import gov.llnl.ontology.util.MRArgOptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class NYTOnelineSectionMR extends Configured implements Tool {

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(NYTOnelineSectionMR.class);

    /**
     * Runs the {@link IngestCorpusMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), new NYTOnelineSectionMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws Exception, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();
        LOG.info("Parse Options");
        // Parse and validate the arguments.
        options.parseOptions(args);
        options.validate("", "<indir>+", NYTOnelineSectionMR.class, -1);

        LOG.info("Setup Configuration");
        // Setup the configuration so that the mappers know which classes to
        // use.
        Configuration conf = getConf();
        LOG.info("Setup Input Data Format");
        // Create the job and set the jar.
        Job job = new Job(conf, "Import Corpus");
        job.setJarByClass(NYTOnelineSectionMR.class);

        job.setMapperClass(NYTOnelineSectionMapper.class);

        // Set the input format class based on the option given.
        job.setInputFormatClass(GzipTarInputFormat.class);

        // Set the path for the input file which contains a list of files to
        // operate over
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            FileInputFormat.addInputPath(
                    job, new Path(options.getPositionalArg(i)));

        LOG.info("Setup Mapper");
        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(
                job, new Path(options.getPositionalArg(0)));
        job.setNumReduceTasks(0);

        LOG.info("Start NYTOnelineSectionMapper job"); 
        // Run the job.
        job.waitForCompletion(true);
        LOG.info("Job Completed");

        return 0;
    }
        
    /**
     * This {@link Mapper} iterates over text documents on disk and extracts
     * various document details and the raw document text.  All of the extracted
     * information is stored in a {@link CorpusTable}.
     */
    public static class NYTOnelineSectionMapper
            extends Mapper<ImmutableBytesWritable, Text, Text, Text> {
        
        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key, 
                        Text value,
                        Context context) throws IOException, InterruptedException {
            NYTCorpusDocument doc =
                NYTDocumentReader.parseNYTCorpusDocumentFromString(
                        value.toString(), false);
            String docKey = doc.key();
            String sections = doc.getOnlineSection();
            if (docKey != null && sections != null)
                context.write(new Text(docKey), new Text(sections));
        }
    }
}
