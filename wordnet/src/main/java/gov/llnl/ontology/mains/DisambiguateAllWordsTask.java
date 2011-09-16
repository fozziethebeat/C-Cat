package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.corpora.SenseEvalAllWordsDocumentReader;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.util.StringPair;
import gov.llnl.ontology.wordnet.OntologyReader;
import gov.llnl.ontology.wordnet.WordNetCorpusReader;
import gov.llnl.ontology.wordnet.wsd.WordSenseDisambiguation;

import com.google.common.collect.Sets;

import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.WorkQueue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class DisambiguateAllWordsTask {

    public static void main(String[] args) throws Exception {
        String dataDir = args[0];
        String algName = args[1];
        String testCorpusName = args[2];
        String taggedCorpus = args[3];
        String outputPostFix = args[4];

        OntologyReader wordnet = WordNetCorpusReader.initialize(dataDir);

        final WordSenseDisambiguation wsd = ReflectionUtil.getObjectInstance(algName);
        wsd.setup(wordnet);

        SenseEvalAllWordsDocumentReader reader =
            new SenseEvalAllWordsDocumentReader();
        reader.parse(testCorpusName);

        BufferedReader br = new BufferedReader(new FileReader(taggedCorpus));
        String sentLine = br.readLine();
        String tokenLine = br.readLine();
        List<Sentence> taggedSentences = Sentence.readSentences(sentLine, tokenLine);
        List<Sentence> sentences = reader.sentences();

        final String[] output = new String[sentences.size()];
        int s = 0;
        WorkQueue workQueue = new WorkQueue();
        Object key = workQueue.registerTaskGroup(sentences.size());
        for (final Sentence sentence : sentences) {
            final Set<Integer> focusIndices = Sets.newHashSet();
            Sentence taggedSent = taggedSentences.get(s);
            System.err.println("Reading sentence: " + s);
            StringPair[] tags = taggedSent.taggedTokens();

            int i = 0;
            for (Annotation annot : sentence) {
                if (annot.get(ValueAnnotation.class) != null) 
                    focusIndices.add(i);
                AnnotationUtil.setPos(annot, tags[i++].y); 
            }

            final int sentenceId = s++;
            workQueue.add(key, new Runnable() {
                public void run() {
                    String res = disambiguate(wsd, sentence, focusIndices);
                    output[sentenceId] = res;
                }
            });
        }
        workQueue.await(key);

        PrintWriter writer = new PrintWriter(wsd.toString() + outputPostFix);
        for (String out : output)
            if (out != null && out.length() > 0)
                writer.println(out);
        writer.close();
    }

    
    public static String disambiguate(WordSenseDisambiguation wsd,
                                      Sentence sentence,
                                      Set<Integer>focusIndices) {
        int i = 0;
        Sentence disambiguated = wsd.disambiguate(sentence, focusIndices);
        System.err.println("Disambiguated a sentence");
        StringBuilder sb = new StringBuilder();
        String prevId = null;
        for (Annotation annot : disambiguated) {
            String id = sentence.getAnnotation(i).get(
                    ValueAnnotation.class);
            if (id != null) {
                String sense = AnnotationUtil.wordSense(annot);
                if (sense == null)
                    sense = "U";

                if (prevId == null)
                    sb.append(id).append(" ").append(sense);
                else if (prevId.equals(id))
                    sb.append(" ").append(sense);
                else
                    sb.append("\n").append(id).append(" ").append(sense);
                prevId = id;
            }
            ++i;
        }
        return sb.toString();
    }
}

