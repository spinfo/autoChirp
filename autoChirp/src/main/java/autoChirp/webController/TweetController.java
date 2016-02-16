package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/tweets")
public class TweetController {

private int tweetsPerPage = 15;

@RequestMapping(value = "/view")
public ModelAndView viewTweets(HttpSession session, @RequestParam(value = "page", defaultValue = "1") String pageParam) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        // int page = Integer.parseInt(pageParam);
        // int offset = (page - 1) * 15;
        //
        // List<Tweet> tweetsList = DBConnector.getTweetsForUser(userID, true, false, offset, tweetsPerPage);

        List<Tweet> tweetsList = DBConnector.getTweetsForUser(userID);

        ModelAndView mv = new ModelAndView("tweets");
        mv.addObject("tweetsList", tweetsList);
        return mv;
}

@RequestMapping(value = "/view/{tweetID}")
public ModelAndView viewTweet(HttpSession session, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = DBConnector.getTweetByID(tweetID);
        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetEntry", tweetEntry);
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

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

@RequestMapping(value = "/add", method = RequestMethod.POST)
public String addGroupPost(HttpSession session, @RequestParam("tweetGroup") String tweetGroup, @RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = new Tweet(tweetDate, content);
        TweetGroup newGroup;

        try {
                int groupID = Integer.parseInt(tweetGroup);
                TweetGroup oldGroup = DBConnector.getTweetGroupForUser(userID, groupID);

                if (oldGroup == null)
                        throw new Exception();

                newGroup = new TweetGroup(oldGroup.title, oldGroup.description);
                newGroup.setTweets(oldGroup.tweets);
                DBConnector.deleteGroup(groupID);
        } catch (Exception e) {
                Date now = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = format.format(now);

                newGroup = new TweetGroup(tweetGroup, "Shorthand-added: " + date);
        }

        newGroup.addTweet(tweetEntry);

        int newGroupID = DBConnector.insertTweetGroup(newGroup, userID);

        if (newGroupID <= 0)
                return "redirect:/error";

        TweetGroup updatedGroup = DBConnector.getTweetGroupForUser(userID, newGroupID);
        Tweet updatedTweet = updatedGroup.tweets.get(updatedGroup.tweets.size() - 1);

        return "redirect:/tweets/view/" + updatedTweet.tweetID;
}


@RequestMapping(value = "/add/{groupID}")
public ModelAndView addTweetToGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

@RequestMapping(value = "/add/{groupID}", method = RequestMethod.POST)
public String addGroupPost(HttpSession session, @PathVariable int groupID, @RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = new Tweet(tweetDate, content);
        TweetGroup oldGroup = DBConnector.getTweetGroupForUser(userID, groupID);
        TweetGroup newGroup = new TweetGroup(oldGroup.title, oldGroup.description);

        newGroup.setTweets(oldGroup.tweets);
        newGroup.addTweet(tweetEntry);

        DBConnector.deleteGroup(groupID);
        int newGroupID = DBConnector.insertTweetGroup(newGroup, userID);

        return (newGroupID > 0)
               ? "redirect:/groups/view/" + newGroupID
               : "redirect:/error";
}

@RequestMapping(value = "/edit/{tweetID}")
public ModelAndView editTweet(HttpSession session, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet tweetEntry = DBConnector.getTweetByID(tweetID);
        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, tweetEntry.groupID);

        ModelAndView mv = new ModelAndView("tweet");
        mv.addObject("tweetEntry", tweetEntry);
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

@RequestMapping(value = "/edit/{tweetID}", method = RequestMethod.POST)
public String editTweetPost(HttpSession session, @PathVariable int tweetID, @RequestParam("content") String content, @RequestParam("tweetDate") String tweetDate) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        Tweet oldTweet = DBConnector.getTweetByID(tweetID);
        Tweet newTweet = new Tweet(tweetDate, content);
        TweetGroup oldGroup = DBConnector.getTweetGroupForUser(userID, oldTweet.groupID);
        TweetGroup newGroup = new TweetGroup(oldGroup.title, oldGroup.description);

        for (Tweet tweetEntry : oldGroup.tweets)
                if (tweetEntry.tweetID != tweetID)
                        newGroup.addTweet(tweetEntry);

        newGroup.addTweet(newTweet);

        DBConnector.deleteGroup(oldTweet.groupID);
        int newGroupID = DBConnector.insertTweetGroup(newGroup, userID);

        if (newGroupID <= 0)
                return "redirect:/error";

        TweetGroup updatedGroup = DBConnector.getTweetGroupForUser(userID, newGroupID);
        Tweet updatedTweet = updatedGroup.tweets.get(updatedGroup.tweets.size() - 1);

        return "redirect:/tweets/view/" + updatedTweet.tweetID;
}

@RequestMapping(value = "/delete/{tweetID}")
public String deleteTweet(HttpSession session, HttpServletRequest request, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return "redirect:/account";

        DBConnector.deleteTweet(tweetID);

        return "redirect:" + request.getHeader("Referer");
}

}
