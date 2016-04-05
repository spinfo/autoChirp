package autoChirp.preProcessing;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.PostConstruct;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * A wrapper-class for HeideltimeStandalone. Overrides the readConfigsFile()-
 * Method to avoid using a FileInputStrean
 *
 * @author Alena Geduldig
 *
 */
@Component
public class HeidelTimeWrapper extends HeidelTimeStandalone {

	@Value("${treeTaggerPath}")
	private String treeTaggerPathProp;

	private static String treeTaggerPath;

	/**
	 * get TreeTagger path
	 */
	@PostConstruct
	public void initializeWrapper() {
		this.treeTaggerPath = treeTaggerPathProp;
	}

	/**
	 *
	 * @param language
	 *            language to be processed with the created copy of HeidelTime
	 * @param typeToProcess
	 *            Domain type to be processed
	 * @param outputType
	 *            output type
	 * @param configPath
	 *            path to the configuration file for HeidlTime Standalone
	 * @param posTagger
	 *            POS Tagger to use for preprocessing
	 * @param doIntervalTagging
	 *            whether or not to invoke the IntervalTagger
	 */

	public HeidelTimeWrapper(Language language, DocumentType typeToProcess, OutputType outputType, String configPath,
			POSTagger posTagger, Boolean doIntervalTagging) {

		setLanguage(language);
		setDocumentType(typeToProcess);
		setOutputType(outputType);
		setPosTagger(posTagger);
		readConfigFile(configPath);
		initialize(language, typeToProcess, outputType, configPath, posTagger, doIntervalTagging);

	}

	/**
	 * @param configPath
	 *            Path to the configuration file for HeidelTime Standalone
	 */
	public static void readConfigFile(String configPath) {
		InputStream configStream = null;
		try {
			configStream = HeidelTimeWrapper.class.getResourceAsStream(configPath);
			Properties props = new Properties();
			props.load(configStream);
      props.setProperty("treeTaggerHome", treeTaggerPath);
			Config.setProps(props);
			configStream.close();
		} catch (FileNotFoundException e) {
			System.exit(-1);
		} catch (IOException   e) {
			e.printStackTrace();
		}
	}
}
