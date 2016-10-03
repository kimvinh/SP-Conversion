package org.knoesis.rdf.sp.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.knoesis.rdf.sp.converter.NamedGraph2SP;
import org.knoesis.rdf.sp.utils.RDFWriteUtils;

public class PrefixTrie {
	final static Logger logger = Logger.getLogger(NamedGraph2SP.class);
	private PrefixTrieNode root;

	public PrefixTrie() {
		root = new PrefixTrieNode();
	}

	// Inserts a prefix into the trie.
	
	public void insert(String uri, String prefix) {
		if (uri == null || prefix == null) return;
		HashMap<Character, PrefixTrieNode> children = root.children;

		for (int i = 0; i < uri.length(); i++) {
			char c = uri.charAt(i);

			PrefixTrieNode node;
			if (children.containsKey(c)) {
				node = children.get(c);
			} else {
				node = new PrefixTrieNode(c);
				children.put(c, node);
			}

			children = node.children;

			// set leaf node
			if (i == uri.length() - 1){
				node.isLeaf = true;
				node.prefix = prefix;
				node.isPrefix = true;
			}
			
		}
	}

	// Returns if the uri is a prefix in the trie.
	public String searchPrefix(String uri) {
		PrefixTrieNode node = searchNode(uri);

		if (node != null && node.isLeaf)
			return node.prefix;
		else
			return null;
	}

	// Returns if there is any word in the trie
	// that starts with the given prefix.
	public boolean matchingPrefix(String uri) {
		if (searchNode(uri) == null)
			return false;
		else
			return true;
	}

	public PrefixTrieNode searchNode(String uri) {
		Map<Character, PrefixTrieNode> children = root.children;
		PrefixTrieNode node = null;
		for (int i = 0; i < uri.length(); i++) {
			char c = uri.charAt(i);
			if (children.containsKey(c)) {
				node = children.get(c);
				children = node.children;
			} else {
				return null;
			}
		}
		return node;
	}
	
	/**
	 * Return a triplet of <shortenURI, prefix, namespace>
	 * */
	public SPNode shortenURIWithPrefix(SPNode uri) {

		Map<Character, PrefixTrieNode> children = root.children;
		String ns = null, prefix = null;
		
		PrefixTrieNode curnode = null, latestLeaf = null;
		int i = 0, lastNsInd = 0;
		
		final String uriStr = uri.getJenaNode().toString();
		int len = uriStr.length();
		StringBuilder shorten = new StringBuilder();
		
		for (i = 0; i < len; i++) {
			
			char c = uriStr.charAt(i);
			
			if (children.containsKey(c)) {
				
				curnode = children.get(c);
				children = curnode.children;

				if (curnode.isPrefix) {
					latestLeaf = curnode;
					lastNsInd = i+1;
//					logger.trace("found ns " + curnode.prefix + " at " + c +" with " + uriStr.substring(i, len));
				}
				
			} else {
//				lastNsInd = i;
				break;
			}
		}
		
		int ind = getLastIndexOfDelimiterWithSecondPeriod(uriStr);
//		System.out.println(uriStr + " uri with len=" + len +  " getLastIndexOfDelimiter=" + ind + " vs. lastNsInd=" + lastNsInd );
		
		if (ind >= lastNsInd) {
			latestLeaf = null;
			lastNsInd = ind;
		}
		
		if (latestLeaf != null){
			// Construct the shorted uri
			ns = uriStr.substring(0, lastNsInd);
			prefix = latestLeaf.prefix;
//			logger.trace(uriStr + " has prefix" + prefix);
			shorten.append(prefix + ":");
			shorten.append(normalizeN3(uriStr.substring(lastNsInd, len)));
		} else {
//			logger.trace("not existing prefix" + uriStr);
			// Generating new prefix and ns, insert it to the trie,
//			prefix = RDFWriteUtils.getNextPrefixNs();
			if (lastNsInd > 2 && uriStr.charAt(lastNsInd-1) != '/' && uriStr.charAt(lastNsInd-2) != ':' ) {
				ns = uriStr.substring(0, lastNsInd + 1);
				prefix = searchPrefix(ns);
				if (prefix == null) prefix = RDFWriteUtils.getNextPrefixNs();

				shorten.append(prefix + ":");
				if (!uriStr.substring(lastNsInd+1, len).isEmpty()){
					shorten.append(normalizeN3(uriStr.substring(lastNsInd+1, len)));
				}
			} else {
				ns = uriStr;
				shorten.append(prefix + ":");
				prefix = RDFWriteUtils.getNextPrefixNs();
			}
			System.out.println(uriStr + "\t" + ns + "\t" + prefix + "\t" + shorten.toString());
			insert(ns, prefix);
		}
		uri.setShorten(shorten.toString());
		uri.setNamespace(ns);
		uri.setPrefix(prefix);
		return uri;

	}
	
	public int getLastIndexOfDelimiter(String uri){
		int ind = uri.length()-1;
				
		boolean pastProtocol = false;
		while (ind >=0){
			if (uri.charAt(ind) == '/' || uri.charAt(ind) == '#' || uri.charAt(ind) == ':' || uri.charAt(ind) == '.'){
				if (!pastProtocol) return ind;
			}
			if (uri.charAt(ind) == '/' && uri.charAt(ind-1) == '/' && uri.charAt(ind-2) == ':') pastProtocol = true;
			ind--;
		}
		return ind;
	}
	
	public int getLastIndexOfDelimiterWithSecondPeriod(String uri){
		int ind = uri.length()-1;
		
		// Find the 3rd slash symbol
		int lastSlash = 0, slashCount = 0;
		while (lastSlash < ind & slashCount < 3){
			if (uri.charAt(lastSlash) == '/') slashCount++;
			lastSlash++;
		}
//		System.out.println("last slash of " + uri + " is at: " + lastSlash);
		
		boolean pastProtocol = false;
		boolean foundPeriod = false;
		while (ind >=0){
			if (uri.charAt(ind) == '/' || uri.charAt(ind) == '#' || uri.charAt(ind) == ':'){
				if (!pastProtocol) return ind;
			}
			if (uri.charAt(ind) == '.' && ind >= lastSlash){
				if (foundPeriod) return ind;
				foundPeriod = true;
			}
			if (uri.charAt(ind) == '/' && uri.charAt(ind-1) == '/' && uri.charAt(ind-2) == ':') pastProtocol = true;
			ind--;
		}
		return ind;
	}
	

	public boolean isSPDelimiter(char c){
		return (c == '/' || c == '#' || c == ':' || c == '.');
	}
	
	public String normalizeN3(String in){
		try {
			return URLEncoder.encode(in, "UTF-8").replaceAll("\\.", "%2E");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return in;
	}
	
}