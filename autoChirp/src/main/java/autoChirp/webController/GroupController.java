package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
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

	@Value("${dateFormatsPath}")
	private String dateFormatsPath; 
	
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
		ModelAndView mv = new ModelAndView("group");
		mv.addObject("tweetGroup", tweetGroup);

		if (tweetsList.size() <= tweetsPerPage) {
			mv.addObject("tweetsList", tweetsList);
			return mv;
		}

		double pgnum = (double) tweetsList.size() / (double) tweetsPerPage;
		int pages = (pgnum > (int) pgnum) ? (int) (pgnum + 1.0) : (int) pgnum;
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

		if (!Arrays.asList("tsv-file", "wikipedia").contains(importer)) {
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
	 */
	@RequestMapping(value = "/import/tsv-file", method = RequestMethod.POST)
	public ModelAndView importTSVGroupPost(@RequestParam("source") MultipartFile source,
			@RequestParam("title") String title, @RequestParam("description") String description,
			@RequestParam("delay") int delay) {
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
		TweetFactory tweeter = new TweetFactory(dateFormatsPath);

		try {
			file = File.createTempFile("upload-", ".tsv");
			source.transferTo(file);
		} catch (Exception e) {
			return new ModelAndView("redirect:/error");
		}

		TweetGroup tweetGroup = tweeter.getTweetsFromTSVFile(file, title, description, (delay <= 0) ? 0 : delay);
		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);
		file.delete();

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
			mv.addObject("error", "The URL mmust be a valid (english or german) Wikipedia Article.");
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

		TweetFactory tweeter = new TweetFactory(dateFormatsPath);
		TweetGroup tweetGroup = tweeter.getTweetsFromUrl(source, new WikipediaParser(), description,
				(prefix == "") ? null : prefix);
		tweetGroup.title = title;

		int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

		return (groupID > 0) ? new ModelAndView("redirect:/groups/view/" + groupID)
				: new ModelAndView("redirect:/error");
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /groups/edit/$groupid. This method is responsible to present the
	 * group-creation form with all values prefilled into the according
	 * input-field. As such the form is re-used to edit a already created
	 * group.
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

		ModelAndView mv = new ModelAndView("group");
		mv.addObject("tweetGroup", tweetGroup);

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

		DBConnector.editGroup(groupID, title, description, userID);

		return new ModelAndView("redirect:/groups/view/" + groupID);
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
	public String toggleGroup(@PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
		boolean enabled = !tweetGroup.enabled;
		DBConnector.updateGroupStatus(groupID, enabled, userID);

		if (enabled)
			TweetScheduler.scheduleTweetsForUser(tweetGroup.tweets, userID);

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
			referer = new URI(request.getHeader("referer")).getPath();
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

}
