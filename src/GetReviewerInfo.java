import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Get reviewer's demographics and other information; This class was added at a
 * later date; the reviewer information is added to a separate SQLite database.
 * 
 * @author Feng Mai
 * 
 */
public class GetReviewerInfo {
	private String ReviewIDFile;
	private String outputDB;

	/**
	 * @param aReviewIDFile  a file with reviewer's ID (one per line)
	 * @param anOutputDB  a SQLite database for storing review's information
	 */
	public GetReviewerInfo(String aReviewIDFile, String anOutputDB) {
		this.ReviewIDFile = aReviewIDFile;
		this.outputDB = anOutputDB;
	}

	public ArrayList<String> reviewer_info(String reviewerID) {
		String url = "http://www.amazon.com/gp/pdp/profile/" + reviewerID;
		String url2 = "http://www.amazon.com/gp/cdp/member-reviews/"
				+ reviewerID + "/?sort_by=MostRecentReview";
		Document doc = null;
		ArrayList<String> attributes = new ArrayList<String>();
		String Reviewer_ranking = "";
		String Total_helpful_votes = "";
		String Total_reviews = "1";
		String Location = "";
		List<String> Recent_rating = new ArrayList<>();
		try {
			doc = Jsoup.connect(url).get();

			Elements Reviewer_ranking_e = doc
					.getElementsContainingOwnText("Top Reviewer Ranking:");
			Pattern pattern = Pattern.compile("(Top Reviewer Ranking: )(\\S+)");
			Matcher matcher = pattern.matcher(Reviewer_ranking_e.text());
			while (matcher.find()) {
				Reviewer_ranking = matcher.group(2);
			}

			pattern = Pattern
					.compile("(Total Helpful Votes: )(\\d+)( of )(\\d+)");
			matcher = pattern.matcher(Reviewer_ranking_e.text());
			while (matcher.find()) {
				Total_helpful_votes = matcher.group(2) + " of "
						+ matcher.group(4);
			}

			Elements Total_reviews_e = doc.getElementsByClass("seeAll");
			pattern = Pattern.compile("(See all )(\\S+)( reviews)");
			matcher = pattern.matcher(Total_reviews_e.text());
			while (matcher.find()) {
				Total_reviews = matcher.group(2);
			}

			try {
				Element Location_e = doc.getElementsByClass("personalDetails")
						.first().getElementsByIndexEquals(0).first();
				if (Location_e.text().contains("Location")) {
					Location = Location_e.text();
					Location = Location.replace("'", "");
				}
			} catch (Exception e) {

			}

			doc = Jsoup.connect(url2).get();
			Elements images = doc.select("img");
			for (Element image : images) {
				String imagealt = image.attr("alt");
				if (imagealt.contains("out of 5 stars")) {
					Recent_rating.add(imagealt.substring(0, 1));
				}
			}

		} catch (IOException e) {
			System.out.println(reviewerID + " Removed");
			return (null);
		}
		if (Recent_rating.size() > 10) {
			Recent_rating = Recent_rating.subList(0, 10);
		} else {
			Total_reviews = Integer.toString(Recent_rating.size());
		}
		String Recent_rating_joined = org.apache.commons.lang.StringUtils.join(
				Recent_rating, " ");
		attributes.addAll(Arrays.asList(reviewerID, Total_reviews,
				Reviewer_ranking, Total_helpful_votes, Location,
				Recent_rating_joined.toString()));
		return (attributes);
	}

	public void crawl() throws FileNotFoundException {
		Connection c = null;
		Statement stmt = null;
		int i = 0;
		for (String reviewer : read_id()) {
			if (i > -1) {
				ArrayList<String> info = reviewer_info(reviewer);
				String insert_q;
				if (info != null) {
					insert_q = "replace into attr values (";
					for (String value : info) {
						insert_q += "\'" + value + "\',";
					}
					insert_q = insert_q.substring(0, insert_q.length() - 1);
					insert_q += ")";
				} else {
					insert_q = "replace into attr values (\'" + reviewer
							+ "\',null,null,null,null,null)";
				}
				try {
					Class.forName("org.sqlite.JDBC");
					c = DriverManager
							.getConnection("jdbc:sqlite:"+outputDB);
					c.setAutoCommit(false);
					System.out.println(insert_q);
					stmt = c.createStatement();
					String sql = insert_q;
					stmt.executeUpdate(sql);
					stmt.close();
					c.commit();
					c.close();
				} catch (Exception e) {
					System.err.println(e.getClass().getName() + ": "
							+ e.getMessage());
					System.out.println(insert_q);
				}
			}
			i++;
		}
	}

	public HashSet<String> read_id() throws FileNotFoundException {
		HashSet<String> reviewerIDs = new HashSet<String>();
		Scanner s = new Scanner(new File(ReviewIDFile));
		while (s.hasNext()) {
			reviewerIDs.add(s.next());
		}
		s.close();
		return (reviewerIDs);
	}

	public void create_data_base() {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager
					.getConnection("jdbc:sqlite:"+outputDB);
			stmt = c.createStatement();
			String sql = "create table attr (reviewer_id Text Primary Key Not Null, total_reviews text, reviewer_ranking text, total_helpful_votes text, location text, recent_ratings text)";
			stmt.executeUpdate(sql);
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Table created successfully");
	}
}
