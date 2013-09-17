import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.*;

public class Review {
	/**
	 * All the pieces in a review on Amazon.com
	 * 
	 * @param aitemID
	 *            the unique item ID of a product
	 * @param areviewID
	 *            the unique review ID
	 * @param acustomerName
	 *            displayed name of a customer
	 * @param acustomerID
	 *            unique customer ID
	 * @param atitle
	 *            title of the review
	 * @param arating
	 *            star rating out of 5 given by the customer
	 * @param afullRating
	 *            max rating can be given (5 for now)
	 * @param ahelpfulVotes
	 *            number of readers who rated the review as helpful
	 * @param atotalVotes
	 *            total number of readers who voted usefulness of the review
	 * @param verifiedornot
	 *            whether the review is from a verified purchase
	 * @param realnameornot
	 *            whether the customer is using his real name when writing the
	 *            review
	 * @param aReviewDate
	 *            date of the review
	 * @param acontent
	 *            textual content of the review
	 */
	public Review(String aitemID, String areviewID, String acustomerName,
			String acustomerID, String atitle, int arating, int afullRating,
			int ahelpfulVotes, int atotalVotes, boolean verifiedornot,
			boolean realnameornot, Date aReviewDate, String acontent) {
		itemID = aitemID;
		reviewID = areviewID;
		customerName = acustomerName;
		customerID = acustomerID;
		title = atitle;
		rating = arating;
		fullRating = afullRating;
		helpfulVotes = ahelpfulVotes;
		totalVotes = atotalVotes;
		verifiedPurchase = verifiedornot;
		realName = realnameornot;
		reviewDate = aReviewDate;
		content = acontent;
	}

	/**
	 * Write single review into a Sqlite database Relatively inefficient, should
	 * use {@link Item#writeReviewsToDatabase(String)} method to write all reviews for an
	 * item at the same time instead
	 * 
	 * @param database
	 *            path of database
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void writeDatabase(String database) throws ClassNotFoundException,
			SQLException {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager
				.getConnection("jdbc:sqlite:" + database);
		PreparedStatement insertreview = conn
				.prepareStatement("insert into review (reviewid, title, content) values (?1, ?2, ?3);");
		PreparedStatement insertreviewinfo = conn
				.prepareStatement("insert into reviewinfo (addedDate, reviewDate, realName, verifiedPurchase, totalVotes, "
						+ "helpfulVotes, fullRating, rating, title, customerID, customerName, reviewID, itemID) values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13);");
		insertreview.setString(1, reviewID);
		insertreview.setString(2, title);
		insertreview.setString(3, content);
		insertreview.addBatch();
		insertreview.executeBatch();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String nowtime = dateFormat.format(date);
		insertreviewinfo.setString(1, nowtime);
		DateFormat dateFormat2 = new SimpleDateFormat("yyyy/MM/dd");
		String reviewdatestring = dateFormat2.format(reviewDate);
		insertreviewinfo.setString(2, reviewdatestring);
		insertreviewinfo.setString(3, String.valueOf(realName));
		insertreviewinfo.setString(4, String.valueOf(verifiedPurchase));
		insertreviewinfo.setInt(5, totalVotes);
		insertreviewinfo.setInt(6, helpfulVotes);
		insertreviewinfo.setInt(7, (int) fullRating);
		insertreviewinfo.setInt(8, (int) rating);
		insertreviewinfo.setString(9, title);
		insertreviewinfo.setString(10, customerID);
		insertreviewinfo.setString(11, customerName);
		insertreviewinfo.setString(12, reviewID);
		insertreviewinfo.setString(13, itemID);
		insertreviewinfo.addBatch();
		insertreviewinfo.executeBatch();
		conn.close();

	}

	public void updateReview(String aitemid, String areviewid,
			String acustomername, String acustomerID, String atitle,
			double arating, double afullRating, int ahelpfulVotes,
			int atotalVotes, boolean verified, boolean realname,
			Date areviewDate, String acontent) {
		this.itemID = aitemid;
		this.reviewID = areviewid;
		this.customerName = acustomername;
		this.customerID = acustomerID;
		this.title = atitle;
		this.rating = arating;
		this.fullRating = afullRating;
		this.helpfulVotes = ahelpfulVotes;
		this.totalVotes = atotalVotes;
		this.verifiedPurchase = verified;
		this.realName = realname;
		this.reviewDate = areviewDate;
		this.content = acontent;
	}

	String itemID;
	String reviewID;
	String customerName;
	String customerID;
	String title;
	double rating;
	double fullRating;
	int helpfulVotes;
	int totalVotes;
	boolean verifiedPurchase;
	boolean realName;
	Date reviewDate;
	String content;
}
