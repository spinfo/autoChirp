package autoChirp.tweetCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoChirp.preProcessing.Document;
import autoChirp.preProcessing.HeidelTimeWrapper;
import autoChirp.preProcessing.SentenceSplitter;
import autoChirp.preProcessing.parser.Parser;
import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import jxl.write.DateTime;

/**
 * @author Alena Geduldig
 * 
 *         A class to generate Tweets and TweetGroups from different input-data
 *
 */

public class TweetFactory {

	// is needed for tweetDate creation
	private int currentYear;
	List<String> dateTimeRegexes;
	List<String> dateRegexes;
	List<String> dateFormats;
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


	public TweetFactory() {
		currentYear = LocalDateTime.now().getYear();
		dateTimeRegexes = new ArrayList<String>();
		dateRegexes = new ArrayList<String>();
		dateFormats = new ArrayList<String>();
		//2016-12-08 12:00:00
		addDateTimeForm("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}","yyyy-MM-dd HH:mm:ss");
		//2016-12-08 12:00
		addDateTimeForm("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}","yyyy-MM-dd HH:mm");
		addDateTimeForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2}", "dd.MM.yyyy HH:mm:ss");
		addDateTimeForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}", "dd.MM.yyyy HH:mm");
		//2016-12-08
		addDateForm("[0-9]{4}-[0-9]{2}-[0-9]{2}","yyyy-MM-dd");
		//2016-12
		addDateForm("[0-9]{4}-[0-9]{2}","yyyy-MM");
		//30.10.2015
		addDateForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}", "dd.MM.yyyy");
		//30.10.15
		addDateForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}", "dd.MM.yy");
		//2016/10/31
		addDateForm("[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}", "dd/MM/yyyy");
	}
	
	private void addDateTimeForm(String regex, String format) {
		dateTimeRegexes.add(regex);
		dateFormats.add(format);
	}
	private void addDateForm(String regex, String format) {
		dateRegexes.add(regex);
		dateFormats.add(format);
	}

	public TweetGroup getTweetsFromCSV(File csvFile, String title, String description, int delay){
		TweetGroup group = new TweetGroup(title, description);
		try {
			BufferedReader in = new BufferedReader(new FileReader(csvFile));
			String line = in.readLine();
			String content;
			String date;
			String time;
			LocalDateTime ldt;
			Tweet tweet;
			while(line!= null){
				String[] split = line.split("\t");
				date = split[0].trim();
				time = split[1].trim();
				if(time.equals("")){
					ldt = parseDateString(date);
				}
				else{
					ldt = parseDateString(date+" "+time);
				}
				if(ldt == null){
					line = in.readLine();
					continue;
				}
				ldt = ldt.plusYears(delay);
				String formattedDate = ldt.format(formatter);
				boolean midnight = false;
				if (time.contains(" 00:00")) {
					midnight = true;
				}
				if (!midnight) {
					formattedDate = formattedDate.replace(" 00:00", " 12:00");
				}
				content = split[2];
				content = trimToTweet(content, null);
				if(delay == 0){
					while(ldt.isBefore(LocalDateTime.now())) {
						ldt = ldt.plusYears(1);
					}
				}
				if(ldt.isAfter(LocalDateTime.now())){
					tweet = new Tweet(formattedDate, content);
					group.addTweet(tweet);
				}		
				line = in.readLine();
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return group;
	}

	
	


	/**
	 * creates a TweetGroup-Object from the given url. 1. Creates a
	 * Document-object with the given Parser 2. Splits text into sentences using
	 * the SentenceSplitter 3. Tags dates using HeidelTime 4. Calculates the
	 * tweetDate for each tagged date and initialize Tweet-object 5. Initializes
	 * TweetGroup
	 * 
	 * @param url
	 * @param parser
	 *            - the appropriate parser for the given url (e.g.
	 *            WikipediaParser for Wikipedia-urls)
	 * @param description
	 *            - description-attribute for the created TweetGroup
	 * @return a new TweetGroup-object
	 */
	public TweetGroup getTweetsFromUrl(String url, Parser parser, String description, String prefix) {
		Document doc = createDocument(url, parser);
		String[] processedSentences = tagDatesWithHeideltime(doc);
		List<Tweet> tweets = new ArrayList<Tweet>();
		for (int i = 0; i < processedSentences.length; i++) {
			String sentence = processedSentences[i];
			List<String> origDates = extractDates(sentence);
			Tweet tweet;
			String content;
			for (String date : origDates) {
				String tweetDate = getTweetDate(date);
				if (tweetDate == null)
					continue;
				if(prefix != null){
					content = trimToTweet(prefix+": "+doc.getSentences().get(i - 1), url);
				}
				else {
					content = trimToTweet(doc.getSentences().get(i - 1), url);
				}
				tweet = new Tweet(tweetDate, content);
				tweets.add(tweet);
			}
		}	
		currentYear = LocalDateTime.now().getYear();
		TweetGroup group = new TweetGroup(doc.getTitle(), description);
		group.setTweets(tweets);
		return group;
	}

	/**
	 * @param document
	 * @return a list of date-tagged sentences
	 */
	private String[] tagDatesWithHeideltime(Document document) {
		System.out.println("  Doc: "+ document);
		HeidelTimeWrapper ht = new HeidelTimeWrapper(document.getLanguage(), DocumentType.NARRATIVES, OutputType.TIMEML,
				"/heideltime/config.props", POSTagger.TREETAGGER, false);
		String toProcess = concatSentences(document.getSentences());
		String processed;
		try {
			processed = ht.process(toProcess);
		} catch (DocumentCreationTimeMissingException e) {
			e.printStackTrace();
			return null;
		}
		return processed.split("#SENTENCE#");
	}

	/**
	 * @param url
	 * @param parser
	 * @return
	 */
	private Document createDocument(String url, Parser parser) {
		Document doc = parser.parse(url);
		SentenceSplitter splitter = new SentenceSplitter(doc.getLanguage());
		doc.setSentences(splitter.splitIntoSentences(doc.getText()));
		return doc;
	}

	private String concatSentences(List<String> sentences) {
		StringBuffer sb = new StringBuffer();
		for (String s : sentences) {
			sb.append("#SENTENCE#" + s);
		}
		return sb.toString();
	}

	private String trimToTweet(String content, String url) {
		if (content.length() > 140) {
			content = content.substring(0, 115);
			content = content.substring(0, content.lastIndexOf(" "));
			if(url != null){
				content = content+" "+url;
			}
		}
		return content;
	}

	private List<String> extractDates(String processed) {
		List<String> dates = new ArrayList<String>();
		String dateRegex = "type=\"DATE\" value=\"([0-9|XXXX]{4}-[0-9]{2}(-[0-9]{2})?)\">";
		String timeRegex = "type=\"TIME\" value=\"(([0-9]{4}|XXXX)-[0-9]{2}(-[0-9]{2})?)(( [A-Z]{2,4})|(T[0-9]{2}:[0-9]{2}(:[0-9]{2})?))\">";
		Pattern pattern = Pattern.compile(dateRegex);
		Matcher matcher = pattern.matcher(processed);
		Pattern pattern2 = Pattern.compile(timeRegex);
		Matcher matcher2 = pattern2.matcher(processed);
		String date = null;
		String time = null;
		while (matcher.find()) {
			date = matcher.group(1);
			// only date with at least a month
			if (date.contains("-")) {
				dates.add(date);
			}
			while (matcher2.find()) {
				time = matcher2.group(1) + matcher2.group(6).replace("T", " ");
				dates.add(time);
			}
		}
		return dates;
	}

	private String getTweetDate(String origDate) {
		boolean midnight = false;
		if (origDate.contains(" 00:00")) {
			midnight = true;
		}
		if (origDate.startsWith("XXXX")) {
			origDate = origDate.replace("XXXX", currentYear+"");
		}
		if(origDate.length()==7){
			origDate = origDate.concat("-01");
		}
		LocalDateTime ldtOriginal = parseDateString(origDate);
		if (ldtOriginal == null)
			return null;

		LocalDateTime ldt = LocalDateTime.of(currentYear, ldtOriginal.getMonth(), ldtOriginal.getDayOfMonth(),
				ldtOriginal.getHour(), ldtOriginal.getMinute());
		LocalDateTime today = LocalDateTime.now();
		if (ldt.isBefore(today)) {
			ldt = ldt.plusYears(1);
		}
		String formattedDate = ldt.format(formatter);
		if (!midnight) {
			formattedDate = formattedDate.replace(" 00:00:00", " 12:00:00");
		}
		return formattedDate;
	}


	private LocalDateTime parseDateString(String date) {
		LocalDateTime ldt;
		LocalDate ld;
		Pattern pattern;
		Matcher matcher;
		for (int i = 0; i < dateTimeRegexes.size(); i++) {
			String regex = dateTimeRegexes.get(i);
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(date);
			if(matcher.find()){
				DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(i));
				ldt = LocalDateTime.parse(date, dtFormatter);
				return ldt;
			}
		}
		int dateTimes = dateTimeRegexes.size();
		for (int j = 0; j < dateRegexes.size(); j++) {
			String regex = dateRegexes.get(j);
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(date);
			if(matcher.find()){
				DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(j+dateTimes));
				ld = LocalDate.parse(date, dtFormatter);
				ldt = LocalDateTime.of(ld, LocalTime.of(12, 0));
				return ldt;
			}
		}
		return null;
	}
}
