import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.sql.*;

/**
 * Update or insert all reviews and product relating information to a SQLite
 * database
 * 
 * @author Feng Mai
 * 
 */
public class DatabaseUpdater {

	private static final Lock lock = new ReentrantLock();

	/**
	 * @param database
	 *            file name of the SQLite database
	 * @param reviews
	 *            an ArralyList of reviews
	 * @param itemID
	 *            ID of the item
	 * @param itemInfo
	 *            item info (in XML string) returned by Product Advertising API
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void doUpdate(String database, ArrayList<Review> reviews,
			String itemID, String itemInfo) throws SQLException,
			ClassNotFoundException, IOException {
		lock.lock();
		try {
			Class.forName("org.sqlite.JDBC");

			// if database not exist
			if (!(new File(database).isFile())) {
				Statement stmt = null;
				try {
					Connection conn = DriverManager
							.getConnection("jdbc:sqlite:" + database);
					stmt = conn.createStatement();
					String sql = "CREATE TABLE reviewinfo ( [KEY] INTEGER PRIMARY KEY, addedDate TEXT, reviewDate TEXT, realName TEXT, verifiedPurchase TEXT, totalVotes NUMERIC, helpfulVotes NUMERIC, fullRating NUMERIC, rating NUMERIC, title TEXT, customerID TEXT, customerName TEXT, reviewID TEXT UNIQUE ON CONFLICT REPLACE, itemID TEXT );";
					stmt.executeUpdate(sql);
					sql = "CREATE TABLE iteminfo ( id INTEGER PRIMARY KEY AUTOINCREMENT, itemID TEXT UNIQUE ON CONFLICT IGNORE, itemXMLInfo TEXT );";
					stmt.executeUpdate(sql);
					sql = "CREATE TABLE review ( [KEY] INTEGER PRIMARY KEY, reviewid TEXT UNIQUE ON CONFLICT REPLACE, title TEXT, content TEXT );";
					stmt.executeUpdate(sql);
					sql = "CREATE INDEX idx_reviewinfo ON reviewinfo ( reviewID );";
					stmt.executeUpdate(sql);
					stmt.close();
					conn.close();
				} catch (Exception e) {
					System.err.println(e.getClass().getName() + ": "
							+ e.getMessage());
					System.exit(0);
				}
				System.out.println("Table created successfully");
			}

			Connection conn = DriverManager.getConnection("jdbc:sqlite:"
					+ database);
			PreparedStatement insertreview = conn
					.prepareStatement("insert into review (reviewid, title, content) values (?1, ?2, ?3);");
			PreparedStatement insertreviewinfo = conn
					.prepareStatement("insert into reviewinfo (addedDate, reviewDate, realName, verifiedPurchase, totalVotes, "
							+ "helpfulVotes, fullRating, rating, title, customerID, customerName, reviewID, itemID) values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13);");
			for (Review areview : reviews) {
				insertreview.setString(1, areview.reviewID);
				insertreview.setString(2, areview.title);
				insertreview.setString(3, areview.content);
				insertreview.addBatch();

				DateFormat dateFormat = new SimpleDateFormat(
						"yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				String nowtime = dateFormat.format(date);
				insertreviewinfo.setString(1, nowtime);
				DateFormat dateFormat2 = new SimpleDateFormat("yyyy/MM/dd");
				String reviewdatestring = dateFormat2
						.format(areview.reviewDate);
				insertreviewinfo.setString(2, reviewdatestring);
				insertreviewinfo.setString(3, String.valueOf(areview.realName));
				insertreviewinfo.setString(4,
						String.valueOf(areview.verifiedPurchase));
				insertreviewinfo.setInt(5, areview.totalVotes);
				insertreviewinfo.setInt(6, areview.helpfulVotes);
				insertreviewinfo.setInt(7, (int) areview.fullRating);
				insertreviewinfo.setInt(8, (int) areview.rating);
				insertreviewinfo.setString(9, areview.title);
				insertreviewinfo.setString(10, areview.customerID);
				insertreviewinfo.setString(11, areview.customerName);
				insertreviewinfo.setString(12, areview.reviewID);
				insertreviewinfo.setString(13, areview.itemID);
				insertreviewinfo.addBatch();
			}
			conn.setAutoCommit(false);
			insertreview.executeBatch();
			insertreviewinfo.executeBatch();

			// insert item information from Product Advertisement API's Large
			// Response
			// in raw XML format
			PreparedStatement insertitemXML = conn
					.prepareStatement("insert into iteminfo(itemid, itemXMLInfo) values (?, ?);");
			insertitemXML.setString(1, itemID);
			insertitemXML.setString(2, itemInfo);
			insertitemXML.addBatch();
			insertitemXML.executeBatch();
			conn.commit();
			conn.close();
		} finally {
			lock.unlock();
		}
	}
}
