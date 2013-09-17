import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.sql.SQLException;
import java.text.ParseException;
import java.io.IOException;

public class crawler {

	/**
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, ParseException,
			ClassNotFoundException, SQLException, InvalidKeyException,
			NoSuchAlgorithmException, InterruptedException {

		//example: write all reviews for Samsung Tab 3 to a SQLite database
		Item samsungTab3 = new Item("B00D02AGU4");
		samsungTab3.fetchReview();
		samsungTab3.writeReviewsToDatabase("c:/reviewtest.db", false);
	}

}
