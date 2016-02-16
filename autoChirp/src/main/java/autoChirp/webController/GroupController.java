package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.preProcessing.parser.WikipediaParser;
import autoChirp.tweetCreation.Tweet;
import autoChirp.tweetCreation.TweetFactory;
import autoChirp.tweetCreation.TweetGroup;
import autoChirp.tweeting.TweetScheduler;
import java.io.File;
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
import org.springframework.web.multipart.MultipartFile;
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

@RequestMapping(value = "/import/{importer}")
public ModelAndView importGroup(HttpSession session, @PathVariable String importer) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");

        if (!Arrays.asList("csv-file", "wikipedia").contains(importer))
                return new ModelAndView("redirect:/error");

        ModelAndView mv = new ModelAndView("import");
        mv.addObject("importer", importer);

        return mv;
}

// @RequestMapping(value = "/import", method = RequestMethod.POST)
// public String importGroupPost(HttpSession session, @RequestParam("importer") String importer, @RequestParam("source") String source, @RequestParam("title") String title, @RequestParam("description") String description) {
//         if (session.getAttribute("account") == null) return "redirect:/account";
//         int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
//
//         TweetGroup tweetGroup;
//
//         switch (importer) {
//         case "wiki":
//                 if (!source.matches("https?:\\/\\/(de|en|es|fr)\\.wikipedia\\.org\\/wiki\\/.*"))
//                         return "redirect:/error";
//
//                 TweetFactory tweeter = new TweetFactory();
//                 tweetGroup = tweeter.getTweetsFromUrl(source, new WikipediaParser(), description);
//                 tweetGroup.title = title;
//                 break;
//         default:
//                 return "redirect:/error";
//         }
//
//         int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);
//
//         return (groupID > 0)
//                ? "redirect:/groups/view/" + groupID
//                : "redirect:/error";
// }

@RequestMapping(value = "/import/csv-file", method = RequestMethod.POST)
public String importWikipediaPost(HttpSession session, @RequestParam("source") MultipartFile source, @RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("delay") int delay) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        File file;
        TweetFactory tweeter = new TweetFactory();

        try {
          file = File.createTempFile("upload-", ".csv");
          source.transferTo(file);
        } catch (Exception e) {
          return "redirect:/error";
        }

        TweetGroup tweetGroup = tweeter.getTweetsFromCSV(file, title, description, delay);

        int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

        return (groupID > 0)
               ? "redirect:/groups/view/" + groupID
               : "redirect:/error";
}

@RequestMapping(value = "/import/wikipedia", method = RequestMethod.POST)
public String importWikipediaPost(HttpSession session, @RequestParam("source") String source, @RequestParam("title") String title, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        if (!source.matches("https?:\\/\\/(de|en|es|fr)\\.wikipedia\\.org\\/wiki\\/.*"))
                return "redirect:/error";

        TweetFactory tweeter = new TweetFactory();
        TweetGroup tweetGroup = tweeter.getTweetsFromUrl(source, new WikipediaParser(), description);
        tweetGroup.title = title;

        int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

        return (groupID > 0)
               ? "redirect:/groups/view/" + groupID
               : "redirect:/error";
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

        boolean enabled = !DBConnector.getTweetGroupForUser(userID, groupID).enabled;
        DBConnector.updateGroupStatus(groupID, enabled);


        if(enabled){
        	 TweetGroup group = DBConnector.getTweetGroupForUser(userID, groupID);
             TweetScheduler.scheduleGroup(group.tweets, userID);
        }
        return "redirect:/groups/view/" + groupID;

        //Disable...
}

@RequestMapping(value = "/delete/{groupID}")
public String deleteGroup(HttpSession session, HttpServletRequest request, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return "redirect:/account";

        DBConnector.deleteGroup(groupID);

        return "redirect:" + request.getHeader("Referer");
}

}
