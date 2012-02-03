package gov.llnl.ontology.text;


/**
 * @author Keith Stevens
 */
public class SimpleAnnotation implements Annotation {

    private String word;

    private String lemma;

    private String pos;

    private String sense;

    private int parent;

    private String relation;

    private int start;

    private int end;

    public SimpleAnnotation(String word) {
        this(word, null, -1, -1, null, null, -1);
    }

    public SimpleAnnotation(String word, String pos) {
        this(word, pos, -1, -1, null, null, -1);
    }

    public SimpleAnnotation(String word, String pos, int start, int end) {
        this(word, pos, start, end, null, null, -1);
    }

    public SimpleAnnotation(String word, String pos, int start, int end,
                            String sense) {
        this(word, pos, start, end, sense, null, -1);
    }

    public SimpleAnnotation(String word, String pos, int start, int end,
                            String sense, String relation, int parent) {
        this.word = word;
        this.pos = pos;
        this.start = start;
        this.end = end;
        this.sense = sense;
        this.relation = relation;
        this.parent = parent;
    }

    public boolean hasDependencyParent() {
        return parent != -1;
    }

    public int dependencyParent() {
        return parent;
    }

    public void setDependencyParent(int parent) {
        this.parent = parent;
    }

    public boolean hasDependencyRelation() {
        return relation != null;
    }

    public String dependencyRelation() {
        return relation;
    }

    public void setDependencyRelation(String relation) {
        this.relation = relation;
    }

    public boolean hasWord() {
        return word != null;
    }

    public String word() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean hasLemma() {
        return lemma != null || word != null;
    }

    public String lemma() {
        return (lemma == null) ? word : lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public boolean hasPos() {
        return pos != null;
    }

    public String pos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public boolean hasSense() {
        return sense != null;
    }

    public String sense() {
        return sense;
    }

    public void setSense(String sense) {
        this.sense = sense;
    }

    public boolean hasSpan() {
        return start != -1;
    }

    public int end() {
        return end;
    }

    public int start() {
        return start;
    }

    public void setSpan(int start, int end) {
        this.start = start;
        this.end = end;
    }
}
