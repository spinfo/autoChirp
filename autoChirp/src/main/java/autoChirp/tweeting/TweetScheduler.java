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

//	public static void scheduleInitialTweets(Map<Integer, List<Tweet>> initialTweets) {
//
//		for (Integer user : initialTweets.keySet()) {
//			System.out.println("user: " + user);
//			List<Tweet> tweetsForUser = initialTweets.get(user);
//			for (Tweet tweet : tweetsForUser) {
//				LocalDateTime ldt = LocalDateTime.parse(tweet.getTweetDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//				LocalDateTime now;
//				Duration d;
//				long delay;
//				now = LocalDateTime.now();
//				d = Duration.between(now, ldt);
//				delay = d.getSeconds();
//				System.out.println();
//				ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//				scheduler.schedule(new TwitterTask(user, tweet), delay, TimeUnit.SECONDS);
//			}
//		}
//	}

	public static void scheduleGroup(List<Tweet> tweets, int user_id) {
		LocalDateTime now;
		Duration d;
		long delay;
		for (Tweet tweet : tweets) {
			LocalDateTime ldt = LocalDateTime.parse(tweet.tweetDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			now = LocalDateTime.now();
			d = Duration.between(now, ldt);
			delay = d.getSeconds();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
			scheduler.schedule(new TwitterTask(user_id, tweet.tweetID), delay, TimeUnit.SECONDS);
			DBConnector.flagAsScheduled(tweet.tweetID, user_id);
		}
	}

}
