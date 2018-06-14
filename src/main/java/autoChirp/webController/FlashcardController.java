package autoChirp.webController;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import autoChirp.DBConnector;
import autoChirp.tweetCreation.Tweet;
import javassist.NotFoundException;

@Controller
public class FlashcardController {

	private HttpSession session;
	
	@Value("${autochirp.flashcards.directory}")
	private String flashcardDir;

	/**
	 * Constructor method, used to autowire and inject the HttpSession object.
	 *
	 * @param session
	 *            Autowired HttpSession object
	 */
	@Inject
	public FlashcardController(HttpSession session) {
		this.session = session;
	}

	@RequestMapping(value = "/cardpreview/{tweetID}/{design}")
	public void cardpreview(HttpServletResponse response, @PathVariable int tweetID, @PathVariable String design)
			throws Exception {
		if (session.getAttribute("account") == null) {
			response.sendRedirect("/account");
			response.getOutputStream().close();
			return;
		}

		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
		byte[] img = renderImage(tweetEntry, design);

		if (img == null) {
			response.sendError(404);
			response.getOutputStream().close();
			return;
		}

		response.setContentType("image/png");
		response.getOutputStream().write(img);
		response.getOutputStream().close();
	}

	/**
	 * RequestMapping to allow previewing flashcards when authenticated.
	 *
	 * @param tweetID
	 *            ID of the Tweet intended to be displayed as flashcard.
	 */
	@RequestMapping(value = "/cardpreview/{tweetID}")
	public void cardpreview(HttpServletResponse response, @PathVariable int tweetID) throws Exception {
		if (session.getAttribute("account") == null) {
			response.sendRedirect("/account");
			response.getOutputStream().close();
			return;
		}

		int userID = Integer.parseInt(((Hashtable<String, String>) session.getAttribute("account")).get("userID"));
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID, userID);
		String flashcard = DBConnector.getFlashcard(tweetEntry.groupID);
		byte[] img = renderImage(tweetEntry, flashcard);

		if (img == null) {
			response.sendError(404);
			response.getOutputStream().close();
			return;
		}

		response.setContentType("image/png");
		response.getOutputStream().write(img);
		response.getOutputStream().close();
	}

	/**
	 * RequestMapping to allow Twitter fetching flashcards (unauthenticated,
	 * only if Tweet is published).
	 *
	 * @param tweetID
	 *            ID of the Tweet intended to be displayed as flashcard.
	 */
	@RequestMapping(value = "/flashcard/{tweetID}")
	public void flashcard(HttpServletResponse response, @PathVariable int tweetID) throws Exception {
		Tweet tweetEntry = DBConnector.getTweetByID(tweetID);
		String flashcard = DBConnector.getFlashcard(tweetEntry.groupID);

		if (!tweetEntry.tweeted) {
			response.sendError(404);
			response.getOutputStream().close();
			return;
		}

		byte[] img = renderImage(tweetEntry, flashcard);

		if (img == null) {
			response.sendError(404);
			response.getOutputStream().close();
			return;
		}

		response.setContentType("image/png");
		response.getOutputStream().write(img);
		response.getOutputStream().close();
	}

	/**
	 * This method takes a Tweet object and returns a rendered flashcard-image
	 * as byte array.
	 *
	 * @param tweetEntry
	 *            Tweet object intended to be displayed as flashcard.
	 * @return flashcard-image as byte array
	 */
	private byte[] renderImage(Tweet tweetEntry, String flashcard) throws Exception {
		if (tweetEntry == null) {
			throw new NotFoundException("Could not create flashcard, sorry!");
		}

		if (tweetEntry.adjustedLength() <= Tweet.MAX_TWEET_LENGTH) {
			return null;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphic = image.createGraphics();
		graphic.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
		String groupName = DBConnector.getGroupTitle(tweetEntry.groupID);
		boolean onlytext = (tweetEntry.imageUrl == null || tweetEntry.imageUrl.isEmpty());

		// colors
		Color textColor = new Color(51, 51, 51);
		Color backgroundColor = new Color(255,255,255, 70);

		// background image
		File file = new File(flashcardDir+File.separator+flashcard);
		if (!file.exists()) {
			flashcard = "default.jpg";
		}
		BufferedImage bgimg = ImageIO
				.read(new File(flashcardDir+File.separator + flashcard));
		graphic.drawImage(bgimg, 0, 0, null);

		// header box
		 graphic.setColor(backgroundColor);
		 //corpus box
		 graphic.fillRect(30, 30, 840, 60);
		 
		 graphic.fillRect(30, 120, 840, 450);

		// formated texts
		Float x, y, bound;
		AttributedString attstr;
		LineBreakMeasurer measure;
		AttributedCharacterIterator iter;
		FontRenderContext frc = graphic.getFontRenderContext();

		// header
		String header = groupName+"";
		String date =  tweetEntry.formatDate().substring(0, 5);

		graphic.setColor(textColor);
		attstr = new AttributedString(header);
		attstr.addAttribute(TextAttribute.FONT, new Font(Font.SANS_SERIF, Font.BOLD, 22));
		iter = attstr.getIterator();
		measure = new LineBreakMeasurer(iter, frc);
		measure.setPosition(iter.getBeginIndex());
		x = 35f;
		y = 68f;
		bound = 830f;
		measure.getPosition();
		measure.nextLayout(bound).draw(graphic, x, y);

		y = 120f;
		
		// corpus
		List<String> corpus = new ArrayList<String>();
		corpus.add(date);
		corpus.add("");
		corpus.addAll(Arrays.asList(tweetEntry.content.split("\n")));
		
		for (int i = 0; i < corpus.size(); i++ ) {
			String string = corpus.get(i);
			if (string.isEmpty())
				string = " ";

			graphic.setColor(textColor);
			attstr = new AttributedString(string.replaceAll("<[^>]*>", ""));
			if(i == 0){
				attstr.addAttribute(TextAttribute.FONT, new Font(Font.SANS_SERIF, Font.BOLD, 20));
			}
			else{

				attstr.addAttribute(TextAttribute.FONT, new Font(Font.SANS_SERIF, Font.PLAIN, 20));
			}
			iter = attstr.getIterator();
			measure = new LineBreakMeasurer(iter, frc);
			measure.setPosition(iter.getBeginIndex());

			x = (onlytext) ? 30 : 360f;
			bound = (onlytext) ? 840 : 510f;
			while (measure.getPosition() < iter.getEndIndex()) {
				TextLayout layout = measure.nextLayout(bound);
				y += layout.getAscent();
				layout.draw(graphic, x, y);
				y += layout.getDescent() + layout.getLeading();
			}
		}

		// header box
		// image
		if (!onlytext) {

			// image box
			// graphic.setColor(background);
			// graphic.fillRect(30, 120, 300, 450);

			// image
			BufferedImage img = ImageIO.read(new URL(tweetEntry.imageUrl));
			Integer width = img.getWidth();
			Integer height = img.getHeight();

			if (width > 290) {
				width = 290;
				height = (width * img.getHeight()) / img.getWidth();
			}
			if (height > 420) {
				height = 420;
				width = (height * img.getWidth()) / img.getHeight();
			}

			Integer center = 35 + (290 - width) / 2;
			graphic.drawImage(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), center, 130, null);
		}

		// done
		ImageIO.write(image, "png", stream);
		return stream.toByteArray();
	}

}
