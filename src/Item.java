import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * A product and all of its reviews
 * @author Feng Mai
 * 
 */


public class Item {
	public Item(String theitemid) {
		itemID = theitemid;
		reviews = new ArrayList<Review>();
	}

	public void addReview(Review thereview) {
		reviews.add(thereview);
	}

    /**
     * retry the Jsoup connection
     * @param url
     * @return
     */
    public org.jsoup.nodes.Document jsoup_load_with_retry(String url) throws IOException {
        int max_retry = 10;
        int retry = 1;
        int sleep_sec = 2;
        org.jsoup.nodes.Document content = null;

        while(retry <= max_retry){
            try {
                content = Jsoup.connect(url).timeout(10 * 1000).get();
                break;
            } catch (Exception ex){
                //wait before retry
                System.out.println(ex.getMessage() + " retrying..");
                try {
                    TimeUnit.SECONDS.sleep(sleep_sec);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            retry++;
        }
        return content;
    }

	/**
	 * Fetch all reviews for the item from Amazon.com
	 */
	public void fetchReview() {
		String url = "http://www.amazon.com/product-reviews/" + itemID
				+ "/?showViewpoints=0&sortBy=byRankDescending&pageNumber=" + 1;
		try {
			// Get the max number of review pages;
			org.jsoup.nodes.Document reviewpage1 = null;
//			reviewpage1 = Jsoup.connect(url).timeout(10*1000).get();
            reviewpage1 = jsoup_load_with_retry(url);
			int maxpage = 1;
			Elements pagelinks = reviewpage1.select("a[href*=pageNumber=]");
			if (pagelinks.size() != 0) {
				ArrayList<Integer> pagenum = new ArrayList<Integer>();
				for (Element link : pagelinks) {
					try {
						pagenum.add(Integer.parseInt(link.text()));
					} catch (NumberFormatException nfe) {
					}
				}
				maxpage = Collections.max(pagenum);
			}
			// collect review from each of the review pages;
			for (int p = 1; p <= maxpage; p = p + 1) {
				url = "http://www.amazon.com/product-reviews/"
						+ itemID
						+ "/?sortBy=helpful&pageNumber="
						+ p;
				org.jsoup.nodes.Document reviewpage = null;
//                reviewpage = Jsoup.connect(url).timeout(10*1000).get();
                reviewpage = jsoup_load_with_retry(url);
                if (reviewpage.select("div.a-section.review").isEmpty()) {
					System.out.println(itemID + " " + "no reivew");
				} else {
					Elements reviewsHTMLs = reviewpage.select(
							"div.a-section.review");
					for (Element reviewBlock : reviewsHTMLs) {
                        Review theReview = cleanReviewBlock(reviewBlock);
						this.addReview(theReview);
					}
				}

			}

		}
		catch (Exception e) {
			System.out.println(itemID + " " + "Exception" + " " + e.toString());
		}

	}

	/**
	 * cleans the html block that contains a review
	 * 
	 * @param reviewBlock
	 *            a html review block (Jsoup Element)
	 * @return
	 * @throws ParseException
	 */
	public Review cleanReviewBlock(Element reviewBlock) throws ParseException {
		String theitemID = this.itemID;
		String reviewID = "";
		String customerName = "";
		String customerID = "";
		String title = "";
		int rating = 0;
		int fullRating = 5;
		int helpfulVotes = 0;
		int totalVotes = -1; // Note: 2016-06-25: Amazon no longer shows the total helpful votes
		boolean verifiedPurchase = false;
		String realName = "N/A"; //Note 2015-11-14 : Amazon no longer shows the real name badge
		Date reviewDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
				.parse("January 1, 1900");
		String content = "";


		// review id
			reviewID = reviewBlock.id();
		try {
			// customer name and id
			Elements customerIDs = reviewBlock.getElementsByAttributeValueContaining(
					"href", "/gp/pdp/profile/");
			if (customerIDs.size() > 0) {
				Element customer = customerIDs.first();
				String customerhref = customer.attr("href");
				String patternString = "(/gp/pdp/profile/)(.+)(/)";
				Pattern pattern = Pattern.compile(patternString);
				Matcher matcher = pattern.matcher(customerhref);
				while(matcher.find()) {
					// cutomer id;
					customerID = matcher.group(2);
					// customer name;
					customerName = customer.text();
				}
			}
			// title
			Element reviewTitle = reviewBlock.select("a.review-title").first();
			title = reviewTitle.text();

			// rating
			Element star = reviewBlock.select("i.a-icon-star").first();
			String starinfo = star.text();
			rating = Integer.parseInt(starinfo.substring(0, 1));

			// usefulness voting
			Elements votes = reviewBlock.select("span.review-votes");
			if (votes.size() > 0) {
				String votingtext = votes.first().text();
				Pattern pattern2 = Pattern.compile("(\\S+)( people found this helpful)");
				Matcher matcher2 = pattern2.matcher(votingtext);
				matcher2.find();
				while(matcher2.find()) {
					helpfulVotes = Integer.parseInt(matcher2.group(1).replaceAll(",", ""));
				}
//			totalVotes = Integer.parseInt(matcher2.group(3).replaceAll(",", ""));
			}

			// verified purchase
			Elements verified = reviewBlock.select("span.a-size-mini:contains(Verified Purchase)");
			if (verified.size() > 0) {
				verifiedPurchase = true;
			}


			// review date
			Elements date = reviewBlock.select("span.review-date");
			String datetext = date.first().text();
			datetext = datetext.substring(3); // remove "On "
			reviewDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
					.parse(datetext);

			// review content
			Element contentDoc = reviewBlock.select("span.review-text").first();
			content = contentDoc.text();
		}
		catch (Exception e) {
			System.out.println(reviewID + " " + "Exception" + " " + e.toString());
		}
		Review thereview = new Review(theitemID, reviewID, customerName,
				customerID, title, rating, fullRating, helpfulVotes,
				totalVotes, verifiedPurchase, realName, reviewDate, content);
        return thereview;
	}

	/**
	 * Write all reviews into a Sqlite database
	 * 
	 * @param database
	 *            Sqlite database file path
	 * @param API
	 *            a boolean value indicating whether to get item related
	 *            information from Product Advertising API (e.g. price, sells
	 *            rank)
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws ClientProtocolException
	 * @throws SQLException
	 * @throws IOException
	 */
	public synchronized void writeReviewsToDatabase(String database, boolean API)
			throws InvalidKeyException, ClassNotFoundException,
			NoSuchAlgorithmException, ClientProtocolException, SQLException,
			IOException {
		if (API == true) {
			DatabaseUpdater.doUpdate(database, reviews, itemID,
					getXMLLargeResponse());
		} else {
			DatabaseUpdater.doUpdate(database, reviews, itemID, "");
		}
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String timenow = dateFormat.format(date);
		System.out.println(this.itemID + " Finished " + timenow);

	}

	/**
	 * @return the RAW XML document of ItemLookup (Large Response) from Amazon
	 *         product advertisement API
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String getXMLLargeResponse() throws InvalidKeyException,
			NoSuchAlgorithmException, ClientProtocolException, IOException {
		String responseBody = "";
		String signedurl = signInput();
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(signedurl);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			responseBody = httpclient.execute(httpget, responseHandler);
			// responseBody now contains the contents of the page
			// System.out.println(responseBody);
			httpclient.getConnectionManager().shutdown();
		} catch (Exception e) {
			System.out.println("Exception" + " " + itemID + " " + e.getClass());
		}
		return responseBody;
	}

	/**
	 * Sign the REST request
	 * 
	 * @return REST request to acquire a "Large ResponseGroup" from ItemLookup
	 *         operation in Amazon Advertising API
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	private String signInput() throws InvalidKeyException,
			NoSuchAlgorithmException, UnsupportedEncodingException {
		// Input to Sign;
		Map<String, String> variablemap = new HashMap<String, String>();
		//*****ADD YOUR AssociateTag HERE*****
		variablemap.put("AssociateTag", "");
		variablemap.put("Operation", "ItemLookup");
		variablemap.put("Service", "AWSECommerceService");
		variablemap.put("ItemId", itemID);
		variablemap.put("ResponseGroup", "Large");

		// Sign and get the REST url;
		SignedRequestsHelper helper = new SignedRequestsHelper();
		String signedurl = helper.sign(variablemap);
		return signedurl;
	}

	/**
	 * Get and print item info using Amazon's Product Advertising API. NOT
	 * COMPLETE
	 * 
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public void getBookSaleInfo() throws InvalidKeyException,
			NoSuchAlgorithmException, UnsupportedEncodingException {
		String signedurl = signInput();
		System.out.println(signedurl);

		// Info requested;
		ArrayList<String> TagNames = new ArrayList<String>();
		TagNames.add("Title");
		TagNames.add("SalesRank");
		TagNames.add("ListPrice");
		TagNames.add("LowestNewPrice");
		TagNames.add("LowestUsedPrice");
		TagNames.add("TotalNew");
		TagNames.add("TotalUsed");
		TagNames.add("PublicationDate");
		TagNames.add("Author");
		TagNames.add("Publisher");
		TagNames.add("EditorialReview");
		// fetch info and print;
		Map<String, String> InfoTagMap = fetchInfo(signedurl, TagNames);
		System.out.println(InfoTagMap.toString());
	}

	/**
	 * Fetch the results of product info requested and return a Hashmap
	 * 
	 * @param requestUrl
	 *            Signed REST request url
	 * @param TagNames
	 *            Strings ArrayList of product info tags
	 *            (http://docs.amazonwebservices
	 *            .com/AWSECommerceService/latest/DG/RG_Large.html)
	 * @return Map(Tag Name, Value)
	 */
	private static Map<String, String> fetchInfo(String requestUrl,
			ArrayList<String> TagNames) {
		Map<String, String> InfoTagMap = new HashMap<String, String>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(requestUrl);
			if (doc.getElementsByTagName("IsValid").item(0).getTextContent()
					.equals("True")) {
				for (String tag : TagNames) {
					NodeList titleNode = doc.getElementsByTagName(tag);
					if (tag.equals("Title")) {
						InfoTagMap.put(tag, titleNode.item(0).getTextContent());
					} else {
						ArrayList<String> infolist = new ArrayList<String>();
						for (int i = 0; i < titleNode.getLength(); i++) {
							infolist.add(titleNode.item(i).getTextContent());
						}
						InfoTagMap.put(tag, infolist.toString());
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return InfoTagMap;
	}

	public String itemID;
	public ArrayList<Review> reviews;
}
