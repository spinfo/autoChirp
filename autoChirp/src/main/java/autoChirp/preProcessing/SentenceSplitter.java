package autoChirp.preProcessing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

/**
 * 
 * A class to split text into sentences using the OpenNLP SentenceDetector
 * 
 * @author Alena Geduldig
 *
 */

public class SentenceSplitter {

	private SentenceModel sentenceModel;

	/**
	 * initializes a SentenceSplitter for the given language
	 * 
	 * @param language
	 *            language
	 */
	public SentenceSplitter(Language language) {
		setSentenceSplittingModel(language);
	}

	/**
	 * loads the SentenceModel for the given language
	 * 
	 * @param language
	 *            language
	 * 
	 */
	private void setSentenceSplittingModel(Language language) {
		String model = "/opennlp/" + language.toString() + "-sent.bin";
		InputStream modelIn = null;
		try {
			modelIn = getClass().getResourceAsStream(model);
			System.out.println(modelIn);
			sentenceModel = new SentenceModel(modelIn);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (modelIn != null) {
				try {
					modelIn.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * @param text
	 *            text to split
	 * @return text splitted into sentences
	 */
	public List<String> splitIntoSentences(String text) {
		String[] sentences = null;
		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		sentences = detector.sentDetect(text);
		return Arrays.asList(sentences);
	}
}
