package autoChirp.tweetCreation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

/**
 * @author Alena Geduldig
 * 
 *         A class to generate tweets and tweetGroups from different input-data
 *         (e.g. excel-files or urls )
 *
 */

public class TweetFactory {

	// the current year is needed to calculate the next possible tweet-date.
	private int currentYear;
	// regexes for the accepted date and time formats
	private List<String> dateTimeRegexes;
	// regexes for the accepted date formats
	private List<String> dateRegexes;
	// accepted date and time formats
	private List<String> dateFormats;
	// a formatter to normalize the different input formats
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	public TweetFactory() {
		// set current year
		currentYear = LocalDateTime.now().getYear();
		dateTimeRegexes = new ArrayList<String>();
		dateRegexes = new ArrayList<String>();
		dateFormats = new ArrayList<String>();
		// set accepted date and time formats
		// e.g. 2016-12-08 12:00:00
		addDateTimeForm("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}", "yyyy-MM-dd HH:mm:ss");
		// 2016-12-08 12:00
		addDateTimeForm("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}", "yyyy-MM-dd HH:mm");
		// 12.08.2016 12:24:13
		addDateTimeForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2}", "dd.MM.yyyy HH:mm:ss");
		// 12.08.2016 12:24
		addDateTimeForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}", "dd.MM.yyyy HH:mm");
		// 12.08.16 12:24
	    addDateTimeForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{2} [0-9]{2}:[0-9]{2}", "dd.MM.yy HH:mm");
		// 2016-12-08
		addDateForm("[0-9]{4}-[0-9]{2}-[0-9]{2}", "yyyy-MM-dd");
		// 2016-12
		addDateForm("[0-9]{4}-[0-9]{2}", "yyyy-MM");
		// 30.10.2015
		addDateForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}", "dd.MM.yyyy");
		// 30.10.15
		addDateForm("[0-9]{2}\\.[0-9]{2}\\.[0-9]{2}", "dd.MM.yy");
		// 2016/10/31
		addDateForm("[0-9]{4}\\/[0-9]{2}\\/[0-9]{2}", "dd/MM/yyyy");
	}

	/**
	 * 
	 * adds a new dateTime format
	 * 
	 * @param regex
	 *            - dateTime-regex
	 * @param format
	 *            - dateTime-format
	 */
	private void addDateTimeForm(String regex, String format) {
		dateTimeRegexes.add(regex);
		dateFormats.add(format);
	}

	/**
	 * adds a new date format
	 * 
	 * @param regex
	 *            - date-regex
	 * @param format
	 *            - date-format
	 */
	private void addDateForm(String regex, String format) {
		dateRegexes.add(regex);
		dateFormats.add(format);
	}

	/**
	 * creates a TweetGroup-object from an excel-file by building a tweet for
	 * each row, which is in the following format: [date][tabulator][time
	 * (optional)][tabulator][tweet-content]
	 * 
	 * @param excelFile
	 * @param title
	 *            - a title for the created tweetGroup
	 * @param description
	 *            - a description for the created tweetGroup
	 * @param delay
	 *            - the number of years between the given date in the file and
	 *            the calculated tweet-date
	 * @return a new tweetGroup with a tweet for each row in the file
	 */
	public TweetGroup getTweetsFromExcelFile(File excelFile, String title, String description, int delay) {
		TweetGroup group = new TweetGroup(title, description);
		try {
			BufferedReader in = new BufferedReader(new FileReader(excelFile));
			String line = in.readLine();
			String content;
			String date;
			String time;
			LocalDateTime ldt;
			Tweet tweet;
			while (line != null) {
				String[] split = line.split("\t");
				// get tweet-date
				date = split[0].trim();
				time = split[1].trim();
				if (time.equals("")) {
					ldt = parseDateString(date);
				} else {
					ldt = parseDateString(date + " " + time);
				}
				if (ldt == null) {
					// row was not in the correct format. go to next row
					line = in.readLine();
					continue;
				}
				// add delay
				ldt = ldt.plusYears(delay);
				// normalize date to the format yyyy-MM-dd HH:mm
				String formattedDate = ldt.format(formatter);
				// set default time to 12:00
				boolean midnight = false;
				if (time.contains(" 00:00")) {
					midnight = true;
				}
				if (!midnight) {
					formattedDate = formattedDate.replace(" 00:00", " 12:00");
				}
				// get tweet-content
				content = split[2];
				// trim content to max. 140 characters
				content = trimToTweet(content, null);
				// calc. next possible tweetDate
				if (delay == 0) {
					while (ldt.isBefore(LocalDateTime.now())) {
						ldt = ldt.plusYears(1);
					}
				}
				if (ldt.isAfter(LocalDateTime.now())) {
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
	 * Creates a TweetGroup-object from the given url. 1. Creates a
	 * Document-object with the given Parser 2. Splits the documents text into
	 * sentences using the SentenceSplitter and concatenates it again with a
	 * sentence-delimiter 3. Tags dates in the concatenated text using
	 * HeidelTime 4. Splits tagged sentences at the delimiter and extract
	 * date-strings for each sentence 5. Calculates the tweetDate (= next
	 * anniversary) for each tagged date 6. Created a new Tweet-object for each
	 * date and its containing sentence and adds it to the TweetGroup
	 * 
	 * @param url
	 * @param parser
	 *            - the appropriate parser for the given url (e.g.
	 *            WikipediaParser for Wikipedia-urls)
	 * @param description
	 *            - a description for the created TweetGroup
	 * @param prefix
	 *            - a prefix for each tweet in the created tweetGroup. Each
	 *            tweet-content will start with "[prefix]: " *
	 * @return a new TweetGroup-object
	 */
	public TweetGroup getTweetsFromUrl(String url, Parser parser, String description, String prefix) {
		// create document
		Document doc = parser.parse(url);
		SentenceSplitter splitter = new SentenceSplitter(doc.getLanguage());
		doc.setSentences(splitter.splitIntoSentences(doc.getText()));
		// tag dates
		String[] processedSentences = tagDatesWithHeideltime(doc);
		List<Tweet> tweets = new ArrayList<Tweet>();
		for (int i = 0; i < processedSentences.length; i++) {
			String sentence = processedSentences[i];
			// extract dates from sentence
			List<String> origDates = extractDates(sentence);
			Tweet tweet;
			String content;
			for (String date : origDates) {
				// calc. next possible tweet-date
				String tweetDate = getTweetDate(date);
				if (tweetDate == null)
					continue;
				// trim sentence to 140 characters
				if (prefix != null) {
					content = trimToTweet(prefix + ": " + doc.getSentences().get(i - 1), url);
				} else {
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
	 * returns a list of TimeML-annotated sentences
	 * 
	 * @param document
	 * @return list of tagged sentences
	 */
	private String[] tagDatesWithHeideltime(Document document) {
		HeidelTimeWrapper ht = new HeidelTimeWrapper(document.getLanguage(), DocumentType.NARRATIVES, OutputType.TIMEML,
				"/heideltime/config.props", POSTagger.TREETAGGER, false);
		String toProcess = concatenateSentences(document.getSentences());
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
	 * concatenates a list of sentences with the delimiter '#SENTENCE#'
	 * 
	 * @param sentences
	 * @return concatenated sentences
	 */
	private String concatenateSentences(List<String> sentences) {
		StringBuffer sb = new StringBuffer();
		for (String s : sentences) {
			sb.append("#SENTENCE#" + s);
		}
		return sb.toString();
	}

	/**
	 * trims a sentence to a tweet-lenth of max. 140 characters and adds the
	 * given url to the tweets content
	 * 
	 * @param toTrim
	 * @param url
	 * @return a valid tweet content
	 */
	private String trimToTweet(String toTrim, String url) {
		if (toTrim.length() > 140) {
			toTrim = toTrim.substring(0, 115);
			toTrim = toTrim.substring(0, toTrim.lastIndexOf(" "));
			if (url != null) {
				toTrim = toTrim + " " + url;
			}
		}
		return toTrim;
	}

	/**
	 * extract date-strings from a TimeML annotated sentence extracts only dates
	 * with at least a month specification
	 * 
	 * @param sentence
	 * @return a list of date-expressions
	 */
	private List<String> extractDates(String sentence) {
		List<String> dates = new ArrayList<String>();
		String dateRegex = "type=\"DATE\" value=\"([0-9|XXXX]{4}-[0-9]{2}(-[0-9]{2})?)\">";
		String timeRegex = "type=\"TIME\" value=\"(([0-9]{4}|XXXX)-[0-9]{2}(-[0-9]{2})?)(( [A-Z]{2,4})|(T[0-9]{2}:[0-9]{2}(:[0-9]{2})?))\">";
		Pattern pattern = Pattern.compile(dateRegex);
		Matcher matcher = pattern.matcher(sentence);
		Pattern pattern2 = Pattern.compile(timeRegex);
		Matcher matcher2 = pattern2.matcher(sentence);
		String date = null;
		String time = null;
		while (matcher.find()) {
			date = matcher.group(1);
			// select only date with at least a month
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

	/**
	 * calculates the next possible tweet-date (= next anniversary in the
	 * future) from the given date-expression (e.g. 1955-08-01 would return
	 * 2017-08-01 )
	 * 
	 * @param origDate
	 * @return next anniversary in the format YYYY-MM-dd HH:mm
	 */
	private String getTweetDate(String origDate) {
		// set default time to 12:00
		boolean midnight = false;
		if (origDate.contains(" 00:00")) {
			midnight = true;
		}
		// handle dates with unspecified year
		if (origDate.startsWith("XXXX")) {
			origDate = origDate.replace("XXXX", currentYear + "");
		}
		// set default day in month to 01
		if (origDate.length() == 7) {
			origDate = origDate.concat("-01");
		}
		LocalDateTime ldtOriginal = parseDateString(origDate);
		if (ldtOriginal == null)
			return null;
		// find next anniverary in the future
		LocalDateTime ldt = LocalDateTime.of(currentYear, ldtOriginal.getMonth(), ldtOriginal.getDayOfMonth(),
				ldtOriginal.getHour(), ldtOriginal.getMinute());
		LocalDateTime today = LocalDateTime.now();
		if (ldt.isBefore(today)) {
			ldt = ldt.plusYears(1);
		}
		// normalize date to the format YYYY-MM-dd HH:mm
		String formattedDate = ldt.format(formatter);
		if (!midnight) {
			formattedDate = formattedDate.replace(" 00:00", " 12:00");
		}
		return formattedDate;
	}

	/**
	 * parses a date-string and returns a LocalDateTime-object of the format
	 * YYYY-MM-dd HH:mm
	 * 
	 * @param date
	 * @return a LocalDateTime-object of the given date-string or null if the
	 *         string does not satisfy one of the accepted date-formats
	 */
	private LocalDateTime parseDateString(String date) {
		LocalDateTime ldt;
		LocalDate ld;
		Pattern pattern;
		Matcher matcher;
		for (int i = 0; i < dateTimeRegexes.size(); i++) {
			String regex = dateTimeRegexes.get(i);
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(date);
			if (matcher.find()) {
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
			if (matcher.find()) {
				DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern(dateFormats.get(j + dateTimes));
				ld = LocalDate.parse(date, dtFormatter);
				ldt = LocalDateTime.of(ld, LocalTime.of(12, 0));
				return ldt;
			}
		}
		return null;
	}
}
