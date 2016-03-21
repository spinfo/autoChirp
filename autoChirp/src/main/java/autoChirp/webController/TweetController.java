package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * A Spring MVC controller, responsible for serving /tweets. This controller
 * implements the logic to manage and schedule Tweets from the web-UI. It
 * loosely implements all CRUD methods and is strongly tied to all
 * template-views.
 *
 * Every method uses the injected HttpSession object to check for an active user
 * account. If no account is found in the session, the user is redirected to a
 * genuin error/login page.
 *
 * @author Philip Schildkamp
 */
@Controller
@RequestMapping(value = "/tweets")
public class TweetController {

	private HttpSession session;
	private int tweetsPerPage = 15;
	private int maxTweetLength = 140;

	/**
	 * Constructor method, used to autowire and inject the HttpSession object.
	 *
	 * @param session
	 *            Autowired HttpSession object
	 */
	@Inject
	public TweetController(HttpSession session) {
		this.session = session;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /tweets/view. This
	 * method provides the returned view with all necessary Tweets from the
	 * database, chunking the results to support pagination.
	 *
	 * @param page
	 *            Request param containing the page number, defaults to 1
	 * @return View containing the global Tweets overview
	 */
	@RequestMapping(value = "/view")
	public ModelAndView viewTweets(@RequestParam(name = "page", defaultValue = "1") int page) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		List<Tweet> tweetsList = DBConnector.getTweetsForUser(userID);
		ModelAndView mv = new ModelAndView("tweets");

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
	 * A HTTP GET request handler, responsible for serving
	 * /tweets/view/$tweetid. This method is called to view a single Tweet in
	 * detail. It reads all relevant information from the database and displays
	 * it as a view. If no Tweet with the requested ID is found, an error is
	 * displayed.
	 *
	 * @param tweetID
	 *            Path param containing an ID-reference to a Tweet
	 * @return View containing details for one Tweet
	 */
	@RequestMapping(value = "/view/{tweetID}")
	public ModelAndView viewTweet(@PathVariable int tweetID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);

		if (tweetEntry == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A tweet with the ID #" + tweetID + " does not exist.");
			return mv;
		}

		ModelAndView mv = new ModelAndView("tweet");
		mv.addObject("tweetEntry", tweetEntry);
		mv.addObject("tweetGroup", DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID));

		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /tweets/add. This
	 * method reads all groups from the database and feeds them to the view
	 * holding the form for Tweet-creation, thereby providing auto-completion
	 * for already created groups.
	 *
	 * @return View containing the Tweet-creation form
	 */
	@RequestMapping(value = "/add")
	public ModelAndView addTweet() {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		List<Integer> groupIDs = DBConnector.getGroupIDsForUser(userID);
		List<TweetGroup> tweetGroups = new ArrayList<TweetGroup>();

		for (int groupID : groupIDs)
			tweetGroups.add(DBConnector.getTweetGroupForUser(userID, groupID));

		ModelAndView mv = new ModelAndView("tweet");
		mv.addObject("tweetGroups", tweetGroups);

		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving /tweets/add. This
	 * method gets POSTed as the Tweet-creation form is submitted. All input-
	 * field values are passed as parameters and checked for validity. Upon
	 * success a new Tweet (possibly a new group, too) is added to the database.
	 * Because groups are referenced by ID but presented with their nicenames, a
	 * check for type-safety is employed to detect if the user wants the Tweet
	 * to be added to an existing (ID-referenced) or new (nicenamed) group.
	 *
	 * @param tweetGroup
	 *            POST param bearing the referenced input-field value
	 * @param content
	 *            POST param bearing the referenced input-field value
	 * @param tweetDate
	 *            POST param bearing the referenced input-field value
	 * @param tweetTime
	 *            POST param bearing the referenced input-field value
	 * @param imageUrl
	 *            POST param bearing the referenced input-field value
	 * @param latitude
	 *            POST param bearing the referenced input-field value
	 * @param longitude
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ModelAndView addTweetPost(@RequestParam("tweetGroup") String tweetGroup,
			@RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate,
			@RequestParam("tweetTime") String tweetTime, @RequestParam("imageUrl") String imageUrl,
			@RequestParam(name = "latitude", defaultValue = "0.0") float latitude,
			@RequestParam(name = "longitude", defaultValue = "0.0") float longitude) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		if (content.length() > maxTweetLength) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet content may be no longer then " + maxTweetLength + " characters.");
			return mv;
		}

		if (!tweetDate.matches("^[0-9]{4}(-[0-9]{2}){2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet date must match the pattern: YYYY-MM-DD");
			return mv;
		}

		if (!tweetTime.matches("^[0-9]{2}:[0-9]{2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet time must match the pattern: HH:MM");
			return mv;
		}

		if (!imageUrl.isEmpty()) {
			try {
				URL url = new URL(imageUrl);
			} catch (MalformedURLException e) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "The image URL must be a valid url.");
				return mv;
			}
		}

		if (latitude < -90 || latitude > 90) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The latitude must be withing the margins of -90 to +90.");
			return mv;
		}

		if (longitude < -180 || longitude > 180) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The longitude must be withing the margins of -180 to +180.");
			return mv;
		}

		Tweet tweetEntry = new Tweet(tweetDate + " " + tweetTime, content, imageUrl, longitude, latitude);
		boolean enabledGroup;
		int groupID;
		int tweetID;

		try {
			groupID = Integer.parseInt(tweetGroup);
			if (!DBConnector.getGroupIDsForUser(userID).contains(groupID))
				throw new Exception();

			enabledGroup = DBConnector.isEnabledGroup(groupID, userID);
			tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);
		} catch (Exception e) {
			Date now = new Date();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String date = format.format(now);

			TweetGroup newGroup = new TweetGroup(tweetGroup, "Shorthand-added: " + date);
			groupID = DBConnector.insertTweetGroup(newGroup, userID);
			tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);
			enabledGroup = false;
		}

		if (groupID < 0 || tweetID < 0)
			return new ModelAndView("redirect:/error");

		if (enabledGroup) {
			ModelAndView mv = new ModelAndView("confirm");
			mv.addObject("confirm",
					"Do You want to keep Your group \"" + DBConnector.getGroupTitle(groupID, userID) + "\" enabled?");
			mv.addObject("next", "/groups/toggle/" + groupID);
			mv.addObject("prev", "/groups/view/" + groupID);
			return mv;
		} else
			return new ModelAndView("redirect:/groups/view/" + groupID);
	}

	/**
	 * A HTTP GET request handler, responsible for serving /tweets/add/$groupid.
	 * This method is responsible to present the Tweet-creation form with the
	 * group prefilled into the according input-field.
	 *
	 * @param groupID
	 *            Path param containing an ID-reference to a group
	 * @return View containing the Tweet-creation form with prefilled group
	 */
	@RequestMapping(value = "/add/{groupID}")
	public ModelAndView addTweetToGroup(@PathVariable int groupID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

		ModelAndView mv = new ModelAndView("tweet");
		mv.addObject("tweetGroup", tweetGroup);

		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /tweets/add/$groupid. This method gets POSTed as the Tweet-creation form
	 * with prefilled group is submitted. All input-field values are passed as
	 * parameters and checked for validity. Upon success a new Tweet is added to
	 * the prefilled group in the database.
	 *
	 * @param groupID
	 *            POST param bearing the referenced input-field value
	 * @param content
	 *            POST param bearing the referenced input-field value
	 * @param tweetDate
	 *            POST param bearing the referenced input-field value
	 * @param tweetTime
	 *            POST param bearing the referenced input-field value
	 * @param imageUrl
	 *            POST param bearing the referenced input-field value
	 * @param latitude
	 *            POST param bearing the referenced input-field value
	 * @param longitude
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/add/{groupID}", method = RequestMethod.POST)
	public ModelAndView addTweetToGroupPost(@PathVariable int groupID, @RequestParam("content") String content,
			@RequestParam("tweetDate") String tweetDate, @RequestParam("tweetTime") String tweetTime,
			@RequestParam("imageUrl") String imageUrl,
			@RequestParam(name = "latitude", defaultValue = "0.0") float latitude,
			@RequestParam(name = "longitude", defaultValue = "0.0") float longitude) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		boolean enabledGroup = DBConnector.isEnabledGroup(groupID, userID);

		if (content.length() > maxTweetLength) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet content may be no longer then " + maxTweetLength + " characters.");
			return mv;
		}

		if (!tweetDate.matches("^[0-9]{4}(-[0-9]{2}){2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet date must match the pattern: YYYY-MM-DD");
			return mv;
		}

		if (!tweetTime.matches("^[0-9]{2}:[0-9]{2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet time must match the pattern: HH:MM");
			return mv;
		}

		if (!imageUrl.isEmpty()) {
			try {
				URL url = new URL(imageUrl);
			} catch (MalformedURLException e) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "The image URL must be a valid url.");
				return mv;
			}
		}

		if (latitude < -90 || latitude > 90) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The latitude must be withing the margins of -90 to +90.");
			return mv;
		}

		if (longitude < -180 || longitude > 180) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The longitude must be withing the margins of -180 to +180.");
			return mv;
		}

		Tweet tweetEntry = new Tweet(tweetDate + " " + tweetTime, content, imageUrl, longitude, latitude);
		int tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);

		if (tweetID < 0)
			return new ModelAndView("redirect:/error");

		if (enabledGroup) {
			ModelAndView mv = new ModelAndView("confirm");
			mv.addObject("confirm",
					"Do You want to keep Your group \"" + DBConnector.getGroupTitle(groupID, userID) + "\" enabled?");
			mv.addObject("next", "/groups/toggle/" + groupID);
			mv.addObject("prev", "/groups/view/" + groupID);
			return mv;
		} else
			return new ModelAndView("redirect:/groups/view/" + groupID);
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /tweets/edit/$tweetid. This method is responsible to present the
	 * Tweet-creation form with the group and date prefilled into the according
	 * input-field. As such the form is re- used to edit a already created
	 * Tweet.
	 *
	 * @param tweetID
	 *            Path param containing an ID-reference to a Tweet
	 * @return View containing the Tweet-creation form with prefilled group and
	 *         date
	 */
	@RequestMapping(value = "/edit/{tweetID}")
	public ModelAndView editTweet(@PathVariable int tweetID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
		TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID);

		if (tweetEntry.tweeted) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "You cannot edit a tweeted Tweet.");
			return mv;
		}

		ModelAndView mv = new ModelAndView("tweet");
		mv.addObject("tweetEntry", tweetEntry);
		mv.addObject("tweetGroup", tweetGroup);

		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving
	 * /tweets/edit/$tweetid. This method gets POSTed as the Tweet-editing form
	 * is submitted. All input- field values are passed as parameters and
	 * checked for validity. Upon success the referenced Tweet gets updated in
	 * the database or an error is shown.
	 *
	 * @param tweetID
	 *            Path param containing an ID-reference to a Tweet
	 * @param content
	 *            POST param bearing the referenced input-field value
	 * @param imageUrl
	 *            POST param bearing the referenced input-field value
	 * @param latitude
	 *            POST param bearing the referenced input-field value
	 * @param longitude
	 *            POST param bearing the referenced input-field value
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/edit/{tweetID}", method = RequestMethod.POST)
	public ModelAndView editTweetPost(@PathVariable int tweetID, @RequestParam("content") String content,
			@RequestParam("imageUrl") String imageUrl,
			@RequestParam(name = "latitude", defaultValue = "0.0") float latitude,
			@RequestParam(name = "longitude", defaultValue = "0.0") float longitude) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);

		if (tweetEntry.tweeted) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "You cannot edit a tweeted Tweet.");
			return mv;
		}

		if (content.length() > maxTweetLength) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet content may be no longer then " + maxTweetLength + " characters.");
			return mv;
		}

		if (!imageUrl.isEmpty()) {
			try {
				URL url = new URL(imageUrl);
			} catch (MalformedURLException e) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "The image URL must be a valid url.");
				return mv;
			}
		}

		if (latitude < -90 || latitude > 90) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The latitude must be withing the margins of -90 to +90.");
			return mv;
		}

		if (longitude < -180 || longitude > 180) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The longitude must be withing the margins of -180 to +180.");
			return mv;
		}

		DBConnector.editTweet(tweetID, content, userID, imageUrl, longitude, latitude);

		return new ModelAndView("redirect:/groups/view/" + tweetEntry.groupID);
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /tweets/delete/$tweetid. This method presents the user with a
	 * confirmation dialog, before forwading to the actual deletion the the
	 * referenced Tweet.
	 *
	 * @param request
	 *            Autowired HttpServletRequest object, containing header-fields
	 * @param tweetID
	 *            Path param containing an ID-reference to a Tweet
	 * @return View containing confirmation dialog for the intended action
	 */
	@RequestMapping(value = "/delete/{tweetID}")
	public ModelAndView deleteTweet(HttpServletRequest request, @PathVariable int tweetID) {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);

		String referer;
		try {
			referer = new URI(request.getHeader("referer")).getPath();
		} catch (URISyntaxException e) {
			referer = null;
		}

		ModelAndView mv = new ModelAndView("confirm");
		mv.addObject("confirm", "Do You want to delete Your tweet \"" + tweetEntry.content + "\" from Your group \""
				+ tweetEntry.groupName + "\"?");
		if (!referer.matches("^/tweets/.+?[0-9]$"))
			mv.addObject("referer", referer);

		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /tweets/delete/$tweetid/confirm. This method triggers the actual deletion
	 * the the referenced Tweet and redirects to a referer, if applicable.
	 *
	 * @param tweetID
	 *            Path param containing an ID-reference to a Tweet
	 * @param referer
	 *            Request param containing the referer to redirect to
	 * @return Redirect-view
	 */
	@RequestMapping(value = "/delete/{tweetID}/confirm")
	public String confirmedDeleteTweet(@PathVariable int tweetID,
			@RequestParam(name = "referer", defaultValue = "") String referer) {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);

		if (referer.isEmpty())
			referer = "/groups/view/" + tweetEntry.groupID;

		DBConnector.deleteTweet(tweetID, userID);
		return "redirect:" + referer;
	}

}
