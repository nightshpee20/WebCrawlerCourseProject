package utilities;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LinkTree {
	public LinkNode root;
	Set<String> urls;
	
	@SuppressWarnings("static-access")
	public LinkTree() {
		root = null;
		urls = new ConcurrentHashMap<>().newKeySet();
	}
	
	public Set<String> getAllUrls() {
		traverseLinkTree(root);
		return urls;
	}
	
	private void traverseLinkTree(LinkNode node) {
		
	}
}
