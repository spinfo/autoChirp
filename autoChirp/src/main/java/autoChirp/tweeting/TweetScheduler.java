package autoChirp.tweeting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

/**
 * @author Alena Geduldig
 * 
 *         A class to schedule Tweets using the
 *         java.util.concurrent.ScheduledExecutorService;
 *
 */
public class TweetScheduler {

	/**
	 * Schedules a list of tweets for the given twitter-user by creating a new TwitterTask for each tweet.
	 * Also updates the tweets status in the database to scheduled = true
	 * 
	 * @param tweets
	 *            - a list of tweets to schedule
	 * @param user_id
	 */
	public static void scheduleTweetsForUser(List<Tweet> tweets, int user_id) {
		LocalDateTime now;
		Duration d;
		long delay;
		for (Tweet tweet : tweets) {
			//calculate delay
			LocalDateTime ldt = LocalDateTime.parse(tweet.tweetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			now = LocalDateTime.now();
			d = Duration.between(now, ldt);
			delay = d.getSeconds();
			if (delay < 0) {
				continue;
			}
			//schedule
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
			scheduler.schedule(new TwitterTask(user_id, tweet.tweetID), delay, TimeUnit.SECONDS);
			//update tweet-status
			DBConnector.flagAsScheduled(tweet.tweetID, user_id);
		}
	}

}
