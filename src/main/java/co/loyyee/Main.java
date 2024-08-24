package co.loyyee;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/***/
@Command(name="tweeit", description = "Tweet from here.",  mixinStandardHelpOptions = true)
class Tweeit implements Callable<Integer> {
	
	private final String apiKey;
	private final String apiSecret;
	private final String token;
	private final String tokenSecret ;
	private final String bearer;

	private final Properties properties = 	Env.get();
	
	@Parameters(index="0", description = "Your tweet (less than 200 characters")
	private String tweet;
	
	public Tweeit() {
		apiKey = properties.getProperty("API_KEY");
		apiSecret = properties.getProperty("API_SECRET_KEY");
		token = properties.getProperty("ACCESS_TOKEN");
		tokenSecret = properties.getProperty("TOKEN_SECRET");
		bearer = properties.getProperty("BEARER");
	}
	
	public static void main(String... args){
    int exitCode = new CommandLine(new Tweeit()).execute(args);
		System.exit(exitCode);
	}
	
	@Override
	public Integer call() throws Exception {
		if(tweet.trim().length() >= 200 ) throw new IllegalArgumentException("Tweet too long.");
		if(tweet.contains("\"")) {
			tweet.replace("\"", "");
		}
    System.out.println(tweet);
		String tweetJson =" { \"text\" : \"%s\"} ".formatted(tweet);
		
		System.out.println(tweetJson);
		
		Map<String, String> params = new HashMap<>();
		String authHeader = OAuth1.generateAuthorizationHeaders(
				"POST",
				"https://api.x.com/2/tweets",
				properties,
				params,
				apiSecret,
				tokenSecret
		);
		
		HttpClient client = HttpClient.newBuilder().build();
		
		HttpRequest request =
				HttpRequest.newBuilder()
						.uri(URI.create("https://api.x.com/2/tweets"))
						.POST(HttpRequest.BodyPublishers.ofString(tweetJson))
						.headers(
								"Authorization", authHeader,
							"Content-Type"	, "application/json"
								)
						.build();
    System.out.println(request.headers());
		System.out.println(request.bodyPublisher().get());
		var response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
		String str = response.get().body();
    System.out.println(str);
		return 0;
	}
}


/**
 * For Posting on X.com we can use OAuth 1.0 or 2, <br>
 * OAuth 1.0 requires less setup but we need to create signature with SHA-1 hashing algorithm.
 * */
class OAuth1 {
	
	/**
	 *<h3>Creating OAuth 1.0 Signature here. </h3>
	 * Product: <br>
	 * Authorization=[OAuth oauth_nonce="xxx", <br>
	 * oauth_signature="xxx", <br>
	 * oauth_token="xxx",  <br>
	 * oauth_consumer_key="xxx",  <br>
	 * oauth_signature_method="HMAC-SHA1", <br>
	 * oauth_timestamp="xxx",  <br>
	 * oauth_version="1.0"]
	 *
	 * */
	public static String generateAuthorizationHeaders(String method, String url, Properties properties , Map<String, String> params, String consumerSecret, String tokenSecret){
		
		params.put("oauth_consumer_key", properties.getProperty("API_KEY"));
		params.put("oauth_token", properties.getProperty("ACCESS_TOKEN"));
		params.put("oauth_signature_method", "HMAC-SHA1");
		params.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
		params.put("oauth_nonce", String.valueOf(System.nanoTime()));
		params.put("oauth_version", "1.0");
		
		try {
			String baseString = createSignatureBaseString(method, url, params);
			String signature = generateSHA1Signature(baseString, consumerSecret, tokenSecret);
			params.put("oauth_signature", signature);
			
			StringBuilder headerBuilder = new StringBuilder("OAuth ");
			for(Map.Entry<String, String> entry: params.entrySet()) {
				headerBuilder
						.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
						.append("=\"")
						.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()))
						.append("\",");
			}
			headerBuilder.setLength(headerBuilder.length() - 1);
			return headerBuilder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
			
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

/**
 * <h3>Create a Base String Signature for OAuth 1.0</h3>
 * The Goal is to create a Base String Signature for OAuth 1.0,<br>
 * which will be hashed with HMAC-SHA1. <br>
 * The whole string is {@link URLEncoder#encode(String, String)}. <br>
 * Product: <br>
 * POST&https%3A%2F%2Fapi.x.com%2F2%2Ftweets&oauth_consumer_key%3Dxxx%26oauth_nonce%3Dxxx%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3Dxxx%26oauth_token%3Dxxx%26oauth_version%3D1.0
 * <br>
 * POST - HTTP Request Method <br>
 * https%3A%2F%2Fapi.x.com%2F2%2Ftweets - https://api.x.com/2/tweets  <br>
 * oauth_consumer_key%3Dxxx - oauth_consumer_key=xxx
 * ...etc <br>
 * oauth_signature_method%3DHMAC-SHA1 - oauth_signature_method=HMAC-SHA1
 * */
	private static String createSignatureBaseString(String method, String url, Map<String, String> params) throws UnsupportedEncodingException {
		// Concat the string with StringBuilder
		StringBuilder baseBuilder = new StringBuilder();
		baseBuilder.append(method.toUpperCase())
				.append("&")
				.append(URLEncoder.encode(url, StandardCharsets.UTF_8))
				.append("&");
		
		/**
		 * Sorting with {@link TreeMap} ensures that the parameters are in lexicographical (alphabetical) order,
		 * which is a requirement for generating the correct signature base string in OAuth 1.0.
		 *
		 * This order is necessary to ensure that both the client and server generate the same signature
		 * for the same request, thus maintaining the integrity and security of the OAuth process.
		 *
		 * The order will be
		 * oauth_consumer_key, oauth_nonce, oauth_signature_method, oauth_timestamp, oauth_token, oauth_version
		 * */
		TreeMap<String, String>	 sortedParams = new TreeMap<>(params);
		StringBuilder paramBuilder  = new StringBuilder();
		for(Map.Entry<String, String> entry : sortedParams.entrySet()){
			paramBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
					.append("=")
					.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()))
					.append("&");
		}
		paramBuilder.setLength(paramBuilder.length() - 1);
		baseBuilder.append(URLEncoder.encode(paramBuilder.toString(), StandardCharsets.UTF_8.name()));
    System.out.println(baseBuilder.toString());
		return baseBuilder.toString();
	}
	
	/**
	 * <h3>This is hashing process with HMAC-SHA-1. </h3>
	 * key is the combination of consumer secret(api secret key from x.com) and token secret (token secret from x.com) <br>
	 * {@link Mac} is a Javax library that hash data based on input algorithm, this time will be HMAC-SHA-1. <br>
	 * and finally return a Base64 encoded string of the hashed value.
	 * */
	private static String generateSHA1Signature(String baseString, String consumerSecret, String tokenSecret) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		String key = URLEncoder.encode(consumerSecret, StandardCharsets.UTF_8.name()) + "&" + URLEncoder.encode(tokenSecret, StandardCharsets.UTF_8.name());
		final Mac mac = Mac.getInstance("HmacSHA1");
		final SecretKey secret = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
				mac.init(secret);
				byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hash)	;
	}
}

/**
 * Access with System.getProperty(KeyString)
 * */
class Env{
	
	public static Optional<Properties> get(String path) {
		if(!path.contains(".env")) throw new IllegalArgumentException("only .env allowed");
		Path env= Path.of(path).toAbsolutePath();
		try(var lines = Files.newBufferedReader(env).lines();) {
			Properties properties = new Properties();
			lines.forEach(line -> {
				String[] kv = line.split("=")	;
				properties.putIfAbsent(kv[0], kv[1])	;
			});
//			System.setProperties(properties);
			
			return Optional.of(properties);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Properties get()	{
		Path env= Path.of("src", "main", "resources", ".env").toAbsolutePath();
		if( !Files.isRegularFile(env)) throw new IllegalArgumentException(" Place the .env file in the src/main/resources directory. ");
		return get(env.toString()).orElseThrow();
	}
}
