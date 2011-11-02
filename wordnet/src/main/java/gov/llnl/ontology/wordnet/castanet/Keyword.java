package gov.llnl.ontology.wordnet.castanet;


/**
 *  Represents a keyword along with its score.
 * 
 *  @author thuang513@gmail.com (Terry Huang)
 */
class Keyword implements Comparable<Keyword> {

	private double score;
	private String word;
    
	public Keyword(String theWord, double score) {
		this.score = score;
		this.word = theWord;
	}

	public int compareTo(Keyword other){
		return Double.compare(getScore(), other.getScore());
	}

	public double getScore() {
		return score;
	}
	
	public String getWord() {
		return word;
	}

	public String toString() {
		return word + " - " + score;
	} 

}