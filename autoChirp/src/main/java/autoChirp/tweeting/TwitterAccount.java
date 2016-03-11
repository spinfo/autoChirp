package autoChirp.tweeting;

import autoChirp.DBConnector;
import java.util.Hashtable;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectInterceptor;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.TwitterProfile;
import org.springframework.social.twitter.api.UserOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.WebRequest;

@Component
public class TwitterAccount implements ConnectInterceptor<Twitter> {

private ConnectionRepository connectionRepository;
private HttpSession session;

@Inject
public TwitterAccount(ConnectionRepository connectionRepository, HttpSession session) {
        this.connectionRepository = connectionRepository;
        this.session = session;
}

public void preConnect(ConnectionFactory<Twitter> connectionFactory, MultiValueMap<String, String> parameters, WebRequest request) {
}

public void postConnect(Connection<Twitter> twitterConnection, WebRequest request) {
        Twitter twitter = twitterConnection.getApi();
        UserOperations userOperations = twitter.userOperations();
        TwitterProfile twitterProfile = userOperations.getUserProfile();

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

        connectionRepository.removeConnection(twitterConnection.getKey());
}

}
