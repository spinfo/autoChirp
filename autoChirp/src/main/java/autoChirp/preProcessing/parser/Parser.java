package autoChirp.preProcessing.parser;

import autoChirp.preProcessing.Document;

/**
 * 
 * A simple interface for a parser (e.g. WikipediaParser) Ensures that each
 * parser has to return a Document-object with text, title, url and language.
 * 
 * @author Alena Geduldig
 *
 */

public interface Parser {

	/**
	 * creates an object of class Document from an url.
	 * 
	 * @param url
	 *            the url of the website to parse
	 * @return an object of class Document consisting of text, title, url and
	 *         language
	 */
	public Document parse(String url);

}
