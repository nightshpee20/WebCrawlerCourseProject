package execution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RefactoredMain {
	public static Set<String> topUrls;
	public static Set<String> urls;
	public static Set<String> sources;
	public static Options options;
	public static AtomicInteger imgCounter = new AtomicInteger();
	
	public static String targetDirPath = "C:\\Users\\night\\Desktop\\Java Internship\\Web Crawler\\img";
	
	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		topUrls = Collections.synchronizedSet(new HashSet<>());
		urls = Collections.synchronizedSet(new HashSet<>());
		sources = Collections.synchronizedSet(new HashSet<>());
		options = new Options();
		
		createOptions();
		
		downloadImage(" ");
		
		try {
			parseOptions(args);
		} catch (ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
	
		for (String str : args)
			if (validateUrl(str)) {
				topUrls.add(str);
				crawlPage(str);
			}
			
		urls.forEach((url) -> {
			crawlPage(url);
		});
		////
		System.out.println("TOP URLS:");
		topUrls.forEach((url) -> {
			System.out.println(url);
		});
		System.out.println("SUB URLS:");
		urls.forEach((url) -> {
			System.out.println(url);
		});
		System.out.println("SOURCES:");
		sources.forEach((source) -> {
			System.out.println(source);
		});
	}

	private static boolean validateUrl(String url) {
		try {
			new URL(url).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static void crawlPage(String url) {
		Document doc = null;
		try {			
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return;
		}
		
		Elements links = doc.select("a[href]");
		Elements images = doc.select("img[src]");
	
		for (Element link : links) {
			String subUrl = link.attr("abs:href");

			if (subUrl.lastIndexOf("#") == -1 && isSubUrl(subUrl)) {
				for (Element image : images) {
		    		String src = image.attr("src");
		   
		    		if (sources.contains(src))
		    			continue;
		    		
		    		if (src.indexOf("https://") != 0)
		    			continue;
		    		
		    		try {
						downloadImage(src);
					} catch (URISyntaxException | IOException | InterruptedException e) {
						System.out.println("Failed to download image: " + src);
					}
		    		
		    		sources.add(src);
		    	}
				urls.add(subUrl);
			}
		}
	}
	
	private static boolean isSubUrl(String subUrl) {
		for (String topUrl : topUrls)
			if (subUrl.lastIndexOf(topUrl) != -1)
				return true;
		return false;
	}
	
	private static void downloadImage(String source) throws URISyntaxException, IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		
		String url1 = "https://wow.zamimg.com/images/wow/journal/ui-ej-boss-grobbulus.png";
		String url2 = "//cdn.shopify.com/s/files/1/0075/8526/7775/products/0031627fa5b4a012f9d5e18f5f028c3f_720x.jpg?v=1630286207";
		
		URI uri = new URI(url1);
		HttpRequest request = HttpRequest.newBuilder()
							  .uri(uri)
							  .GET()
							  .build();
		
		
		HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		System.out.println(response.body().length);
		
		String fileType = response.headers().firstValue("Content-Type").get();
		int backslashIndex = fileType.indexOf("/");
		fileType = fileType.substring(backslashIndex + 1, fileType.length());
		
		System.out.println(fileType);
		Path path = Paths.get("img\\bruh2." + fileType);
		Files.write(path, response.body());
	}
	
	private static void parseOptions(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);
		
		if (cmd.hasOption("outputDir")) {
			String val = (String)cmd.getParsedOptionValue(options.getOption("outputDir"));
			
		} 
		if (cmd.hasOption("imageFormat")) {
			String val = (String)cmd.getParsedOptionValue(options.getOption("imageFormat"));
			
		}
		if (cmd.hasOption("userAgent")) {
			String val = (String)cmd.getParsedOptionValue(options.getOption("userAgent"));
			
		}
	}
	
	private static void createOptions() {
		Option outputDir = new Option("outputDir", false, null);
		Option imageFormat = new Option("imageFormat", false, null);
		Option userAgent = new Option("userAgent", false, null);
		
		outputDir.setArgs(1);
		imageFormat.setArgs(1);
		userAgent.setArgs(1);
		
		options.addOption(outputDir);
		options.addOption(imageFormat);
		options.addOption(userAgent);
	}
}
