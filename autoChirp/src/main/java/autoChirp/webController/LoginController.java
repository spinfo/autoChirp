package autoChirp.webController;

import javax.inject.Inject;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController extends ConnectController {

@Inject
public LoginController(
        ConnectionFactoryLocator connectionFactoryLocator,
        ConnectionRepository connectionRepository) {
        super(connectionFactoryLocator, connectionRepository);
}

@Override
protected String connectView(String providerId) {
        return "redirect:/account";
}

@Override
protected String connectedView(String providerId){
        return "redirect:/account/login";
}

}
