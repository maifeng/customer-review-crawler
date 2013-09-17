
# Quick Start Guide

## Get reviews from one product
To get all reviews for a product, first get the Amazon Standard Identification Number (ASIN) of the product. It is the 10-character alphanumeric ID followed by /product/ in the url.
Then add the following code to the main function
```java
		Item samsungTab3 = new Item("B00D02AGU4");
		samsungTab3.fetchReview();
		samsungTab3.writeReviewsToDatabase("c:/reviewtest.db", false);
```

## Get reviews from a product category 
To get all reviews from a product category, first find out the node ID for the category from Amazon.com's url (&node=).

```java
	GetTabletASIN getTabletid = new GetASINbyNode("541966%2C1232597011", 1,	300);
	getTabletid.getIDList();
	getTabletid.writeIDsToCSV("c:/idlist.txt");
	ItemList thelist = new ItemList("c:/idlist.txt");
	thelist.writeReviewsToDatabase("c:/reviews.db", false);
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
Once you have them you can change the 2nd argument of writeReviewsToDatabase() to true, and pricing information will be saved in the same database in XML format.

To test your keys, try
```java
		Item testItem = new Item("B00D02AGU4");
		System.out.println(testItem.getXMLLargeResponse());
```



# Common Exceptions:
1. java.io.IOException means that the item no longer exist on Amazon.com. You do not have to do anything with that item.
2. java.net.SocketTimeoutException means that connection to the website is taking too long. Rerun the crawler on the items with this exception.