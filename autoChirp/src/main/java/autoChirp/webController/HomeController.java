package autoChirp.webController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * A Spring MVC controller, responsible for serving /, /home, /about, /contact
 * and /help. This controller serves static templates containing basic
 * information about the application.
 *
 * @author Philip Schildkamp
 */
@Controller
public class HomeController {

	/**
	 * A HTTP GET request handler, responsible for redirecting / to /home.
	 *
	 * @return Redirection to /home
	 */
	@RequestMapping(value = "/")
	public String index() {
		return "redirect:/home";
	}

	/**
	 * A HTTP GET request handler, responsible for serving /home.
	 *
	 * @return View containing the home page
	 */
	@RequestMapping(value = "/home")
	public ModelAndView home() {
		ModelAndView mv = new ModelAndView("home");
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /about.
	 *
	 * @return View containing the about page
	 */
	@RequestMapping(value = "/about")
	public ModelAndView about() {
		ModelAndView mv = new ModelAndView("about");
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /contact.
	 *
	 * @return View containing the contact page
	 */
	@RequestMapping(value = "/contact")
	public ModelAndView contact() {
		ModelAndView mv = new ModelAndView("contact");
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /help.
	 *
	 * @return View containing the help page
	 */
	@RequestMapping(value = "/help")
	public ModelAndView help() {
		ModelAndView mv = new ModelAndView("help");
		return mv;
	}

}
