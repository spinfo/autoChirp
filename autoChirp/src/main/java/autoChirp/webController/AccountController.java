package autoChirp.webController;

import autoChirp.DBConnector;
import java.util.Hashtable;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Philip Schildkamp
 *
 */
@Controller
public class AccountController {

/**
 * @param session
 * @return
 */
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

/**
 * @param session
 * @param model
 * @return
 */
@RequestMapping("/account/logout")
public String logout(HttpSession session, Model model) {
        // connectionRepository.removeConnections("twitter");
        session.invalidate();
        model.asMap().clear();

        return "redirect:/home";
}

/**
 * @param session
 * @param model
 * @return
 */
@RequestMapping(value = "/account/delete")
public ModelAndView delete(HttpSession session, Model model) {
        if (session.getAttribute("account") == null) return new ModelAndView("redirect:/account");
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        ModelAndView mv = new ModelAndView("confirm");
        mv.addObject("confirm", "Do You want to delete Your account and all associated data?");

        return mv;

}

/**
 * @param session
 * @param model
 * @return
 */
@RequestMapping(value = "/account/delete/confirm")
public String confirmedDelete(HttpSession session, Model model) {
        if (session.getAttribute("account") == null) return "redirect:/account";
        int userID = Integer.parseInt(((Hashtable<String,String>)session.getAttribute("account")).get("userID"));

        // connectionRepository.removeConnections("twitter");
        session.invalidate();
        model.asMap().clear();
        DBConnector.deleteUser(userID);

        return "redirect:/home";
}

}
