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

private int groupsPerPage = 15;
private int tweetsPerPage = 15;

@RequestMapping(value = "/view")
public ModelAndView viewGroups(HttpSession session, @RequestParam(name = "page", defaultValue = "1") int page) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        List<Integer> tweetGroupIDs = DBConnector.getGroupIDsForUser(userID);
        List<TweetGroup> tweetGroups = new ArrayList<TweetGroup>();
        ModelAndView mv = new ModelAndView("groups");

        for (int groupID : tweetGroupIDs)
                tweetGroups.add(DBConnector.getTweetGroupForUser(userID, groupID));

        if (tweetGroups.size() < groupsPerPage) {
                mv.addObject("tweetGroups", tweetGroups);
                return mv;
        }

        List<TweetGroup> pageGroupList;
        int pages = tweetGroups.size() / groupsPerPage;
        int offset = (page - 1) * groupsPerPage;

        pageGroupList = (offset + groupsPerPage < tweetGroups.size())
                        ? tweetGroups.subList(offset, offset + groupsPerPage)
                        : tweetGroups.subList(offset, tweetGroups.size() - 1);

        mv.addObject("tweetGroups", pageGroupList);
        mv.addObject("page", page);
        mv.addObject("pages", pages);
        return mv;
}

@RequestMapping(value = "/view/{groupID}")
public ModelAndView viewGroup(HttpSession session, @PathVariable int groupID, @RequestParam(name = "page", defaultValue = "1") int page) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
        List<Tweet> tweetsList = tweetGroup.tweets;
        ModelAndView mv = new ModelAndView("group");
        mv.addObject("tweetGroup", tweetGroup);

        if (tweetsList.size() < tweetsPerPage) {
                mv.addObject("tweetsList", tweetsList);
                return mv;
        }

        List<Tweet> pageTweetsList;
        int pages = tweetsList.size() / tweetsPerPage;
        int offset = (page - 1) * tweetsPerPage;

        pageTweetsList = (offset + tweetsPerPage < tweetsList.size())
                         ? tweetsList.subList(offset, offset + tweetsPerPage)
                         : tweetsList.subList(offset, tweetsList.size() - 1);

        mv.addObject("tweetsList", pageTweetsList);
        mv.addObject("page", page);
        mv.addObject("pages", pages);

        return mv;
}

@RequestMapping(value = "/add")
public ModelAndView addGroup(HttpSession session) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");

        ModelAndView mv = new ModelAndView("group");
        return mv;
}

@RequestMapping(value = "/add", method = RequestMethod.POST)
public ModelAndView addGroupPost(HttpSession session, @RequestParam("title") String title, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

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

        return (groupID > 0)
               ? new ModelAndView("redirect:/groups/view/" + groupID)
               : new ModelAndView("redirect:/error");
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

@RequestMapping(value = "/import/csv-file", method = RequestMethod.POST)
public ModelAndView importCSVGroupPost(HttpSession session, @RequestParam("source") MultipartFile source, @RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("delay") int delay) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

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
        TweetFactory tweeter = new TweetFactory();

        try {
                file = File.createTempFile("upload-", ".csv");
                source.transferTo(file);
        } catch (Exception e) {
                return new ModelAndView("redirect:/error");
        }

        TweetGroup tweetGroup = tweeter.getTweetsFromCSV(file, title, description, (delay <= 0) ? 0 : delay);

        int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

        return (groupID > 0)
               ? new ModelAndView("redirect:/groups/view/" + groupID)
               : new ModelAndView("redirect:/error");
}

@RequestMapping(value = "/import/wikipedia", method = RequestMethod.POST)
public ModelAndView importWikipediaGroupPost(HttpSession session, @RequestParam("source") String source, @RequestParam("title") String title, @RequestParam("prefix") String prefix, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        if (!source.matches("https?:\\/\\/(de|en|es|fr)\\.wikipedia\\.org\\/wiki\\/.*")) {
                ModelAndView mv = new ModelAndView("error");
                mv.addObject("error", "The URL mmust be a valid (english, german, spanish or french) Wikipedia Article.");
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

        if (prefix == "")
                prefix = null;

        TweetFactory tweeter = new TweetFactory();
        TweetGroup tweetGroup = tweeter.getTweetsFromUrl(source, new WikipediaParser(), description, prefix);
        tweetGroup.title = title;

        int groupID = DBConnector.insertTweetGroup(tweetGroup, userID);

        return (groupID > 0)
               ? new ModelAndView("redirect:/groups/view/" + groupID)
               : new ModelAndView("redirect:/error");
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
public ModelAndView editGroupPost(HttpSession session, @PathVariable int groupID, @RequestParam("title") String title, @RequestParam("description") String description) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

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

@RequestMapping(value = "/toggle/{groupID}")
public String toggleGroup(HttpSession session, @PathVariable int groupID) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        TweetGroup tweetGroup = DBConnector.getTweetGroupForUser(userID, groupID);
        boolean enabled = !tweetGroup.enabled;
        DBConnector.updateGroupStatus(groupID, enabled, userID);

        if (enabled)
                TweetScheduler.scheduleGroup(tweetGroup.tweets, userID);

        return "redirect:/groups/view/" + groupID;
}

@RequestMapping(value = "/delete/{groupID}")
public ModelAndView deleteGroup(HttpSession session, HttpServletRequest request, @PathVariable int groupID) throws URISyntaxException {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
        String referer = new URI(request.getHeader("referer")).getPath();

        ModelAndView mv = new ModelAndView("confirm");
        mv.addObject("confirm", "Do You want to delete Your group \"" + DBConnector.getGroupTitle(groupID, userID) + "\" and all containing tweets?");
        if (!referer.matches("^/groups/.+?[0-9]$")) mv.addObject("referer", referer);

        return mv;
}

@RequestMapping(value = "/delete/{groupID}/confirm")
public String confirmedDeleteGroup(HttpSession session, HttpServletRequest request, @PathVariable int groupID, @RequestParam(name = "referer", defaultValue = "/groups/view") String referer) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
        DBConnector.deleteGroup(groupID, userID);
        return "redirect:" + referer;
}


}
