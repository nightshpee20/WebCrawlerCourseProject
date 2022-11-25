package execution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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
	public static BlockingQueue<String> subUrlsBQ;
	public static Set<String> subUrlsSet;
	public static Set<String> visitedUrls;
//	public static Set<String> sources;
	public static Set<String> downloadedSources;
	
	public static AtomicInteger imgCounter;
	public static AtomicInteger urlCounter;
	private static AtomicInteger runningTasks;
	
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
		subUrlsBQ = new LinkedBlockingQueue<>();
		subUrlsSet = Collections.synchronizedSet(new HashSet<>());
		visitedUrls = Collections.synchronizedSet(new HashSet<>());
//		sources = Collections.synchronizedSet(new HashSet<>());
		downloadedSources = Collections.synchronizedSet(new HashSet<>());
		
		imgCounter = new AtomicInteger(1);
		urlCounter = new AtomicInteger(0);
		runningTasks = new AtomicInteger(0);
		
		threadPool = Executors.newFixedThreadPool(10);

		//Crawl topUrls and fill the threadPool with tasks.
		for (String str : args) {
			if (validateUrl(str) && !topUrls.contains(str)) {
				topUrls.add(str);
				try {
					System.out.println("**CRAWLING PAGE: " + str);
					crawlPage(str);
					urlCounter.incrementAndGet();
				} catch (URISyntaxException | IOException | InterruptedException e) {
					//TODO: Notify user
				}
			}
		}
		
		do {
			if (runningTasks.get() == 10)
				continue;
			Runnable task = new Runnable() {
				public void run() {	
					
					String url = null;
					try {
						do {
							url = subUrlsBQ.take();
						} while (visitedUrls.contains(url));
						System.out.println("######CRAWLING PAGE: " + url);
						runningTasks.incrementAndGet();
						crawlPage(url);
						runningTasks.decrementAndGet();
					} catch (URISyntaxException | IOException | InterruptedException e) {}
					if (url != null) 
						urlCounter.incrementAndGet();
//					System.out.println(url + " " + urlCounter.get());
					
//					System.out.println("doneAA " + runningTasks.get());
//					try { Thread.sleep(1000); } catch (InterruptedException e) {}
					return;
				}
			};
			threadPool.submit(task);
			System.out.println("------Downloaded Images: " + downloadedSources.size() + " ------URLs CRAWLED: " + visitedUrls.size());
			if (subUrlsBQ.size() == 0 && runningTasks.get() == 0) {
				break;
			}
		} while (true);
		
//		System.out.println("\n\n");
//		for (String subUrl : subUrlsBQ)
//			System.out.println(subUrl);
		
		System.out.println("\n\n");
		for (String url : visitedUrls) {
			System.out.println(url);
		}
		System.out.println("\n\n");
		
		System.out.println("SHUTTING DOWN");
		while (true) {
			if (runningTasks.get() == 0) {
				threadPool.shutdownNow();
				System.out.println(runningTasks + " runningTasks");
				break;
			}
				
		}
		System.out.println("TOTAL URLS: " + urlCounter);
		System.out.println(threadPool.isTerminated());
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
			
			if (isSubUrl(subUrl) && !subUrlsSet.contains(subUrl) && subUrl.lastIndexOf("#") == -1) {
				subUrlsBQ.add(subUrl);
				subUrlsSet.add(subUrl);								
			}
		}

//		System.out.println("downloadImages(doc)");
		downloadImages(doc);
		
		visitedUrls.add(url);
	}
	
	private static void downloadImages(Document doc) {
		Elements imageSources = doc.select("img[src]");
		
		for (Element imageSource : imageSources) {
			String source = imageSource.attr("abs:src");
			
//			System.out.println("validateUrl(source)");
			if (validateUrl(source) && !downloadedSources.contains(source)) {
//				System.out.println("DOWNLOAD START: " + imgCounter.get());
//				System.out.println("SOURCE: " + source);
				URI uri = null;
				try { uri = new URI(source); } catch (URISyntaxException e) {} //TODO: fix formatting
				HttpRequest request = HttpRequest.newBuilder()
									             .uri(uri)
									             .GET()
									             .build();
				HttpResponse<byte[]> response = null;
				try {
					response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
				} catch (IOException | InterruptedException e) {}
				
				String fileType = response.headers().firstValue("Content-Type").get();
				int backslashIndex = fileType.indexOf("/");
				fileType = fileType.substring(backslashIndex + 1, fileType.length());
//				System.out.println("DOWNLOAD SIZE: " + response.body().length);
				String path = String.format("C:\\Users\\night\\Desktop\\Java Internship\\Web Crawler\\img\\image%d.%s", System.currentTimeMillis(), fileType);
//				System.out.println("DOWNLOAD NAME: " + path);
				File newImg = new File(path);
				try { newImg.createNewFile(); } catch (IOException e) {}
				try { Files.write(Path.of(newImg.getAbsolutePath()), response.body());} catch (IOException e) {}
				downloadedSources.add(source);
//				System.out.println("DOWNLOAD END: " + (imgCounter.get() - 1));
//				System.out.println("\n-----------------\n");
			}
		}
	}
	
	private static boolean validateUrl(String url) {
		try {
			new URL(url).toURI();
			return true;
		} catch (Exception e) {
			return false;
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


/*

Path path = Paths.get(String.format("image%d.%s", imgCounter.getAndIncrement(), fileType));
				

threadPool.submit(new Runnable() {
					public void run() {
						runningTasks.incrementAndGet();
						String url = null;
						try {
							do {
								url = subUrls.take();							
							} while (visitedUrls.contains(url));
							//!isSubUrl(url) || 
							System.out.println("Crawling: " + url);
							visitedUrls.add(url);
							crawlPage(url);
							urlCounter.incrementAndGet();
						} catch (URISyntaxException | IOException | InterruptedException e) {
							System.out.println("Error: Could not crawl " + url);
						} finally {
							runningTasks.decrementAndGet();
						}
					}
				});
*/