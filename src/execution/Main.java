package execution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	public static Set<String> urls = Collections.synchronizedSet(new HashSet<>());
	public static Set<String> sources = Collections.synchronizedSet(new HashSet<>());
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Crawl accepts a single argument!(crawl <url>)");			
			return;
		}
		
		getSubUrls(args[0]);
		urls.forEach((url) -> {
			getSubUrls(url);
		});
		
		urls.forEach((url) -> {
			getImageSources(url);
		});
		
		sources.forEach((source) -> {
			downloadImage(source);
		});
		System.out.println("Done");
	}

	private static void getSubUrls(String url) {
		Document doc = null;
		try {			
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
			return;
		}
		
		Elements links = doc.select("a[href]");
		
		for (Element link : links) {
			String subURL = link.attr("abs:href");
			if (subURL.lastIndexOf("#") == -1 && subURL.lastIndexOf(url) != -1)	{
				urls.add(subURL);
			}
		}
		
		System.out.println("Total subURLs: " + urls.size());
	}
	//TODO: merge with getSubUrls
	private static void getImageSources(String url) {
		Document document = null;
	    
		try {
	    	document = Jsoup.connect(url).get();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		
    	Elements images = document.select("img[src]"); 
    	
    	for (Element image : images) {
    		String src = image.attr("src");
    		
    		if (src.startsWith("//"))
    			src = "https:" + src;
    		
    		sources.put(url, src);
    	}
	}
	
	private static void downloadImage(String parentUrl, String source) {
		System.out.println("Downloading Image From: " + parentUrl);
		
		try{ 	         
	         URL sourceUrl = new URL(source);
//	         System.out.println("Source:" + sourceUrl.toString());
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
}

