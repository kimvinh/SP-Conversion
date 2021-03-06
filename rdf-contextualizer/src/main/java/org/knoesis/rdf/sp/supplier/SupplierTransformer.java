package org.knoesis.rdf.sp.supplier;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.knoesis.rdf.sp.exception.SPException;
import org.knoesis.rdf.sp.model.PrefixTrie;
import org.knoesis.rdf.sp.model.SPNode;
import org.knoesis.rdf.sp.model.SPTriple;
import org.knoesis.rdf.sp.parser.ParserElement;
import org.knoesis.rdf.sp.parser.Reporter;
import org.knoesis.rdf.sp.pipeline.PipedNodesStream;
import org.knoesis.rdf.sp.pipeline.PipedSPTripleIterator;
import org.knoesis.rdf.sp.utils.Constants;
import org.knoesis.rdf.sp.utils.RDFWriteUtils;

import com.romix.scala.collection.concurrent.TrieMap;

public class SupplierTransformer implements Supplier<ParserElement>{
	PipedNodesStream transformerInputStream;
    PipedSPTripleIterator converterIter;
	Reporter reporter;
	ParserElement element;
	TrieMap<String,String> prefixMapping = RDFWriteUtils.prefixMapping;
	PrefixTrie trie = RDFWriteUtils.trie;
	long NS = 0;

    public SupplierTransformer(PipedNodesStream transformerInputStream,
    		PipedSPTripleIterator converterIter, ParserElement element, Reporter reporter) {
		super();
		this.transformerInputStream = transformerInputStream;
		this.converterIter = converterIter;
		this.reporter = reporter;
		this.element = element;
    }
    public SupplierTransformer(){
		prefixMapping.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
		prefixMapping.add("http://www.w3.org/2002/07/owl#", "owl");
		prefixMapping.add("http://www.w3.org/2000/01/rdf-schema#", "rdfs");
		prefixMapping.add("http://www.w3.org/ns/prov#", "prov");
		prefixMapping.add("http://www.w3.org/2001/XMLSchema#", "xsd");
    }
	@Override
    public ParserElement get() {
		long start = System.currentTimeMillis();
		reporter.reportStartStatus(element, Constants.PROCESSING_STEP_TRANFORM);
		
        transformerInputStream.start();
        if (reporter.getExt().equals(Constants.TURTLE_EXT)){
    		Iterator<Entry<String, String>> it = prefixMapping.entrySet().iterator();
    		while (it.hasNext()) {
    		    Map.Entry<String,String> pair = (Map.Entry<String,String>)it.next();
    		    ((PipedNodesStream) transformerInputStream).node("@prefix " + pair.getValue() + ": <" + pair.getKey() + "> . \n");
    		}
        }
        try{
	        String node;
	        SPTriple sptriple;
			while (converterIter.hasNext()){
			// Transform the SPTriple to string node
				Object obj = converterIter.next();
				if (obj instanceof SPTriple){
					sptriple = (SPTriple) obj;
					if (reporter.getExt().equals(Constants.NTRIPLE_EXT))
						node = this.printSPTriple2NT(sptriple);
					else 
						node = this.printSPTriple2N3(sptriple, reporter.isShortenURI());
					if (node != null) ((PipedNodesStream) transformerInputStream).node(node);
				}
			}
			
			reporter.reportSystem(start, element, Constants.PROCESSING_STEP_TRANFORM);
        	transformerInputStream.finish();
        	converterIter.close();
       } catch (Exception e){
			throw new SPException("File " + element.getFilein() + ": encounter streaming exception in transformer", e);
		} 
        return element;
    }
	
	StringBuilder curPrefix = new StringBuilder();
	
	public String toN3(SPNode node){
		StringBuilder str = new StringBuilder();
		if (node.getJenaNode().isURI()){
			String prefix;
			if (!prefixMapping.containsKey(node.getNodePrefix())){
				prefix = "ns" + NS;
				appendPrefix(NS, node.getNodePrefix());
				NS++;
				prefixMapping.put(node.getNodePrefix(), prefix);
			} else {
				prefix = prefixMapping.get(node.getNodePrefix());
			}
			str.append(prefix);
			str.append(":");
			str.append(node.getNodeSuffix());
			return str.toString();
		} else if (node.getJenaNode().isLiteral()){
			String prefix = prefixMapping.get(node.getDatatypePrefix());
			if (prefix == null){
				prefix = "ns" + NS;
				appendPrefix(NS, node.getDatatypePrefix());
				NS++;
				prefixMapping.put(node.getNodePrefix(), prefix);
			}
			str.append("\"");
			str.append(node.getJenaNode().getLiteralValue());
			str.append("\"^^");
			str.append(prefix);
			str.append(':');
			str.append(node.getDatatypeSuffix());
		}
		return node.toString();
	}
	
	public void appendPrefix(long ns, String namespace){
		curPrefix.append("@prefix ns");
		curPrefix.append(ns);
		curPrefix.append(": <");
		curPrefix.append(namespace);
		curPrefix.append("> . \n");
	}
	
	public String printSPTriple2N3(SPTriple triple, boolean shortenURI){
		if (triple == null) return "";
		
		StringBuilder prefix = new StringBuilder();
		StringBuilder triples = new StringBuilder();
		// Singleton instance
		if (triple.getSingletonInstanceTriples().size() > 0) {
			triples.append(triple.getPredicate().toN3(prefixMapping, trie, shortenURI).getShorten());
			triples.append("\n\trdf:singletonPropertyOf\t ");
			List<SPTriple> lst = triple.getSingletonInstanceTriples();
			triples.append(lst.get(0).getObject().toN3(prefixMapping, trie, shortenURI).getShorten());
			if (lst.size() > 1){
				for (int i = 1; i < lst.size(); i++){
					triples.append(", ");
					triples.append(lst.get(i).getObject().toN3(prefixMapping, trie, shortenURI).getShorten());
				}
			} 
			if (triple.getMetaTriples().size() == 0)
				triples.append(" . \n");
		}
		if (triple.getMetaTriples().size() > 0) {
			List<SPTriple> lst = triple.getMetaTriples();
			for (int i = 0; i < lst.size(); i++){
				triples.append("\t; \n \t");
				triples.append(lst.get(i).getPredicate().toN3(prefixMapping, trie, shortenURI).getShorten());
				triples.append("\t");
				triples.append(lst.get(i).getObject().toN3(prefixMapping, trie, shortenURI).getShorten());
			}
			triples.append(" . \n");
		}
		// Inferred generic property triple
		triples.append(triple.getSubject().toN3(prefixMapping, trie, shortenURI).getShorten());
		triples.append("\t");
		triples.append(triple.getPredicate().toN3(prefixMapping, trie, shortenURI).getShorten());
		triples.append("\t");
		triples.append(triple.getObject().toN3(prefixMapping, trie, shortenURI).getShorten());
		// Meta property triple
		if (triple.getGenericPropertyTriples().size() > 0) {
			List<SPTriple> lst = triple.getGenericPropertyTriples();
			for (int i = 0; i < lst.size(); i++){
				triples.append("\t;\n \t");
				triples.append(lst.get(i).getPredicate().toN3(prefixMapping, trie, shortenURI).getShorten());
				triples.append("\t");
				triples.append(lst.get(i).getObject().toN3(prefixMapping, trie, shortenURI).getShorten());
			}
		}
		triples.append("\t . \n");
		prefix.append(triples);
		curPrefix = new StringBuilder();
		return prefix.toString();
	}

	public String printSPTriple2NT(SPTriple triple){
		if (triple == null) return "";
		
		StringBuilder triples = new StringBuilder();
		// Singleton instance
		if (triple.getSingletonInstanceTriples().size() > 0) {
			List<SPTriple> lst = triple.getSingletonInstanceTriples();
			for (SPTriple t : lst){
				triples.append(t.printTriple2NT());
			}
		}
		
		if (triple.getMetaTriples().size() > 0) {
			List<SPTriple> lst = triple.getMetaTriples();
			for (SPTriple t : lst){
				triples.append(t.printTriple2NT());
			}
		}
		triples.append(triple.printTriple2NT());
		// Meta property triple
		if (triple.getGenericPropertyTriples().size() > 0) {
			List<SPTriple> lst = triple.getGenericPropertyTriples();
			for (SPTriple t : lst){
				triples.append(t.printTriple2NT());
			}
		}
		return triples.toString();
	}

	public PipedRDFStream<String> getTransformerInputStream() {
		return transformerInputStream;
	}

	public void setTransformerInputStream(
			PipedNodesStream transformerInputStream) {
		this.transformerInputStream = transformerInputStream;
	}

	public PipedRDFIterator<SPTriple> getConverterIter() {
		return converterIter;
	}

	public void setConverterIter(PipedSPTripleIterator converterIter) {
		this.converterIter = converterIter;
	}

}
