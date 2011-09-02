package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.Sentence;
import gov.llnl.ontology.util.AnnotationUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.stanford.nlp.ling.CoreAnnotations.StemAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.pipeline.Annotation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author Keith Stevens
 */
public class SenseEvalAllWordsDocumentReader extends DefaultHandler {

    private List<Sentence> sentences = Lists.newArrayList();

    private List<Annotation> currentSentence = Lists.newArrayList();

    private Annotation currentAnnotation;

    private Map<String, Annotation> satMap = Maps.newHashMap();

    private String currentTextId;

    private boolean inText;

    private boolean inHead;

    private boolean inSat;

    private StringBuilder b;

    /**
     * The internal xml parser.
     */
    private SAXParser saxParser;

    public SenseEvalAllWordsDocumentReader() {
        b = new StringBuilder();
        SAXParserFactory saxfac = SAXParserFactory.newInstance();
        saxfac.setValidating(false);

        // Ignore a number of xml features, like dtd grammars and validation.
        try {
            saxfac.setFeature("http://xml.org/sax/features/validation", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxParser = saxfac.newSAXParser();
        } catch (SAXNotRecognizedException e1) {
            throw new RuntimeException(e1);
        }catch (SAXNotSupportedException e1) {
            throw new RuntimeException(e1);
        } catch (ParserConfigurationException e1) {
            throw new RuntimeException(e1);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public void parse(String fileName) {
        inText = false;
        inHead = false;
        inSat = false;
        // Parse!
        try {
            saxParser.parse(new File(fileName), this);
        } catch (SAXException se) {
            throw new RuntimeException(se);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes atts)
            throws SAXException {
        if ("text".equals(name)) {
            inText = true;
            currentTextId = atts.getValue("id");
            currentAnnotation = new Annotation();
        } else if ("head".equals(name)) {
            inHead = true;
            String id = currentTextId + " " + atts.getValue("id");
            currentAnnotation.set(ValueAnnotation.class, id);
            String sats = atts.getValue("sats");
            if (sats != null)
                currentAnnotation.set(StemAnnotation.class, sats);
        } else if ("sat".equals(name)) {
            inSat = true;
            satMap.put(atts.getValue("id"), currentAnnotation);
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if ("sat".equals(name)) {
            inSat = false;
        } else if ("head".equals(name)) {
            inHead = false;
        } else if ("text".equals(name)) {
            inText = false;
            endSentence();
        }
    }

    private void endSentence() {
        Sentence sentence = new Sentence(0, 0, currentSentence.size());
        int i = 0;
        for (Annotation word : currentSentence) {
            sentence.addAnnotation(i++, word);
            String satIds = word.get(StemAnnotation.class);
            if (satIds == null)
                continue;
            String id = word.get(ValueAnnotation.class);
            String idPart = id.split("\\s")[1];
            String lemma = AnnotationUtil.word(word);
            StringBuilder sb = new StringBuilder();
            boolean addedLemma = false;
            for (String satId : satIds.split("\\s+")) {
                if (idPart.equals(satId)) {
                    sb.append(lemma).append(" ");
                    addedLemma =true;
                    continue;
                }

                if (!addedLemma && 
                    (satId.length() > idPart.length() ||
                     (satId.length() == idPart.length() &&
                      satId.compareTo(idPart) > 0))) {
                    sb.append(lemma).append(" ");
                    addedLemma = true;
                }
                sb.append(AnnotationUtil.word(satMap.get(satId))).append(" ");
            }
            AnnotationUtil.setWord(word, sb.toString().trim().toLowerCase());
        }
        sentences.add(sentence);
        currentSentence.clear();
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (!inText && !inHead && !inSat)
            return;

        String s = (new String(ch, start, length)).trim();
        String[] parts = (inHead || inSat)
            ? s.split("[-/]")
            : s.split("\\s+");
        if (parts.length == 0 || (parts.length == 1 && parts[0].equals("")))
            return;

        for (int i = 0; i < parts.length; ++i) {
            if (parts[i].equals("'s"))
                parts[i] = "is";
            parts[i] = parts[i].toLowerCase();
        }

        AnnotationUtil.setWord(currentAnnotation, parts[0]);
        currentSentence.add(currentAnnotation);
        if (parts[0].equals("."))
            endSentence();

        for (int i = 1; i < parts.length; ++i) {
            if ("".equals(parts[i]))
                continue;
            currentAnnotation = new Annotation(currentAnnotation);
            AnnotationUtil.setWord(currentAnnotation, parts[i]);
            currentSentence.add(currentAnnotation);
            if (parts[i].equals("."))
                endSentence();
        }

        currentAnnotation = new Annotation();

    }

    public List<Sentence> sentences() {
        return sentences;
    }

    public static void main(String[] args) {
        SenseEvalAllWordsDocumentReader reader =
            new SenseEvalAllWordsDocumentReader();
        reader.parse(args[0]);
        for (Sentence sentence : reader.sentences())
            for (Annotation annot : sentence) {
                String id = annot.get(ValueAnnotation.class);
                if (id != null)
                    System.out.printf("%s %s\n", 
                                      id, AnnotationUtil.word(annot));
            }
    }
}
