package gov.llnl.ontology.mapreduce;

import gov.llnl.ontology.mapreduce.table.EvidenceTable;
import gov.llnl.ontology.util.MRArgOptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.IdentityTableReducer;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;

import java.io.IOException;


/**
 * @author Keith Stevens
 */
public abstract class EvidenceTableMR extends Configured implements Tool {

    /**
     * The configuration key prefix.
     */
    public static String CONF_PREFIX =
        "gov.llnl.ontology.mapreduce.EvidenceTableMR";

    /**
     * The configuration key for setting the {@link EvidenceTable}.
     */
    public static String EVIDENCE =
        CONF_PREFIX + ".evidenceTable";

    /**
     * Acquire the logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(EvidenceTableMR.class);

    /**
     * Add more command line arguments.
     */
    protected void addOptions(MRArgOptions options) {
    }

    /**
     * Returns true if the {@link MRArgOptions} contains a valid value for each
     * requried option.
     */
    protected boolean validOptions(MRArgOptions options) {
        return true;
    }

    /**
     * Copies command line arguments to a {@link Configuration} so that
     * Map/Reduce jobs can utilize the values set.
     */
    protected void setupConfiguration(MRArgOptions options,
                                      Configuration conf) {
    }

    /**
     * Sets up the Reducer for this job.  By default, it is a {@link
     * IdentityTableReducer}.
     */
    protected void setupReducer(String tableName,
                                Job job,
                                MRArgOptions options) throws IOException {
        // Setup an empty reducer.
        TableMapReduceUtil.initTableReducerJob(tableName,
                                               IdentityTableReducer.class, 
                                               job);
        job.setNumReduceTasks(0);
    }

    /**
     * Returns the {@link Class} object for the Mapper task.
     */
    protected abstract Class mapperClass();

    /**
     * Returns the {@link Class} object for the Mapper Key of this task.
     */
    protected Class mapperKeyClass() {
        return ImmutableBytesWritable.class;
    }

    /**
     * Returns the {@link Class} object for the Mapper Value of this task.
     */
    protected Class mapperValueClass() {
        return Put.class;
    }

    /**
     * {@inheritDoc}
     */
    public int run(String[] args)
            throws IOException, InterruptedException, ClassNotFoundException {
        // Setup the main arguments used.
        MRArgOptions options = new MRArgOptions();
        addOptions(options);

        // Parse and validate the arguments.
        LOG.info("Parse Options");
        options.parseOptions(args);
        if (!validOptions(options)) {
            System.err.println("usage: java IngestCorpusMR [OPTIONS]\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        // Setup the configuration.
        LOG.info("Setup Configuration");
        Configuration conf = getConf();
        conf.set(EVIDENCE, options.evidenceTableType());
        setupConfiguration(options, conf);

        // Create the corpus table and setup the scan.
        LOG.info("Setup Table Scan");
        EvidenceTable table = options.evidenceTable();
        Scan scan = new Scan();
        table.setupScan(scan, options.sourceCorpus());

        // Create the job and set the jar.
        LOG.info("Setup Job");
        Job job = new Job(conf, "Ingest Corpus");
        job.setJarByClass(EvidenceTableMR.class);

        // Setup the mapper class.
        TableMapReduceUtil.initTableMapperJob(table.tableName(),
                                              scan, 
                                              mapperClass(),
                                              mapperKeyClass(),
                                              mapperValueClass(),
                                              job);
        setupReducer(table.tableName(), job, options);

        LOG.info("Start Mapper job"); 
        // Run the job.
        job.waitForCompletion(true);
        LOG.info("Job Completed");

        return 0;
    }
}

