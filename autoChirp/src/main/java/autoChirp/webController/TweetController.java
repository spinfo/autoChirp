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
        int groupID;
        int tweetID;

        try {
                groupID = Integer.parseInt(tweetGroup);
                if (!DBConnector.getGroupIDsForUser(userID).contains(groupID))
                        throw new Exception();

                tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);
        } catch (Exception e) {
                Date now = new Date();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String date = format.format(now);

                TweetGroup newGroup = new TweetGroup(tweetGroup, "Shorthand-added: " + date);
                groupID = DBConnector.insertTweetGroup(newGroup, userID);
                tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);
        }

        return (groupID <= 0 || tweetID <= 0)
               ? "redirect:/error"
               : "redirect:/tweets/view/" + tweetID;
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
        int tweetID = DBConnector.addTweetToGroup(userID, tweetEntry, groupID);

        return (tweetID > 0)
               ? "redirect:/tweets/view/" + groupID
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
public String editTweetPost(HttpSession session, @PathVariable int tweetID, @RequestParam("content") String content) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        DBConnector.editTweet(tweetID, content);

        return "redirect:/tweets/view/" + tweetID;
}

@RequestMapping(value = "/delete/{tweetID}")
public String deleteTweet(HttpSession session, HttpServletRequest request, @PathVariable int tweetID) {
        if (session.getAttribute("account") == null) return "redirect:/account";

        DBConnector.deleteTweet(tweetID);
        String ref = request.getHeader("Referer");

        return (ref.endsWith("/tweets/view/" + tweetID))
               ? "redirect:/tweets/view"
               : "redirect:" + request.getHeader("Referer");
}

}
