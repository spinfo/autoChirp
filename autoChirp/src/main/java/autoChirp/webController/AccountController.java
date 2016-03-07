package autoChirp.webController;

import autoChirp.DBConnector;
import java.util.Hashtable;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.TwitterProfile;
import org.springframework.social.twitter.api.UserOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccountController {

private ConnectionRepository connectionRepository;
private Twitter twitter;

@Inject
public AccountController(ConnectionRepository connectionRepository, Twitter twitter) {
        this.connectionRepository = connectionRepository;
        this.twitter = twitter;
}

@RequestMapping("/account")
public ModelAndView account(HttpSession session) {
        ModelAndView mv = new ModelAndView("account");

        if (session.getAttribute("account") != null) {
                int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));
                int groups = DBConnector.getGroupIDsForUser(userID).size();
                int tweets = DBConnector.getTweetsForUser(userID).size();

                mv.addObject("groups", groups);
                mv.addObject("tweets", tweets);
        }

        return mv;
}

@RequestMapping("/account/login")
public String login(HttpSession session) {
        if (connectionRepository.findPrimaryConnection(Twitter.class) != null) {
                UserOperations userOperations = twitter.userOperations();
                TwitterProfile twitterProfile = userOperations.getUserProfile();
                Connection<Twitter> twitterConnection = connectionRepository.getPrimaryConnection(Twitter.class);

                long twitterID = userOperations.getProfileId();
                int userID = DBConnector.checkForUser(twitterID);

                if (userID == -1) {
                        ConnectionData twitterConnectionData = twitterConnection.createData();
                        String token = twitterConnectionData.getAccessToken();
                        String secret = twitterConnectionData.getSecret();
                        userID = DBConnector.insertNewUser(twitterID, token, secret);
                }

                Hashtable<String, String> account = new Hashtable<String, String>();
                account.put("userID", Integer.toString(userID));
                account.put("twitterID", Long.toString(twitterID));
                account.put("name", twitterProfile.getName());
                account.put("handle", twitterProfile.getScreenName());
                account.put("description", twitterProfile.getDescription());
                account.put("url", twitterProfile.getProfileUrl());
                account.put("image", twitterProfile.getProfileImageUrl());
                account.put("protected", String.valueOf(twitterProfile.isProtected()));
                session.setAttribute("account", account);
        }

        return "redirect:/account";
}

@RequestMapping("/account/logout")
public String logout(HttpSession session, Model model) {
        connectionRepository.removeConnections("twitter");
        session.invalidate();
        model.asMap().clear();

        return "redirect:/home";
}

@RequestMapping(value = "/account/delete")
public ModelAndView delete(HttpSession session, Model model) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        ModelAndView mv = new ModelAndView("confirm");
        mv.addObject("confirm", "Do You want to delete Your account and all associated data?");

        return mv;

}

@RequestMapping(value = "/account/delete/confirm")
public String confirmedDelete(HttpSession session, Model model) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        connectionRepository.removeConnections("twitter");
        session.invalidate();
        model.asMap().clear();
        DBConnector.deleteUser(userID);

        return "redirect:/home";
}

}
