import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * This class provides methods to digitally sign a REST request for Product Advertising API
 * @author Based on templates provided by Amazon.com 
 *         
 */
public class SignedRequestsHelper {
	private static final String UTF8_CHARSET = "UTF-8";
	private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
	private static final String REQUEST_URI = "/onca/xml";
	private static final String REQUEST_METHOD = "GET";

	private String endpoint = "webservices.amazon.com"; // must be lowercase
	// KeyId and SecretKey aws ccount;
	//*****ADD YOUR KEY HERE*****
	private String awsAccessKeyId = "";
	private String awsSecretKey = "";

	private SecretKeySpec secretKeySpec = null;
	private Mac mac = null;

	public SignedRequestsHelper() throws InvalidKeyException,
			NoSuchAlgorithmException, UnsupportedEncodingException {
		byte[] secretyKeyBytes = awsSecretKey.getBytes(UTF8_CHARSET);
		secretKeySpec = new SecretKeySpec(secretyKeyBytes,
				HMAC_SHA256_ALGORITHM);
		mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
		mac.init(secretKeySpec);
	}

	public String sign(Map<String, String> params) {
		params.put("AWSAccessKeyId", awsAccessKeyId);
		params.put("Timestamp", timestamp());

		SortedMap<String, String> sortedParamMap = new TreeMap<String, String>(
				params);
		String canonicalQS = canonicalize(sortedParamMap);
		String toSign = REQUEST_METHOD + "\n" + endpoint + "\n" + REQUEST_URI
				+ "\n" + canonicalQS;

		String hmac = hmac(toSign);
		String sig = percentEncodeRfc3986(hmac);
		String url = "http://" + endpoint + REQUEST_URI + "?" + canonicalQS
				+ "&Signature=" + sig;

		return url;
	}

	private String hmac(String stringToSign) {
		String signature = null;
		byte[] data;
		byte[] rawHmac;
		try {
			data = stringToSign.getBytes(UTF8_CHARSET);
			rawHmac = mac.doFinal(data);
			Base64 encoder = new Base64();
			signature = new String(encoder.encode(rawHmac));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
		}
		return signature;
	}

	private String timestamp() {
		String timestamp = null;
		Calendar cal = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
		timestamp = dfm.format(cal.getTime());
		return timestamp;
	}

	private String canonicalize(SortedMap<String, String> sortedParamMap) {
		if (sortedParamMap.isEmpty()) {
			return "";
		}

		StringBuffer buffer = new StringBuffer();
		Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet()
				.iterator();

		while (iter.hasNext()) {
			Map.Entry<String, String> kvpair = iter.next();
			buffer.append(percentEncodeRfc3986(kvpair.getKey()));
			buffer.append("=");
			buffer.append(percentEncodeRfc3986(kvpair.getValue()));
			if (iter.hasNext()) {
				buffer.append("&");
			}
		}
		String cannoical = buffer.toString();
		return cannoical;
	}

	private String percentEncodeRfc3986(String s) {
		String out;
		try {
			out = URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20")
					.replace("*", "%2A").replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}

}