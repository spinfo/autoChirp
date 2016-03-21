package autoChirp.preProcessing;

import java.util.List;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;

/**
 * 
 * Represents a document created from an url. Every document consists of a text
 * (plain text of the website), url, title, and language.
 * 
 * @author Alena Geduldig
 */

public class Document {

	private String text;
	private String url;
	private String title;
	private List<String> sentences;
	private Language language;

	/**
	 * @param text
	 *            plain text of the website this document is created from
	 * @param url
	 *            the url of the website
	 * @param title
	 *            e.g. extracted from the header-element
	 * @param language
	 *            the language of the documents text
	 */
	public Document(String text, String url, String title, Language language) {
		this.text = text;
		this.url = url;
		this.title = title;
		this.language = language;
	}

	/**
	 * @return language
	 */
	public Language getLanguage() {
		return language;
	}

	/**
	 * @return text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @return url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param sentences
	 *            text, splitted into sentences e.g. by a SentenceSplitter
	 */
	public void setSentences(List<String> sentences) {
		this.sentences = sentences;
	}

	/**
	 * @return sentences (text, splitted into sentences)
	 */
	public List<String> getSentences() {
		return sentences;
	}

}
