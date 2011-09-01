package gov.llnl.ontology.mains;

import gov.llnl.ontology.mapreduce.table.CorpusTable;
import gov.llnl.ontology.util.MRArgOptions;
import gov.llnl.ontology.util.StringPair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class SelectRandomTaggedNodes {

    public static void main(String[] args) throws Exception {
        MRArgOptions options = new MRArgOptions();
        options.addOption('n', "numberOfPairs",
                          "Sets the number of pairs to create",
                          true, "INT", "Required");
        options.addOption('k', "keyList",
                          "Sets the key list of valid document keys.  Key " +
                          "pairs will be selected at random from this list",
                          true, "FILE", "Required");
        options.parseOptions(args);
        options.validate("", "<out>", SelectRandomTaggedNodes.class,
                         1, 'n', 'k');

        List<String> documentKeys = loadDocKeys(options.getStringOption('k'));
        int numPairs = options.getIntOption('n');

        Set<StringPair> keyPairs = Sets.newHashSet();
        Random random = new Random();
        while (keyPairs.size() < numPairs) {
            int i = random.nextInt(documentKeys.size());
            int j = random.nextInt(documentKeys.size());
            if (i == j)
                continue;
            keyPairs.add(new StringPair(
                        documentKeys.get(i), documentKeys.get(j)));
        }

        CorpusTable table = options.corpusTable();
        for (StringPair pair : keyPairs) {
            StringBuilder builder = new StringBuilder();
            addCategories(builder, pair.x, table);
            builder.append("|");
            addCategories(builder, pair.y, table);
            System.out.println(builder.toString());
        }
    }

    public static List<String> loadDocKeys(String keyFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(keyFile));
        List<String> docKeys = Lists.newArrayList();
        for (String line = null; (line = br.readLine()) != null;)
            docKeys.add(line.trim());
        return docKeys;
    }

    public static void addCategories(StringBuilder builder,
                                     String key, 
                                     CorpusTable table) throws Exception {
        Get get = new Get(key.getBytes());
        Result row = table.table().get(get);
        for (String category : table.getCategories(row))
            builder.append(category).append(";");
    }
}
