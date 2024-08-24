package co.loyyee;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
    System.out.println(System.getProperty("os.name"));
    int exitCode = new CommandLine(new Tweeit()).execute(args);
		System.exit(exitCode);
	}
	
	@Override
	public Integer call() throws Exception {
		// TODO: setup authorization header OAuth 1.0
		
		System.out.println(apiKey);
		
		Map<String, String> params = new HashMap<>();
//		params.put("status", tweet);
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
						.POST(HttpRequest.BodyPublishers.ofString(" { \"text\" : \" Hello from CLI\"} " ))
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

class OAuth1 {
	
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
	private static String createSignatureBaseString(String method, String url, Map<String, String> params) throws UnsupportedEncodingException {
		StringBuilder baseBuilder = new StringBuilder();
		baseBuilder.append(method.toUpperCase())
				.append("&")
				.append(URLEncoder.encode(url, StandardCharsets.UTF_8))
				.append("&");
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
		return baseBuilder.toString();
	}
	private static String generateSHA1Signature(String baseString, String consumerSecret, String tokenSecret) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
		String key = URLEncoder.encode(consumerSecret, StandardCharsets.UTF_8.name()) + "&" + URLEncoder.encode(tokenSecret, StandardCharsets.UTF_8.name());
		final Mac mac = Mac.getInstance("HmacSHA1");
		final SecretKey secret = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), mac.getAlgorithm());
				mac.init(secret);
				byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(hash)	;
	}
}



@Command(name = "checksum", mixinStandardHelpOptions = true, version = "checksum 4.0",
		description = "Prints the checksum (SHA-256 by default) of a file to STDOUT.")
class CheckSum implements Callable<Integer> {
	
	@Parameters(index = "0", description = "The file whose checksum to calculate.")
	private File file;
	
	@Option(names = {"-a", "--algorithm"}, description = "MD5, SHA-1, SHA-256, ...")
	private String algorithm = "SHA-256";
	
	// this example implements Callable, so parsing, error handling and handling user
	// requests for usage help or version help can be done with one line of code.
	public static void main(String... args) {
		int exitCode = new CommandLine(new CheckSum()).execute(args);
		System.exit(exitCode);
	}
	
	@Override
	public Integer call() throws Exception { // your business logic goes here...
		byte[] fileContents = Files.readAllBytes(file.toPath());
		byte[] digest = MessageDigest.getInstance(algorithm).digest(fileContents);
		System.out.printf("%0" + (digest.length*2) + "x%n", new BigInteger(1, digest));
		return 0;
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
