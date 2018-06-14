package autoChirp.webController;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.MalformedTSVFileException;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;

/**
 * A Spring MVC controller, responsible for serving /groups. This controller
 * implements the logic to manage groups from the web-UI. It loosely implements
 * all CRUD methods and is strongly tied to all template- views.
 *
 * Every method uses the injected HttpSession object to check for an active user
 * account. If no account is found in the session, the user is redirected to a
 * genuin error/login page.
 *
 * @author Philip Schildkamp
 */
@Controller
@RequestMapping(value = "/groups")
public class GroupController {

	@Value("${autochirp.parser.uploadtemp}")
	private String uploadtemp;
	
	@Value("${autochirp.flashcards.directory}")
	private String flashcardDir;

	@Value("${autochirp.parser.dateformats}")
	private String dateformats;

	private HttpSession session;
	private int groupsPerPage = 15;
	private int tweetsPerPage = 15;

	/**
	 * Constructor method, used to autowire and inject the HttpSession object.
	 *
	 * @param session
	 *            Autowired HttpSession object
	 */
	@Inject
	public GroupController(HttpSession session) {
		this.session = session;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /groups/view. This
	 * method provides the returned view with all groups, read from the
	 * database, chunking the results to support pagination.
	 *
	 * @param page
	 *            Request param containing the page number, defaults to 1
	 * @return View containing the groups overview
	 */
	@RequestMapping(value = "/view")
	public ModelAndView viewGroups(@RequestParam(name = "page", defaultValue = "1") int page) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		List<Integer> tweetGroupIDs = DBConnector.getGroupIDsForUser(userID);
		List<TweetGroup> tweetGroups = new ArrayList<TweetGroup>();
		ModelAndView mv = new ModelAndView("groups");

		for (int groupID : tweetGroupIDs)
			tweetGroups.add(DBConnector.getTweetGroupForUser(userID, groupID));

		if (tweetGroups.size() <= groupsPerPage) {
			mv.addObject("tweetGroups", tweetGroups);
			return mv;
		}

		List<TweetGroup> pageGroupList;
		double pgnum = (double) tweetGroups.size() / (double) groupsPerPage;
		int pages = (pgnum > (int) pgnum) ? (int) (pgnum + 1.0) : (int) pgnum;
		while(page > pages ){
			page--;
		}
		int offset = (page - 1) * groupsPerPage;
		int endset = (offset + groupsPerPage <= tweetGroups.size()) ? offset + groupsPerPage : tweetGroups.size();

		mv.addObject("tweetGroups", tweetGroups.subList(offset, endset));
		mv.addObject("page", page);
		mv.addObject("pages", pages);
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/view/$groupid. This method is called to view a single group and
	 * its Tweets in detail. It reads all relevant information from the database
	 * and displays it as a view. If no group with the requested ID is found, an
	 * error is displayed.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @param page
	 *            Request param containing the page (of Tweets), defaults to 1
	 * @return View containing details for one group and its Tweets
	 */
	@RequestMapping(value = "/view/{groupID}")
	public ModelAndView viewGroup(@PathVariable int groupID,
			@RequestParam(name = "page", defaultValue = "1") int page) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

		if (tweetGroup == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A group with the ID #" + groupID + " does not exist.");
			return mv;
		}
		List<Tweet> tweetsList = tweetGroup.tweets;
		//reorder tweetsList: upcoming tweets first - past tweets second
		LocalDateTime now = LocalDateTime.now();
		List<Tweet> old = new ArrayList<Tweet>();
		List<Tweet> upcoming = new ArrayList<Tweet>();
		for (Tweet tweet : tweetsList) {
			DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime tweetDate = LocalDateTime.parse(tweet.tweetDate, dtFormatter);
			if(tweetDate.isBefore(now)){
				old.add(tweet);
			}
			else{
				upcoming.add(tweet);
			}	
		}
		tweetsList.removeAll(old);
		tweetsList.addAll(old);
		ModelAndView mv = new ModelAndView("group");
		mv.addObject("tweetGroup", tweetGroup);

		if (tweetsList.size() <= tweetsPerPage) {
			mv.addObject("tweetsList", tweetsList);
			return mv;
		}

		double pgnum = (double) tweetsList.size() / (double) tweetsPerPage;
		int pages = (pgnum > (int) pgnum) ? (int) (pgnum + 1.0) : (int) pgnum;
		while(page > pages ){
			page--;
		}
		int offset = (page - 1) * tweetsPerPage;
		int endset = (offset + tweetsPerPage <= tweetsList.size()) ? offset + tweetsPerPage : tweetsList.size();
		mv.addObject("tweetsList", tweetsList.subList(offset, endset));
		mv.addObject("page", page);
		mv.addObject("pages", pages);
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /groups/add. This
	 * method returns a view containing the form to add a new, empty group.
	 *
	 * @return View containing the group-creation form
	 */
	@RequestMapping(value = "/add")
	public ModelAndView addGroup() {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");

		ModelAndView mv = new ModelAndView("group");
		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving /groups/add. This
	 * method gets POSTed as the group-creation form is submitted. All input-
	 * field values are passed as parameters and checked for validity. Upon
	 * success a new group is added to the database.
	 *
	 * @param title
	 *            POST param bearing the referenced input-field value
	 * @param description
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ModelAndView addGroupPost(@RequestParam("title") String title,
			@RequestParam("description") String description) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}

		TweetGroup tweetGroup = new TweetGroup(title, description);
		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

		return (groupID > 0) ? new ModelAndView("redirect:/groups/view/" + groupID)
				: new ModelAndView("redirect:/error");
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/import/$importer. This method returns a view containing the form
	 * to import a group. Depending on the $importer path param, the view
	 * behaves differently; if an unknown importer-type is requested, an error
	 * is shown.
	 *
	 * @param importer
	 *            Path param containing the importer-type
	 * @return View containing the group-import form
	 */
	@RequestMapping(value = "/import/{importer}")
	public ModelAndView importGroup(@PathVariable String importer) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");

		if (!Arrays.asList("gdrive", "tsv-file", "wikipedia").contains(importer)) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "An importer of type " + importer + " does not exist.");
			return mv;
		}

		ModelAndView mv = new ModelAndView("import");
		mv.addObject("importer", importer);

		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /groups/import/gdrive. This method gets POSTed as the GoogleDrive-import
	 * form is submitted. All input-field values are passed as parameters and
	 * checked for validity. The URL of the GoogleDocs-spreadsheet is validated
	 * against a basic regex and then further processed. As the corresponding
	 * TSV-file is successfully downloaded, it gets passed to the
	 * TSV-file-parser and then inserted into the database.
	 *
	 * @param source
	 *            POST param bearing the Wikipedia-article URL
	 * @param title
	 *            POST param bearing the referenced input-field value
	 * @param description
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 * @throws Exception
	 */
	@RequestMapping(value = "/import/gdrive", method = RequestMethod.POST)
	public ModelAndView importGdriveGroupPost(@RequestParam("source") String source,
			@RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("encoding") String encoding, @RequestParam(name = "delay", defaultValue = "0") int delay) throws Exception {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (!source.matches("https?:\\/\\/docs\\.google\\.com\\/spreadsheets\\/d\\/.*")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The URL must be a valid Spreadsheet from GoogleDrive.");
			return mv;
		}

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}

		Pattern pattern = Pattern.compile("https?:\\/\\/docs\\.google\\.com\\/spreadsheets\\/d\\/([^/]*)");
		Matcher matcher = pattern.matcher(source);

		if (!matcher.find()) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The documentID could not be extracted form the given source URL [" + source + "].");
			return mv;
		}

		URL url = new URL("https://docs.google.com/spreadsheets/d/" + matcher.group(1) + "/export?exportFormat=tsv");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.connect();

		if (connection.getResponseCode() != 200) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The file could not be read, sure it's accessmode is set to public?");
			return mv;
		}

		File file;
		TweetGroup tweetGroup;
		TweetFactory tweeter = new TweetFactory(dateformats);

		try {
			file = File.createTempFile("upload-", ".tsv", new File(uploadtemp));
			Files.copy(url.openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The downloaded file could not be opened.");
			return mv;
		}

		try {
			tweetGroup = tweeter.getTweetsFromTSVFile(file, title, description, (delay <= 0) ? 0 : delay, encoding);
		} catch (MalformedTSVFileException e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "Parsing error, " + e.getMessage());
			return mv;
		}

		List<Tweet> trimmed = new ArrayList<Tweet>();
		for (Tweet t : tweetGroup.tweets)
				if(t.adjustedLength() > Tweet.MAX_TWEET_LENGTH)
					trimmed.add(t);
		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);
		file.delete();

		if (!trimmed.isEmpty()) {
			// String trim = new String();
			// tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
			// for (Tweet t : tweetGroup.tweets)
			// for (Tweet u : trimmed)
			// if (t.compareTo(u) == 0)
			// trim += trim.isEmpty() ? t.tweetID : "," + t.tweetID;

			ModelAndView mv = new ModelAndView("confirm");
			mv.addObject("next", "/groups/view/" + groupID);
			mv.addObject("confirm", "Attention! Some of the imported Tweets exeed Twitters "+Tweet.MAX_TWEET_LENGTH+" character limit. "
							+ "For Your conveniance the full text will be atteched to those Tweets as an image. "
							+ "Those Tweets are highlighted on the next page.");

			return mv;
		}

		return (groupID > 0) ? new ModelAndView("redirect:/groups/view/" + groupID)
				: new ModelAndView("redirect:/error");
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /groups/import/tsv-file. This method gets POSTed as the tsv-import form
	 * is submitted. All input- field values are passed as parameters and
	 * checked for validity. The tsv-file itself is passed as MultipartFile and
	 * later transfered to a temporary file. This file is passed to the
	 * TweetFactory, which returns a parsed TweetGroup, which then is inserted
	 * to the database and shown to the user.
	 *
	 * @param source
	 *            POST param bearing the tsv-MultipartFile
	 * @param title
	 *            POST param bearing the referenced input-field value
	 * @param description
	 *            POST param bearing the referenced input-field value
	 * @param delay
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 * @throws MalformedTSVFileException
	 */
	@RequestMapping(value = "/import/tsv-file", method = RequestMethod.POST)
	public ModelAndView importTSVGroupPost(@RequestParam("source") MultipartFile source,
			@RequestParam("encoding") String encoding, @RequestParam("title") String title,
			@RequestParam("description") String description, @RequestParam(name = "delay", defaultValue = "0") int delay)
			throws MalformedTSVFileException {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}

		File file;
		TweetGroup tweetGroup;
		TweetFactory tweeter = new TweetFactory(dateformats);

		try {
			file = File.createTempFile("upload-", ".tsv", new File(uploadtemp));
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(source.getBytes());
			fos.close();
		} catch (Exception e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The uploaded file could not be opened.");
			return mv;
		}

		try {
			tweetGroup = tweeter.getTweetsFromTSVFile(file, title, description, (delay <= 0) ? 0 : delay, encoding);
		} catch (MalformedTSVFileException e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "Parsing error, " + e.getMessage());
			return mv;
		}

		List<Tweet> trimmed = new ArrayList<Tweet>();
		for (Tweet t : tweetGroup.tweets)
				if(t.adjustedLength() > Tweet.MAX_TWEET_LENGTH)
					trimmed.add(t);

		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);
		file.delete();

		if (!trimmed.isEmpty()) {
			// String trim = new String();
			// tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
			// for (Tweet t : tweetGroup.tweets)
			// for (Tweet u : trimmed)
			// if (t.compareTo(u) == 0)
			// trim += trim.isEmpty() ? t.tweetID : "," + t.tweetID;

			ModelAndView mv = new ModelAndView("confirm");
			mv.addObject("next", "/groups/view/" + groupID);
			mv.addObject("confirm", "Attention! Some of the imported Tweets exeed Twitters "+Tweet.MAX_TWEET_LENGTH+" character limit. "
							+ "For Your conveniance the full text will be atteched to those Tweets as an image. "
							+ "Those Tweets are highlighted on the next page.");

			return mv;
		}

		return (groupID > 0) ? new ModelAndView("redirect:/groups/view/" + groupID)
				: new ModelAndView("redirect:/error");
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /groups/import/wikipedia This method gets POSTed as the Wikipdia-import
	 * form is submitted. All input-field values are passed as parameters and
	 * checked for validity. The URL of the Wikipedia-article is validated
	 * against a basic regex and then passed to the TweetFactory, along with an
	 * according WikipediaParser-object, which returns a parsed TweetGroup,
	 * which then is inserted to the database and shown to the user.
	 *
	 * @param source
	 *            POST param bearing the Wikipedia-article URL
	 * @param title
	 *            POST param bearing the referenced input-field value
	 * @param prefix
	 *            POST param bearing the referenced input-field value
	 * @param description
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/import/wikipedia", method = RequestMethod.POST)
	public ModelAndView importWikipediaGroupPost(@RequestParam("source") String source,
			@RequestParam("title") String title, @RequestParam("prefix") String prefix,
			@RequestParam("description") String description) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (!source.matches("https?:\\/\\/(de|en)\\.wikipedia\\.org\\/wiki\\/.*")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The URL must be a valid (english or german) Wikipedia Article.");
			return mv;
		}

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (prefix.length() > 20) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group prefix may be no longer then 20 characters.");
			return mv;
		}

		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}

		TweetFactory tweeter = new TweetFactory(dateformats);
		TweetGroup tweetGroup = tweeter.getTweetsFromUrl(source, new WikipediaParser(), description,
				(prefix == "") ? null : prefix);
		tweetGroup.title = title;

		List<Tweet> trimmed = new ArrayList<Tweet>();
		for (Tweet t : tweetGroup.tweets)
				if(t.adjustedLength() > Tweet.MAX_TWEET_LENGTH)
					trimmed.add(t);

		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

		if (!trimmed.isEmpty()) {
			// String trim = new String();
			// tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
			// for (Tweet t : tweetGroup.tweets)
			// for (Tweet u : trimmed)
			// if (t.compareTo(u) == 0)
			// trim += trim.isEmpty() ? t.tweetID : "," + t.tweetID;

			ModelAndView mv = new ModelAndView("confirm");
			mv.addObject("next", "/groups/view/" + groupID);
			mv.addObject("confirm", "Attention! Some of the imported Tweets exeed Twitters "+Tweet.MAX_TWEET_LENGTH+" character limit. "
							+ "For Your conveniance the full text will be atteched to those Tweets as an image. "
							+ "Those Tweets are highlighted on the next page.");

			return mv;
		}

		return (groupID > 0) ? new ModelAndView("redirect:/groups/view/" + groupID)
				: new ModelAndView("redirect:/error");
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/edit/$groupid. This method is responsible to present the
	 * group-creation form with all values prefilled into the according
	 * input-field. As such the form is re-used to edit a already created group.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return View containing the group-creation form with prefilled values
	 */
	@RequestMapping(value = "/edit/{groupID}")
	public ModelAndView editGroup(@PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
		boolean longTweet = false;
		for (Tweet tweet : tweetGroup.tweets) {
			if(tweet.adjustedLength() > Tweet.MAX_TWEET_LENGTH){
				longTweet = true;
				break;
			}
		}
		String[] flashcards = null;
		if(longTweet){
			File file = new File(flashcardDir);
			flashcards = file.list();
		}
		
		ModelAndView mv = new ModelAndView("group");
		mv.addObject("tweetGroup", tweetGroup);
		mv.addObject("flashcards", flashcards);
		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /groups/edit/$groupid. This method gets POSTed as the group-editing form
	 * is submitted. All input- field values are passed as parameters and
	 * checked for validity. Upon success the referenced group gets updated in
	 * the database or an error is shown.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @param title
	 *            POST param bearing the referenced input-field value
	 * @param description
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/edit/{groupID}", method = RequestMethod.POST)
	public ModelAndView editGroupPost(@PathVariable int groupID, @RequestParam("title") String title,
			@RequestParam("description") String description,@RequestParam(name = "flashcard", defaultValue = "paper") String flashcard) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}
		DBConnector.editGroup(groupID, title, description, userID, flashcard);
		return new ModelAndView("redirect:/groups/view/" + groupID);
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/copy/next/$groupid. This method copies the referenced group one
	 * year into the future and redirects the user to the newly created group.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return Redirect-view to the copied group overview
	 */
	@RequestMapping(value = "/copy/year/{groupID}")
	public String copyGroupYear(@PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";

		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		TweetGroup baseGroup = DBConnector.getTweetGroupForUser(userID, groupID);
		TweetGroup tweetGroup = DBConnector.createRepeatGroupInYears(baseGroup, userID, 1);
		int newGroupID = DBConnector.insertTweetGroup(tweetGroup, userID);

		return "redirect:/groups/view/" + newGroupID;
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/copy/date/$groupid. This method prompts the user for a reference
	 * Tweet from the group referenced by $groupid and a new scheduling for that
	 * Tweet. Then the user can copy and shift the scheduling of the referenced
	 * group.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @param page
	 *            Request param containing the page number, defaults to 1
	 * @return Redirect-view to the copied group overview
	 */
	@RequestMapping(value = "/copy/date/{groupID}")
	public ModelAndView copyGroupDate(@PathVariable int groupID,
			@RequestParam(name = "page", defaultValue = "1") int page) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");

		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

		if (tweetGroup == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A group with the ID #" + groupID + " does not exist.");
			return mv;
		}

		List<Tweet> tweetsList = tweetGroup.tweets;
		ModelAndView mv = new ModelAndView("copy");
		mv.addObject("tweetGroup", tweetGroup);

		if (tweetsList.size() <= tweetsPerPage) {
			mv.addObject("tweetsList", tweetsList);
			return mv;
		}

		double pgnum = (double) tweetsList.size() / (double) tweetsPerPage;
		int pages = (pgnum > (int) pgnum) ? (int) (pgnum + 1.0) : (int) pgnum;
		while(page > pages ){
			page--;
		}
		int offset = (page - 1) * tweetsPerPage;
		int endset = (offset + tweetsPerPage <= tweetsList.size()) ? offset + tweetsPerPage : tweetsList.size();

		mv.addObject("tweetsList", tweetsList.subList(offset, endset));
		mv.addObject("page", page);
		mv.addObject("pages", pages);
		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /groups/copy/date/$groupid. This method does the actual copying and
	 * shifting of the referenced TweetGroup and redirects the user to the newly
	 * created group.
	 * 
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @param title
	 *            Title of the newly created TweetGroup
	 * @param referenceDate
	 *            new date-scheduling of the referenced Tweet
	 * @param referenceTime
	 *            new time-scheduling of the referenced Tweet
	 * @param referenceTweetID
	 *            ID reference to a Tweet within the TweetGroup
	 * @return redirect-view to the newly created and shifted TweetGroup
	 */
	@RequestMapping(value = "/copy/date/{groupID}", method = RequestMethod.POST)
	public ModelAndView copyGroupDatePost(@PathVariable int groupID, @RequestParam("title") String title,
			@RequestParam("referenceDate") String referenceDate, @RequestParam("referenceTime") String referenceTime,
			@RequestParam("referenceTweetID") int referenceTweetID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");

		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

		if (title.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group title may be no longer then 255 characters.");
			return mv;
		}

		if (!referenceDate.matches("^[0-9]{4}(-[0-9]{2}){2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet date must match the pattern: YYYY-MM-DD");
			return mv;
		}

		if (referenceTime.matches("^[0-9]{2}:[0-9]{2}$")) {
			referenceTime = referenceTime + ":00";
		}

		else if (!referenceTime.matches("^[0-9]{2}:[0-9]{2}:[0-9]{2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet time must match the pattern: HH:MM:SS");
			return mv;
		}

		Tweet tweetEntry = null;
		for (Tweet t : tweetGroup.tweets)
			if (t.tweetID == referenceTweetID)
				tweetEntry = DBConnector.getTweetByID(t.tweetID, userID);

		if (tweetEntry == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "Error selecting correct reference Tweet");
			return mv;
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.parse(tweetEntry.tweetDate, formatter);
		LocalDateTime then = LocalDateTime.parse(referenceDate + " " + referenceTime, formatter);

		TweetGroup newTweetGroup = DBConnector.createRepeatGroupInSeconds(tweetGroup, userID,
				(int) ChronoUnit.SECONDS.between(now, then), title);
		int newGroupID = DBConnector.insertTweetGroup(newTweetGroup, userID);

		return new ModelAndView("redirect:/groups/view/" + newGroupID);
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/toggle/$groupid. This method provides a way to toggle the
	 * activation-state of the group, referenced by $groupid. If the group is
	 * enabled after the toggle, the TweetScheduler is called to schedule all
	 * Tweets in the (now enabled) group.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return Redirect-view to the toggled group overview
	 */
	@RequestMapping(value = "/toggle/{groupID}")
	public String toggleGroup(@PathVariable int groupID, HttpServletRequest request) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
		boolean enabled = !tweetGroup.enabled;
		DBConnector.updateGroupStatus(groupID, enabled, userID);

		if (enabled)
			TweetScheduler.scheduleTweetsForUser(tweetGroup.tweets, userID);
		String referer = request.getHeader("Referer");
		return "redirect:" + referer;
	}
	
	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/threading/$groupid. This method provides a way to toggle the
	 * activation-state of the group, referenced by $groupid. If the group is
	 * enabled after the toggle, the TweetScheduler is called to schedule all
	 * Tweets in the (now enabled) group.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return Redirect-view to the toggled group overview
	 */
	@RequestMapping(value = "/threading/{groupID}")
	public String toggleThreading(@PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
		boolean threaded = !tweetGroup.threaded;
		DBConnector.setThreaded(groupID, userID, threaded);
		return "redirect:/groups/view/" + groupID;
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/delete/$groupid. This method presents the user with a
	 * confirmation dialog, before forwading to the actual deletion the the
	 * referenced group.
	 *
	 * @param request
	 *            Autowired HttpServletRequest object, containing header-fields
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return View containing confirmation dialog for the intended action
	 */
	@RequestMapping(value = "/delete/{groupID}")
	public ModelAndView deleteGroup(HttpServletRequest request, @PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		String referer;
		try {
			referer = new URI(request.getHeader("referer")).getPath().substring(request.getContextPath().length());
		} catch (URISyntaxException e) {
			referer = null;
		}

		ModelAndView mv = new ModelAndView("confirm");
		mv.addObject("confirm", "Do You want to delete Your group \"" + DBConnector.getGroupTitle(groupID, userID)
				+ "\" and all containing tweets?");
		if (!referer.matches("^/groups/.+?[0-9]$"))
			mv.addObject("referer", referer);

		return mv;
	}
	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/delete/$groupid/confirm. This method triggers the actual deletion
	 * the the referenced group and redirects to a referer, if applicable.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @param referer
	 *            Request param containing the referer to redirect to
	 * @return Redirect-view
	 */
	@RequestMapping(value = "/delete/{groupID}/confirm")
	public String confirmedDeleteGroup(@PathVariable int groupID,
			@RequestParam(name = "referer", defaultValue = "/groups/view") String referer) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		DBConnector.deleteGroup(groupID, userID);
		return "redirect:" + referer;
	}
	

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/delete. This method executes the deletion of all groups referred by grouIDs
	 *
	 * @param groupIDs
	 *            Request param containing a list of groupIDs
	 * @return groups overview
	 */
	@RequestMapping(value = "/delete", method= RequestMethod.POST)
	public ModelAndView deleteGroups(@RequestParam("groupID") List<Integer> groupIDs) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		for (Integer id : groupIDs) {
			DBConnector.deleteGroup(id, userID);
		}
		return new ModelAndView("redirect:/groups/view");
	}
	



}
