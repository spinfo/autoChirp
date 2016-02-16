package autoChirp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import autoChirp.tweeting.TweetScheduler;

@SpringBootApplication
public class Application {

@Value("${dbFilePath}")
private String dbFilePath;

@Value("${createDatabaseFile}")
private String createDatabaseFile;

public static void main(String[] args) throws IOException {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
}

@PostConstruct
private void connectDatabase(){
        File file = new File(dbFilePath);

        if (!file.exists()) {
                DBConnector.connect(dbFilePath);
                DBConnector.createOutputTables(createDatabaseFile);
        } else {
                DBConnector.connect(dbFilePath);
        }
       //Schedule tweets again
        
}

}
