package autoChirp.webController;

import javax.annotation.PostConstruct;

import autoChirp.tweeting.TwitterAccount;
import javax.inject.Inject;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.stereotype.Controller;

/**
 * @author Philip Schildkamp
 *
 */
@Controller
public class LoginController extends ConnectController {

private TwitterAccount twitterAccount;


/**
 * @param connectionFactoryLocator
 * @param connectionRepository
 * @param twitterAccount
 */
@Inject
public LoginController(
        ConnectionFactoryLocator connectionFactoryLocator,
        ConnectionRepository connectionRepository,
        TwitterAccount twitterAccount) {
        super(connectionFactoryLocator, connectionRepository);
        this.twitterAccount = twitterAccount;
}


/**
 * 
 */
@PostConstruct
public void twitterAccountInterceptor() {
        this.addInterceptor(twitterAccount);
}

/* (non-Javadoc)
 * @see org.springframework.social.connect.web.ConnectController#connectView(java.lang.String)
 */
@Override
protected String connectView(String providerId) {
        return "redirect:/account";
}

/* (non-Javadoc)
 * @see org.springframework.social.connect.web.ConnectController#connectedView(java.lang.String)
 */
@Override
protected String connectedView(String providerId){
        return "redirect:/account";
}

}
