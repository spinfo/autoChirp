package autoChirp.webController;

import autoChirp.tweeting.TwitterAccount;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.stereotype.Controller;

/**
 * A Spring MVC controller, extending Spring Socials ConnectController. This
 * controller-class overrides the default redirect paths of the Spring Social
 * module and adds an interceptor to handle Twitter authentication.
 *
 * @author Philip Schildkamp
 */
@Controller
public class LoginController extends ConnectController {

	private TwitterAccount twitterAccount;

	/**
	 * Constructor method, used to autowire and inject necessary objects.
	 *
	 * @param connectionFactoryLocator
	 *            Autowired ConnectionFactoryLocator object
	 * @param connectionRepository
	 *            Autowired ConnectionRepository object
	 * @param twitterAccount
	 *            Autowired TwitterAccount interceptor
	 */
	@Inject
	public LoginController(ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository,
			TwitterAccount twitterAccount) {
		super(connectionFactoryLocator, connectionRepository);
		this.twitterAccount = twitterAccount;
	}

	/**
	 * PostConstruct method, used to add an interceptor to handle Twitter auth.
	 */
	@PostConstruct
	public void twitterAccountInterceptor() {
		this.addInterceptor(twitterAccount);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.social.connect.web.ConnectController#connectView(java
	 * .lang.String)
	 */
	@Override
	protected String connectView(String providerId) {
		return "redirect:/account";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.social.connect.web.ConnectController#connectedView(
	 * java.lang.String)
	 */
	@Override
	protected String connectedView(String providerId) {
		return "redirect:/account";
	}

}
