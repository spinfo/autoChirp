package autoChirp.webController;

import autoChirp.DBConnector;
import java.util.Hashtable;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * A Spring MVC controller, responsible for serving /account. This controller
 * implements the logic to view the user account details, including details
 * about the associated Twitter account, and means to delete ones own account.
 *
 * @author Philip Schildkamp
 */
@Controller
@RequestMapping(value = "/account")
public class AccountController {

	private HttpSession session;

	/**
	 * Constructor method, used to autowire and inject the HttpSession object.
	 *
	 * @param session
	 *            Autowired HttpSession object
	 */
	@Inject
	public AccountController(HttpSession session) {
		this.session = session;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /account. This method
	 * reads details about the active user account from the database and hands
	 * them to the returned view. All details about the associated
	 * Twitter-account are stored within the session and can be accessed
	 * directly from the templating layer of the application.
	 *
	 * @return View containing the account overview
	 */
	@RequestMapping("")
	public ModelAndView account() {
		ModelAndView mv = new ModelAndView("account");

		if (session.getAttribute("account") != null) {
			int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
			int groups = DBConnector.getGroupIDsForUser(userID).size();
			int tweets = DBConnector.getTweetsForUser(userID).size();

			mv.addObject("groups", groups);
			mv.addObject("tweets", tweets);
		}

		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /account/logout. This
	 * method invalidates the session, therefore terminating all information
	 * about the user account and the associated Twitter-account and as such,
	 * logging out the user.
	 *
	 * @return Redirection to /home
	 */
	@RequestMapping("/logout")
	public String logout() {
		session.invalidate();

		return "redirect:/home";
	}

	/**
	 * A HTTP GET request handler, responsible for serving /account/delete. This
	 * method just presents a confirmation dialog to the user requesting
	 * account-deletion.
	 *
	 * @return View containing confirmation dialog for the intended action
	 */
	@RequestMapping(value = "/delete")
	public ModelAndView delete() {
		if (session.getAttribute("account") == null)
			return new ModelAndView("redirect:/account");
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		ModelAndView mv = new ModelAndView("confirm");
		mv.addObject("confirm", "Do You want to delete Your account and all associated data?");

		return mv;

	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /account/delete/confirm. This method does the same as the logout()
	 * method, but additionally deletes all information about the user and all
	 * associated objects (Twitter-account, scheduled Tweets, created groups,
	 * etc.) from the database, effectively destroying the user account.
	 *
	 * @return Redirection to /home
	 */
	@RequestMapping(value = "/delete/confirm")
	public String confirmedDelete() {
		if (session.getAttribute("account") == null)
			return "redirect:/account";
		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));

		session.invalidate();
		DBConnector.deleteUser(userID);

		return "redirect:/home";
	}

}
