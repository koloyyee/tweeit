package co.loyyee;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="tweeit", description = "Tweet from here.")
class Tweeit implements Callable<String> {
	static {
		Env.get();
	}
	private static final String apiKey = System.getProperty("API_KEY");
	private static final String apiSecret = System.getProperty("API_SECRET_KEY");
	private static final String token = System.getProperty("ACCESS_TOKEN");
	private static final String tokenSecret = System.getProperty("TOKEN_SECRET");
	private static final String bearer = System.getProperty("BEARER");
	
	public static void main(String[] args){
    System.out.println(apiKey);
	}
	
	@Override
	public String call() throws Exception {
		return "";
	}
}

/**
 * Access with System.getProperty(KeyString)
 * */
class Env{
	
	public static void get(String path) {
		if(!path.contains(".env")) throw new IllegalArgumentException("only .env allowed");
		Path env= Path.of(path).toAbsolutePath();
		try(var lines = Files.newBufferedReader(env).lines();) {
			Properties properties = new Properties();
			lines.forEach(line -> {
				String[] kv = line.split("=")	;
				properties.putIfAbsent(kv[0], kv[1])	;
			});
			System.setProperties(properties);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void get()	{
		Path env= Path.of("src", "main", "resources", ".env").toAbsolutePath();
		if( !Files.isRegularFile(env)) throw new IllegalArgumentException(" Place the .env file in the src/main/resources directory. ");
		get(env.toString());
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
