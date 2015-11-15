customer-review-crawler
=======================

A crawler to collect reviews and product infomation on Amazon.com and save them to SQLite databases.


# Quick Start Guide

Updated: 2015-11-14 to reflect changes on Amazon website.

## Get reviews from one product
To get all reviews for a product, first get the Amazon Standard Identification Number (ASIN) of the product. It is the 10-character alphanumeric ID followed by /product/ in the url.
Then add the following code to the main function
```java
	Item samsungTab3 = new Item("B00D02AGU4");
	samsungTab3.fetchReview();
	samsungTab3.writeReviewsToDatabase("reviewtest.db", false);
```
Data in the created SQlite database can be managed or exported using tools such as [SQLiteStudio](http://sqlitestudio.pl/) or [RSQLite](http://sandymuspratt.blogspot.com/2012/11/r-and-sqlite-part-1.html).

## Get reviewer information
```java
	GetReviewerInfo reviewer_crawler = new GetReviewerInfo("reviewer_ids.txt","reviewer_test.db");
	reviewer_crawler.crawl();
```
where reviewer_ids.txt is a file whose content lists all reviewer ids, and reviewer_test.db is a SQlite db.

## Get reviews from a product category
To get all reviews from a product category, find out the node ID for the category from Amazon.com's url (&node=). The node ID will be the first arugument in the GetASINbyNode() constructor.
Then you should estimate about how many products there are and divide that number by 5, which will be the third argument in GetASINbyNode().
For example:
```java
	GetASINbyNode getIDs = new GetASINbyNode("541966%2C1232597011", 1,	10);
	getIDs.getIDList();
	getIDs.writeIDsToCSV("idlist.txt");
	ItemList thelist = new ItemList("idlist.txt");
	thelist.writeReviewsToDatabase("reviews.db", false);
```

## Get pricing information
To get the pricing information you need to register as an Amazon Associate (https://affiliate-program.amazon.com).
Then you need to add your Associate tag in the signInput() function in Item.java:
```java
	variablemap.put("AssociateTag", "your_tag_here");
```
You also need your Product Advertising API Key & Secret Key and add them to SignedRequestsHelper.java:
```java
	private String awsAccessKeyId = "your_api_key";
	private String awsSecretKey = "your_seceret_key";
```
Once you have them you can change the second argument of writeReviewsToDatabase() to true, and pricing information will be saved in the same database in XML format.

To test your keys, try
```java
	Item testItem = new Item("B00D02AGU4");
	System.out.println(testItem.getXMLLargeResponse());
```


# Common Exceptions
1. java.io.IOException means that the item no longer exist on Amazon.com. You do not have to do anything with that item.
2. java.net.SocketTimeoutException means that connection to the website is taking too long. Rerun the crawler on the items with this exception.

# Licence
The code is released into public domain. If you find the code useful in your research work, I appreciate if you can cite
"Market Dynamics and User-Generated Content about Tablet Computers" by Xin (Shane) Wang, Feng Mai and Roger H.L. Chiang, Marketing Science 33.3 (2014): 449-458
