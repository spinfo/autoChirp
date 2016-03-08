package autoChirp.webController;

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

/**
 * @param connectionFactoryLocator
 * @param connectionRepository
 */
@Inject
public LoginController(
        ConnectionFactoryLocator connectionFactoryLocator,
        ConnectionRepository connectionRepository) {
        super(connectionFactoryLocator, connectionRepository);
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
        return "redirect:/account/login";
}

}
