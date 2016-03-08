package autoChirp.preProcessing;

import java.io.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.unihd.dbs.heideltime.standalone.Config;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;



/**
 * @author Alena Geduldig
 *
 *
 * A wrapper-class for HeideltimeStandalone
 *
 * Overrides the readConfigsFile()- Method to avoid using a FileInputStrean
 *
 */
public class HeidelTimeWrapper extends HeidelTimeStandalone {

	/**
	 *
	 * @param language
	 * @param typeToProcess
	 * @param outputType
	 * @param configPath
	 * @param posTagger
	 * @param doIntervalTagging
	 */

	public HeidelTimeWrapper(Language language, DocumentType typeToProcess, OutputType outputType, String configPath, POSTagger posTagger, Boolean doIntervalTagging){

		setLanguage(language);
		setDocumentType(typeToProcess);
		setOutputType(outputType);
		setPosTagger(posTagger);
		readConfigFile(configPath);
		initialize(language, typeToProcess, outputType, configPath, posTagger, doIntervalTagging);

	}


	/**
	 * @param configPath 
	 *        - Path to config.props
	 */
	public static void readConfigFile(String configPath) {
		InputStream configStream = null;
		try {
			configStream = HeidelTimeWrapper.class.getResourceAsStream(configPath);
			Properties props = new Properties();
			props.load(configStream);
			Config.setProps(props);
			configStream.close();
		} catch (FileNotFoundException e) {
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
