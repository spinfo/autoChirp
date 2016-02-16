package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetGroup;
import java.util.ArrayList;
import java.util.Arrays;
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
@RequestMapping(value = "/groups")
public class GroupController {

@RequestMapping(value = "/view")
public ModelAndView viewGroups(HttpSession session) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        List<Integer> tweetGroupIDs = DBConnector.getGroupIDsForUser(userID);
        List<TweetGroup> tweetGroups = new ArrayList<TweetGroup>();

        for (int groupID : tweetGroupIDs)
                tweetGroups.add(DBConnector.getTweetGroupForUser(userID, groupID));

        ModelAndView mv = new ModelAndView("groups");
        mv.addObject("tweetGroups", tweetGroups);

        return mv;
}

@RequestMapping(value = "/view/{groupID}")
public ModelAndView viewGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
        List<Tweet> tweetsList = tweetGroup.tweets;

        ModelAndView mv = new ModelAndView("group");
        mv.addObject("tweetGroup", tweetGroup);
        mv.addObject("tweetsList", tweetGroup.tweets);

        return mv;
}

@RequestMapping(value = "/add")
public ModelAndView addGroup(HttpSession session) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        ModelAndView mv = new ModelAndView("group");
        return mv;
}

@RequestMapping(value = "/add", method = RequestMethod.POST)
public String addGroupPost(HttpSession session, @RequestParam("title") String title, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = new TweetGroup(title, description);
        int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

        return (groupID > 0)
               ? "redirect:/groups/view/" + groupID
               : "redirect:/error";
}

@RequestMapping(value = "/import")
public ModelAndView importGroup(HttpSession session) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        ModelAndView mv = new ModelAndView("import");
        return mv;
}

@RequestMapping(value = "/edit/{groupID}")
public ModelAndView editGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);

        ModelAndView mv = new ModelAndView("group");
        mv.addObject("tweetGroup", tweetGroup);

        return mv;
}

@RequestMapping(value = "/edit/{groupID}", method = RequestMethod.POST)
public String addGroupPost(HttpSession session, @PathVariable int groupID, @RequestParam("title") String title, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup oldGroup = DBConnector.getTweetGroupForUser(userID, groupID);
        TweetGroup newGroup = new TweetGroup(title, description);

        newGroup.tweets = oldGroup.tweets;
        int newGroupID = DBConnector.insertTweetGroup(newGroup, userID);

        if (newGroupID > 0) {
          DBConnector.deleteGroup(groupID);
          return "redirect:/groups/view/" + newGroupID;
        } else return "redirect:/error";
}

@RequestMapping(value = "/toggle/{groupID}")
public String toggleGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        boolean enabled = DBConnector.getTweetGroupForUser(userID, groupID).enabled;
        DBConnector.updateGroupStatus(groupID, !enabled);

        return "redirect:/groups/view/" + groupID;
}

@RequestMapping(value = "/delete/{groupID}")
public String deleteGroup(HttpSession session, HttpServletRequest request, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return "redirect:/account";

        DBConnector.deleteGroup(groupID);

        return "redirect:" + request.getHeader("Referer");
}

}
