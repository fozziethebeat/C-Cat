package gov.llnl.ontology.mains;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SerializableUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


/**
 * @author Keith Stevens
 */
public class DependencyPathBasisMaker {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('k', "keepHighest", 
                          "Set to either a or d for ascending or descending " +
                          "word order based on path frequencies.",
                          false, null, "Optional");
        options.addOption('n', "numWords", 
                          "Specifies the top N words to keep in the basis.",
                          true, "INT", "Optional");
        options.addOption('o', "outputFile",
                          "Specifies the name of the output file, which " +
                          "will hold the final basis mapping.",
                          true, "FILE", "Required");
        options.parseOptions(args);

        if (options.numPositionalArgs() == 0 ||
            !options.hasOption('o')) {
            System.out.println(
                    "usage: java DependencyPathBasisMaker [OPTIONS] <in>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        boolean keepHighest = options.hasOption('k');
        int numWords = options.getIntOption('n', Integer.MAX_VALUE);

        MultiMap<Integer,String> pathMap =
            new BoundedSortedMultiMap<Integer, String>(numWords, keepHighest);
        BufferedReader br = new BufferedReader(new FileReader(
                    options.getPositionalArg(0)));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] pathCount = line.split("\\s+");
            String path = pathCount[0];
            int count = Integer.parseInt(pathCount[1]);
            pathMap.put(count, path);
        }

        StringBasisMapping basis = new StringBasisMapping();
        for (String path : pathMap.values())
            basis.getDimension(path);

        SerializableUtil.save(basis, new File(options.getStringOption('o')));
    }
}

