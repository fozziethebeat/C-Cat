package gov.llnl.ontology.mains;

import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.Synset;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;

import gov.llnl.text.util.FileUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author Keith Stevens
 */
public class TagToSynsetLinker {
    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('d', "wordnetDir", 
                          "Specifies the directory for wordnet data files",
                          true, "PATH", "Required");
        options.addOption('t', "tagWordCount",
                          "Specifies the file with tag,word co-occurrence " +
                          "counts",
                          true, "FILE", "Required");
        options.parseOptions(args);

        OntologyReader reader = WordNetCorpusReader.initialize(
                options.getStringOption('w'));

        StringBasisMapping basis = new StringBasisMapping();
        Map<String, SparseDoubleVector> tagVectors = Maps.newHashMap();
        String tagFile = options.getStringOption('t');
        for (String line : FileUtils.iterateFileLines(tagFile)) {
            String[] tagWordCount = line.split("\\s+");
            SparseDoubleVector tagVector = tagVectors.get(tagWordCount[0]);
            if (tagVector == null) {
                tagVector = new CompactSparseVector();
                tagVectors.put(tagWordCount[0], tagVector);
            }
            int wordIndex = basis.getDimension(tagWordCount[1]);
            int count = Integer.parseInt(tagWordCount[2]);
            tagVector.add(wordIndex, count);
        }

        basis.setReadOnly(true);

        for (Map.Entry<String, SparseDoubleVector> e : tagVectors.entrySet()) {
            String tag = e.getKey();
            SparseDoubleVector vector = e.getValue();

            List<Synset> possibleSynsets = Lists.newArrayList();
            for (String tagItem : tag.split("/"))
                possibleSynsets.addAll(Arrays.asList(
                            reader.getSynsets(tagItem)));

            Synset bestSynset = null;
            double bestScore = -1.0;
            for (Synset synset : possibleSynsets) {
                SparseDoubleVector glossVector = new CompactSparseVector();
                for (String glossToken : synset.getGloss().split("\\s+")) {
                    int index = basis.getDimension(glossToken);
                    if (index >= 0)
                        glossVector.add(index, 1);
                }
                double score = Similarity.cosineSimilarity(vector, glossVector);
                if (score >= bestScore) {
                    bestScore = score;
                    bestSynset = synset;
                }
            }
            System.out.printf("%s -> %s\n", tag, bestSynset.getName());
        }
    }
}
