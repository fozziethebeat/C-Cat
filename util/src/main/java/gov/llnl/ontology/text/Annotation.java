package gov.llnl.ontology.text;


/**
 * @author Keith Stevens
 */
public interface Annotation {

    boolean hasDependencyParent();

    int dependencyParent();

    void setDependencyParent(int parent);

    boolean hasDependencyRelation();

    String dependencyRelation();

    void setDependencyRelation(String relation);

    boolean hasWord();

    String word();

    void setWord(String word);

    boolean hasLemma();

    String lemma();

    void setLemma(String lemma);

    boolean hasPos();

    String pos();

    void setPos(String pos);

    boolean hasSense();

    String sense();

    void setSense(String sense);

    boolean hasSpan();

    int end();

    int start();

    void setSpan(int start, int end);
}
