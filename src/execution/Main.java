package execution;

import java.io.IOException;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
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

class Main {
	public static Options options;
	public static HttpClient client;
	
	public static Set<String> topUrls;
	public static BlockingQueue<String> subUrls;
	public static Set<String> visitedUrls;
	public static Set<String> sources;
	public static Set<String> downloadedSources;
	
	public static AtomicInteger imgCounter;
	public static AtomicInteger urlCounter;
	
	private static ExecutorService threadPool;
	
	public static void main(String[] args)  {
		options = new Options();
		createOptions();
		
		try {
			parseOptions(args);
		} catch (ParseException e) {
			System.out.println("Error: " + e.getMessage());
		}
		
		client = HttpClient.newHttpClient();
		
		topUrls = Collections.synchronizedSet(new HashSet<>());
		subUrls = new LinkedBlockingQueue<>();
		visitedUrls = Collections.synchronizedSet(new HashSet<>());
		sources = Collections.synchronizedSet(new HashSet<>());
		downloadedSources = Collections.synchronizedSet(new HashSet<>());
		
		imgCounter = new AtomicInteger(1);
		urlCounter = new AtomicInteger(0);
		
		for (String str : args) {
			if (validateUrl(str)) {
				topUrls.add(str);
				try {
					crawlPage(str);
				} catch (URISyntaxException | IOException | InterruptedException e) {
					//TODO: Notify user
				}
			}
		}
		
		threadPool = Executors.newFixedThreadPool(20);
		AtomicInteger runningTasks = new AtomicInteger(0);
		do {
			System.out.println(topUrls.size());
			threadPool.submit(new Runnable() {
				public void run() {
					runningTasks.incrementAndGet();
					System.out.println(runningTasks + " AAAAAAAAAAA");
					String url = null;
					try {
						do {
							url = subUrls.take();							
						} while (!isSubUrl(url) || visitedUrls.contains(url));

						System.out.println("Crawling: " + url);
						visitedUrls.add(url);
						crawlPage(url);
						urlCounter.incrementAndGet();
					} catch (URISyntaxException | IOException | InterruptedException e) {
						System.out.println("Error: Could not crawl " + url);
					} finally {
						runningTasks.decrementAndGet();
						System.out.println(runningTasks + " BBBBBBBB");
					}
				}
			});
		} while (runningTasks.get() > 0);
		
		System.out.println("\n\n\n\n");
		for (String url : visitedUrls) {
			System.out.println(url);
		}
		
		System.out.println("SHUTTING DOWN");
		System.out.println("TOTAL URLS: " + urlCounter);
	}
	
	private static boolean validateUrl(String url) {
		try {
			new URL(url).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	private static boolean isSubUrl(String url) {
		for (String topUrl : topUrls)
			if (url.indexOf(topUrl) == 0)
				return true;
		return false;
	}
	
	private static void crawlPage(String url) throws URISyntaxException, IOException, InterruptedException  {
		Document doc = Jsoup.parse(getHtml(url));
		
		Elements links = doc.select("a[href]");
		
		for (Element link : links) {
			String subUrl = link.attr("abs:href");
			
			if (subUrl.lastIndexOf("#") == -1 && isSubUrl(subUrl))
				subUrls.add(subUrl);				
		}
		
	}
	
	private static String getHtml(String url) throws URISyntaxException, IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(url))
				.GET()
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	
	//TODO: Add functionality
	private static void parseOptions(String[] args) throws ParseException {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
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