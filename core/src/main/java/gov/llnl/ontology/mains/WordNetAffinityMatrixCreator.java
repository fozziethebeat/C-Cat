package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.ArgOptions;

import java.io.PrintWriter;


/**
 * @author Keith Stevens
 */
public class WordNetAffinityMatrixCreator {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('d', "wordnetDir", 
                          "Specifies the word net data directory",
                          true, "PATH", "Required");
        options.addOption('r', "relationList",
                          "The list of relation types to " +
                          "consider as valid edges in the wordnet graph",
                          true, "REL[,REL]+", "Required");
        options.parseOptions(args);

        String[] relations = options.getStringOption('r').split(",");
        OntologyReader reader = WordNetCorpusReader.initialize(
                options.getStringOption('d'));

        PrintWriter writer = new PrintWriter(options.getPositionalArg(0));
        StringBasisMapping basis = new StringBasisMapping();
        for (String term : reader.wordnetTerms()) {
            for (Synset s : reader.getSynsets(term)) {
                int index = basis.getDimension(s.getName());
                for (String relation : relations)
                    for (Synset z : s.getRelations(relation)) {
                        int index2 = basis.getDimension(z.getName());
                        writer.printf("%d %d 1\n", index+1, index2+1);
                    }
            }
        }
        writer.close();
    }
}

