package autoChirp.webController;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Philip Schildkamp
 *
 */
@Controller
@SessionAttributes("twitter")
public class HomeController {


/**
 * @return
 */
@RequestMapping(value = "/")
public String index() {
        return "redirect:/home";
}

/**
 * @return
 */
@RequestMapping(value = "/home")
public ModelAndView home() {
        ModelAndView mv = new ModelAndView("home");
        return mv;
}

/**
 * @return
 */
@RequestMapping(value = "/about")
public ModelAndView about() {
        ModelAndView mv = new ModelAndView("about");
        return mv;
}

/**
 * @return
 */
@RequestMapping(value = "/contact")
public ModelAndView contact() {
        ModelAndView mv = new ModelAndView("contact");
        return mv;
}

/**
 * @return
 */
@RequestMapping(value = "/help")
public ModelAndView help() {
        ModelAndView mv = new ModelAndView("help");
        return mv;
}

}
