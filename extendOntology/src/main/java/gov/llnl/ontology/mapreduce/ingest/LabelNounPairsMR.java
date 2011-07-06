package gov.llnl.ontology.mapreduce.ingest;

import gov.llnl.ontology.mapreduce.EvidenceTableMR;
import gov.llnl.ontology.mapreduce.table.EvidenceTable;
import gov.llnl.ontology.util.StringPair;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.SynsetRelations;
import gov.llnl.ontology.wordnet.SynsetRelations.HypernymStatus;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import gov.llnl.ontology.util.MRArgOptions;

import edu.ucla.sspace.util.ReflectionUtil;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;

import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class LabelNounPairsMR extends EvidenceTableMR {

    public static final String WORD_NET_DIR = CONF_PREFIX + ".wordnetDir";

    /**
     * Runs the {@link LabelNounPairsMR}.
     */
    public static void main(String[] args) throws Exception {
        ToolRunner.run(HBaseConfiguration.create(), 
                       new LabelNounPairsMR(), args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addOptions(MRArgOptions options) {
        options.addOption('w', "wordnetDir",
                          "Specifies the path of the wordnet dict files.",
                          true, "PATH", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected boolean validOptions(MRArgOptions options) {
        return options.hasOption('w');
    }

    /**
     * {@inheritDoc}
     */
    protected void setupConfiguration(MRArgOptions options, 
                                      Configuration conf) {
        conf.set(WORD_NET_DIR, options.getStringOption('w'));
    }

    /**
     * {@inheritDoc}
     */
    protected Class mapperClass() {
        return LabelNounPairsMapper.class;
    }

    /**
     */
    public static class LabelNounPairsMapper
            extends TableMapper<ImmutableBytesWritable, Put> {

        /**
         * The {@link EvidenceTable} that dictates the structure of the table
         * containing a corpus.
         */
        private EvidenceTable table;

        private OntologyReader reader;

        /**
         * {@inheritDoc}
         */
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            table = ReflectionUtil.getObjectInstance(conf.get(EVIDENCE));
            table.table();
            reader = WordNetCorpusReader.initialize(
                    conf.get(WORD_NET_DIR), true);
        }

        /**
         * {@inheritDoc}
         */
        public void map(ImmutableBytesWritable key,
                        Result row, 
                        Context context) {
            StringPair nounPair = table.nounPair(row);
            HypernymStatus status = SynsetRelations.getHypernymStatus(
                    nounPair.x, nounPair.y);
            table.putHypernymStatus(key, status);
        }

        protected void cleanup(Context context) {
            table.close();
        }
    }
}
