package gov.llnl.ontology.mains;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.text.corpora.SenseEvalAllWordsDocumentReader;
import gov.llnl.ontology.util.AnnotationUtil;
import gov.llnl.ontology.util.StringPair;
import gov.llnl.ontology.text.tag.OpenNlpMEPOSTagger;

import com.google.common.collect.Sets;

import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.WorkQueue;

import opennlp.tools.postag.POSTagger;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class SaveTaggedSenseTask {

    public static void main(String[] args) throws Exception {
        String testCorpusName = args[0];
        String outputPostFix = args[1];

        POSTagger tagger = new OpenNlpMEPOSTagger();

        SenseEvalAllWordsDocumentReader reader =
            new SenseEvalAllWordsDocumentReader();
        reader.parse(testCorpusName);

        List<Sentence> sentences = reader.sentences();
        final String[] output = new String[sentences.size()];
        int s = 0;
        WorkQueue workQueue = new WorkQueue();
        Object key = workQueue.registerTaskGroup(sentences.size());
        for (final Sentence sentence : sentences) {
            int i = 0;
            final Set<Integer> focusIndices = Sets.newHashSet();
            String[] tokens = new String[sentence.numTokens()];
            for (Annotation annot : sentence) {
                tokens[i] = AnnotationUtil.word(annot);
                if (tokens[i].indexOf(" ") != -1)
                    tokens[i] = annot.get(StemAnnotation.class);

                if (annot.get(ValueAnnotation.class) != null) {
                    focusIndices.add(i);
                }
                ++i;
            }

            i = 0;
            String[] tags = tagger.tag(tokens);
            for (Annotation annot : sentence)
                AnnotationUtil.setPos(annot, tags[i++]);
        }

        StringPair lines = Sentence.writeSentences(sentences);
        PrintWriter writer = new PrintWriter(outputPostFix);
        writer.println(lines.x);
        writer.println(lines.y);
        writer.close();
    }
} 
