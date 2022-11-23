package execution;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	@SuppressWarnings("static-access")
	public static Set<String> urls = new ConcurrentHashMap<>().newKeySet();
	public static Map<String, String> sources = new ConcurrentHashMap<>();
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Crawl accepts a single argument!(crawl <url>)");			
			return;
		}
		
		Instant i1 = Instant.now();
		getSubUrls(args[0]);
		urls.forEach((url) -> {
			getSubUrls(url);
		});
		Instant i2 = Instant.now();
		Duration dur = Duration.between(i1, i2);
		System.out.println("Total subURLs: " + urls.size() + " Benchmark(sec): " + dur.toSeconds());
		
		i1 = Instant.now();
		urls.forEach((url) -> {
			getImageSources(url);
		});
		i2 = Instant.now();
		dur = Duration.between(i1, i2);
		System.out.println("imgSrcCollector Benchmark(sec): " + dur.toSeconds());
		System.out.println("Sources count: " + sources.size());
		
		i1 = Instant.now();
		sources.forEach((url, source) -> {
			downloadImage(url, source);
		});
		i2 = Instant.now();
		dur = Duration.between(i1, i2);
		System.out.println("imgDownloader Benchmark(sec): " + dur.toSeconds());
		System.out.println("Done");
	}

	private static void getSubUrls(String url) {
		Document doc = null;
		try {			
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
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
	
	private static void getImageSources(String url) {
		Document document = null;
	    
		try {
	    	document = Jsoup.connect(url).get();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
		
    	Elements images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]"); 
    	
    	for (Element image : images) {
    		String src = image.attr("src");
    		
    		if (src.startsWith("//"))
    			src = "https:" + src;
    		
    		sources.put(url, src);
    	}
	}
	
	private static void downloadImage(String parentUrl, String source) {
		try{ 
	         System.out.println("Downloading Image From: " + parentUrl);
	         
	         URL sourceUrl = new URL(source);
	         System.out.println("Source:" + sourceUrl.toString());
	         URL url = new URL(parentUrl);
	         InputStream inputStream = url.openStream();
	         OutputStream outputStream = new FileOutputStream(source);
	         byte[] buffer = new byte[2048];
	         
	         int length = 0;
	         
	         while ((length = inputStream.read(buffer)) != -1) 
	            outputStream.write(buffer, 0, length);
	         
	         inputStream.close();
	         outputStream.close();
	         
	      } catch(Exception e) {
	         e.printStackTrace();
	      }
	}
}

