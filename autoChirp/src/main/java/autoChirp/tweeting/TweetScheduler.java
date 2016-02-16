package autoChirp.tweeting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;

public class TweetScheduler {


	public static void scheduleGroup(List<Tweet> tweets, int user_id) {
		LocalDateTime now;
		Duration d;
		long delay;
		for (Tweet tweet : tweets) {
			LocalDateTime ldt = LocalDateTime.parse(tweet.tweetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			now = LocalDateTime.now();
			d = Duration.between(now, ldt);
			delay = d.getSeconds();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
			System.out.println("schedule: in seconds" + delay);
			scheduler.schedule(new TwitterTask(user_id, tweet.tweetID), delay, TimeUnit.SECONDS);
			DBConnector.flagAsScheduled(tweet.tweetID, user_id);
		}
	}

}
