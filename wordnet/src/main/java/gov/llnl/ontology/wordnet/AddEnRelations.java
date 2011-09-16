package gov.llnl.ontology.wordnet;

import gov.llnl.ontology.wordnet.Synset.PartsOfSpeech;
import gov.llnl.ontology.wordnet.Synset.Relation;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * @author Keith Stevens
 */
public class AddEnRelations {
    public static void main(String[] args) throws Exception {
        WordNetCorpusReader reader = WordNetCorpusReader.initialize(args[0]);
        BufferedReader br = new BufferedReader(new FileReader(args[1]));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] srcRest = line.split("\\s+", 2);
            if (srcRest.length != 2)
                continue;

            String[] offsetPos = srcRest[0].split("\\.");
            int offset = Integer.parseInt(offsetPos[0]);
            PartsOfSpeech pos = PartsOfSpeech.fromId(offsetPos[1]);
            Synset source = reader.getSynsetFromOffset(offset, pos);
            if (source == null) {
                System.out.printf("Skipping: %d.%s\n", offset, pos);
                continue;
            }
            for (String link : srcRest[1].split("\\s+")) {
                if (link.equals(""))
                    continue;
                Synset target = reader.getSynset(link);
                if (target == null)
                    continue;

                source.addRelation(Relation.SIMILAR_TO, target);
                System.out.printf("%s -> %s\n", source.getName(), target.getName());
            }
        }
    }
}
