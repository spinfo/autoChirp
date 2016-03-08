package autoChirp.webController;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;

/**
 * @author Philip Schildkamp
 *
 */
@Controller
@RequestMapping(value = "/tweets")
public class TweetController {

private int tweetsPerPage = 15;
private int maxTweetLength = 140;

/**
 * @param session
 * @param page
 * @return
 */
@RequestMapping(value = "/view")
public ModelAndView viewTweets(HttpSession session, @RequestParam(name = "page", defaultValue = "1") int page) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

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
 * @param session
 * @param tweetID
 * @return
 */
@RequestMapping(value = "/view/{tweetID}")
public ModelAndView viewTweet(HttpSession session, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetEntry", tweetEntry);
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

/**
 * @param session
 * @return
 */
@RequestMapping(value = "/add")
public ModelAndView addTweet(HttpSession session) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        List<Integer> groupIDs = DBConnector.getGroupIDsForUser(userID);
        List<TweetGroup> tweetGroups = new ArrayList<TweetGroup>();

        for (int groupID : groupIDs)
                tweetGroups.add(DBConnector.getTweetGroupForUser(userID, groupID));

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetGroups", tweetGroups);

        return mv;
}

/**
 * @param session
 * @param tweetGroup
 * @param content
 * @param tweetDate
 * @param tweetTime
 * @return
 */
@RequestMapping(value = "/add", method = RequestMethod.POST)
public ModelAndView addTweetPost(HttpSession session, @RequestParam("tweetGroup") String tweetGroup, @RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate, @RequestParam("tweetTime") String tweetTime) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

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

        Tweet tweetEntry = new Tweet(tweetDate + " " + tweetTime, content);
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
                mv.addObject("confirm", "Do You want to keep Your group \"" + DBConnector.getGroupTitle(groupID, userID) + "\" enabled?");
                mv.addObject("next", "/groups/toggle/" + groupID);
                mv.addObject("prev", "/tweets/view/" + tweetID);
                return mv;
        } else return new ModelAndView("redirect:/tweets/view/" + tweetID);
}


/**
 * @param session
 * @param groupID
 * @return
 */
@RequestMapping(value = "/add/{groupID}")
public ModelAndView addTweetToGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

/**
 * @param session
 * @param groupID
 * @param content
 * @param tweetDate
 * @param tweetTime
 * @return
 */
@RequestMapping(value = "/add/{groupID}", method = RequestMethod.POST)
public ModelAndView addTweetToGroupPost(HttpSession session, @PathVariable int groupID, @RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate, @RequestParam("tweetTime") String tweetTime) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
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

        Tweet tweetEntry = new Tweet(tweetDate + " " + tweetTime, content);
        int tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);

        if (tweetID < 0)
                return new ModelAndView("redirect:/error");

        if (enabledGroup) {
                ModelAndView mv = new ModelAndView("confirm");
                mv.addObject("confirm", "Do You want to keep Your group \"" + DBConnector.getGroupTitle(groupID, userID) + "\" enabled?");
                mv.addObject("next", "/groups/toggle/" + groupID);
                mv.addObject("prev", "/tweets/view/" + tweetID);
                return mv;
        } else return new ModelAndView("redirect:/tweets/view/" + tweetID);
}

/**
 * @param session
 * @param tweetID
 * @return
 */
@RequestMapping(value = "/edit/{tweetID}")
public ModelAndView editTweet(HttpSession session, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetEntry", tweetEntry);
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

/**
 * @param session
 * @param tweetID
 * @param content
 * @return
 */
@RequestMapping(value = "/edit/{tweetID}", method = RequestMethod.POST)
public ModelAndView editTweetPost(HttpSession session, @PathVariable int tweetID, @RequestParam("content") String content) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        if (content.length() > maxTweetLength) {
                ModelAndView mv = new ModelAndView("error");
                mv.addObject("error", "The tweet content may be no longer then " + maxTweetLength + " characters.");
                return mv;
        }

        DBConnector.editTweet(tweetID, content, userID);

        return new ModelAndView("redirect:/tweets/view/" + tweetID);
}

/**
 * @param session
 * @param request
 * @param tweetID
 * @return
 * @throws URISyntaxException
 */
@RequestMapping(value = "/delete/{tweetID}")
public ModelAndView deleteTweet(HttpSession session, HttpServletRequest request, @PathVariable int tweetID) throws URISyntaxException {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
        Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
        String referer = new URI(request.getHeader("referer")).getPath();

        ModelAndView mv = new ModelAndView("confirm");
        mv.addObject("confirm", "Do You want to delete Your tweet \"" + tweetEntry.content + "\" from Your group \"" + tweetEntry.groupName + "\"?");
        if (!referer.matches("^/tweets/.+?[0-9]$")) mv.addObject("referer", referer);

        return mv;
}

/**
 * @param session
 * @param request
 * @param tweetID
 * @param referer
 * @return
 */
@RequestMapping(value = "/delete/{tweetID}/confirm")
public String confirmedDeleteTweet(HttpSession session, HttpServletRequest request, @PathVariable int tweetID, @RequestParam(name = "referer", defaultValue = "/tweets/view") String referer) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
        DBConnector.deleteTweet(tweetID, userID);
        return "redirect:" + referer;
}

}
