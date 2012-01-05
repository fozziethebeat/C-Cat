package gov.llnl.ontology.text.corpora;

import gov.llnl.ontology.text.corpora.Article;
import gov.llnl.ontology.text.corpora.ArticleFactory;
import gov.llnl.ontology.text.corpora.BbcArticle;
	
/**
 *  Factory for creating BBC news articles.
 */
public class BbcArticleFactory implements ArticleFactory {
	
	/**
	 *  {@inheritDoc}
	 */
	public Article createArticle(String articleUrl) {
		return new BbcArticle(articleUrl);
	}
}