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
 * Get reviewer's demographics and other information (including the 10 most recent review star ratings)
 * This class was added at a
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
        this.create_data_base();
	}

	public ArrayList<String> reviewer_info(String reviewerID) {
        System.out.println("Reviewer: " + reviewerID);
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
			doc = Jsoup.connect(url).header("User-Agent",
                    "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2").get();

            // reviewer ranking
			Elements Reviewer_ranking_e = doc.select("span.a-size-small:contains(Reviewer Ranking: #)");
            System.out.println(Reviewer_ranking_e);
            Pattern pattern = Pattern.compile("(Reviewer ranking: #)(\\S+)");
			Matcher matcher = pattern.matcher(Reviewer_ranking_e.text());
			if (matcher.find()) {
				Reviewer_ranking = matcher.group(2);
			}

            // review helpful votes
			Element total_vote = doc.select("span.a-size-small:contains(votes received on reviews)").first();
            if(total_vote != null){
                Element vote_parent = total_vote.parent();
                String votes_string = vote_parent.select("span:contains( of )").text();
                pattern = Pattern
                        .compile("([(])(\\S+)( of )(\\S+)([)])");
                matcher = pattern.matcher(votes_string);
                if(matcher.find()){
                    Total_helpful_votes = matcher.group(2) + " of "
                            + matcher.group(4);
                }
            }

            // total number of reviews
			Element Total_reviews_e = doc.select("div.reviews-link").first();
            if(Total_reviews_e != null){
                pattern = Pattern.compile("(Reviews [(])((\\S+))([)])");
                matcher = pattern.matcher(Total_reviews_e.text());
                if (matcher.find()) {
                    Total_reviews = matcher.group(2);
                }
            }

            // location of the reviewer (if listed)
            Element Location_e = doc.select("div.profile-name-container").first();
            if(Location_e.parent() != null)
                Location = Location_e.parent().text();


            //recent 10 ratings
			doc = Jsoup.connect(url2).get();
			Elements images = doc.select("img");
			for (Element image : images) {
				String imagealt = image.attr("alt");
                if (imagealt.contains("out of 5 stars")) {
                    Recent_rating.add(imagealt.substring(0, 1));
                }
            }

		} catch (IOException e) {
            System.out.println(e);
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
//					System.out.println(insert_q);
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
		}
		System.out.println("Table created successfully");
	}
}
