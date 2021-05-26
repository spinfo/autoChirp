package autoChirp.webController;

import autoChirp.DBConnector;
import autoChirp.recentTweetWidget.TweetWidgetCreator;
import autoChirp.tweetCreation.Tweet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * A Spring MVC controller, responsible for serving /, /home, /about, /trivia
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
		mv.addObject("maxTweetLength", Tweet.MAX_TWEET_LENGTH);
		mv.addObject("recentTweets", TweetWidgetCreator.getRecentTweetWidget());
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /trivia.
	 *
	 * @return View containing the trivia page
	 */
	@RequestMapping(value = "/trivia")
	public ModelAndView trivia() {
		ModelAndView mv = new ModelAndView("trivia");
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
	 * A HTTP GET request handler, responsible for serving /help.
	 *
	 * @return View containing the help page
	 */
	@RequestMapping(value = "/help")
	public ModelAndView help() {
		ModelAndView mv = new ModelAndView("help");
		mv.addObject("maxTweetLength", Tweet.MAX_TWEET_LENGTH);
		return mv;
	}

	/**
	 * A HTTP GET request handler, responsible for serving /stats.
	 *
	 * @return View containing the statistics page
	 */
	@RequestMapping(value = "/stats")
	public ModelAndView stats() {
		ModelAndView mv = new ModelAndView("stats");

    mv.addObject("upcomingTweets", DBConnector.getUpcomingTweets());
    mv.addObject("latestTweets", DBConnector.getLatestTweets());
    mv.addObject("registeredUsers", DBConnector.getRegisteredUsers());
    mv.addObject("allTweets", DBConnector.getAllTweets());
    mv.addObject("publishedTweets", DBConnector.getPublishedTweets());
    mv.addObject("scheduledTweets", DBConnector.getScheduledTweets());

		return mv;
	}

}
