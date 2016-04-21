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

/**
 * A Spring Social ConnectInterceptor responsible for Twitter authentication.
 * This class implements a interceptor for Twitter-connect workflows, provided
 * by the Spring Social Twitter module. While the preConnect method has an empty
 * body, the postConnect method makes the magic happen: As soon as a successful
 * connection is established to Twitter, the associated user is searched for in
 * the database - if none is found, a new user (and the Twitter API secrets) is
 * created. Then all detail relevant to the application is stored in the user
 * session. At last the connection to Twitter is closed, as it isn't needed.
 *
 * @author Philip Schildkamp
 */
@Component
public class TwitterAccount implements ConnectInterceptor<Twitter> {

	private ConnectionRepository connectionRepository;
	private HttpSession session;

	/**
	 * Constructor method, used to autowire and inject necessary objects.
	 *
	 * @param connectionRepository
	 *            Autowired ConnectionRepository object
	 * @param session
	 *            Autowired HttpSession object
	 */
	@Inject
	public TwitterAccount(ConnectionRepository connectionRepository, HttpSession session) {
		this.connectionRepository = connectionRepository;
		this.session = session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.social.connect.web.ConnectInterceptor#preConnect(org.
	 * springframework.social.connect.ConnectionFactory,
	 * org.springframework.util.MultiValueMap,
	 * org.springframework.web.context.request.WebRequest)
	 */
	public void preConnect(ConnectionFactory<Twitter> connectionFactory, MultiValueMap<String, String> parameters,
			WebRequest request) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.social.connect.web.ConnectInterceptor#postConnect(org
	 * .springframework.social.connect.Connection,
	 * org.springframework.web.context.request.WebRequest)
	 */
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
		account.put("geoEnabled",String.valueOf(twitterProfile.isGeoEnabled()));
		account.put("handle", twitterProfile.getScreenName());
		account.put("description", twitterProfile.getDescription());
		account.put("url", twitterProfile.getProfileUrl());
		account.put("image", twitterProfile.getProfileImageUrl());
		account.put("protected", String.valueOf(twitterProfile.isProtected()));
		session.setAttribute("account", account);

		connectionRepository.removeConnection(twitterConnection.getKey());
	}

}
