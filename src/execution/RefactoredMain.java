package execution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
	
	public static void main(String[] args) {
		topUrls = Collections.synchronizedSet(new HashSet<>());
		urls = Collections.synchronizedSet(new HashSet<>());
		sources = Collections.synchronizedSet(new HashSet<>());
		options = new Options();
		
		createOptions();
		
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
		    		
		    		downloadImage(subUrl, src);
		    		
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
	
	private static void downloadImage(String parentUrl, String source) {
		int questionMarkIndex = source.indexOf("?");
		if (questionMarkIndex != -1)
			source = source.substring(0, questionMarkIndex);
		
		try{ 	         
	         URL url = new URL(parentUrl);
	         try (InputStream inputStream = url.openStream(); 
	        	  OutputStream outputStream = new FileOutputStream(source)) {
	        	 byte[] buffer = new byte[2048];
		         
		         int length = 0;
		         
		         while ((length = inputStream.read(buffer)) != -1) 
		            outputStream.write(buffer, 0, length);
	         }
	      } catch(Exception e) {
	         e.printStackTrace();
	      }
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
		
		//Each option accepts a single argument
		outputDir.setArgs(1);
		imageFormat.setArgs(1);
		userAgent.setArgs(1);
		
		options.addOption(outputDir);
		options.addOption(imageFormat);
		options.addOption(userAgent);
	}
}
