package org.knoesis.rdf.sp.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.knoesis.rdf.sp.converter.ContextualRepresentationConverter;
import org.knoesis.rdf.sp.converter.NamedGraph2SP;
import org.knoesis.rdf.sp.converter.NanoPub2SP;
import org.knoesis.rdf.sp.converter.Reification2SP;
import org.knoesis.rdf.sp.converter.Triple2SP;
import org.knoesis.rdf.sp.inference.ContextualInference;
import org.knoesis.rdf.sp.model.SPTriple;
import org.knoesis.rdf.sp.utils.Constants;
import org.knoesis.rdf.sp.utils.RDFWriteUtils;

public class SPProcessor{

	protected String ext;
	protected String rep;
	protected boolean isInfer;
	protected String ontoDir;
	protected long start;
	protected String dsName;
	protected boolean shortenURI = true;
	protected String prefix;
	
	// For all already-printed prefixes and namespaces
	static Map<String,String> prefixMapping = new ConcurrentHashMap<String,String>();
	// For all possible prefixes and namespaces
	static Map<String,String> trie = new ConcurrentHashMap<String,String>();
	ContextualInference reasoner = new ContextualInference();
	NamedGraph2SP converterNG2SP;
	Reification2SP converterRei2SP;
	NanoPub2SP converterNano2SP;
	Triple2SP converterTriple2SP;
	ContextualRepresentationConverter converter;
	
	public SPProcessor(String _rep){
		rep = _rep;
		converterNG2SP = new NamedGraph2SP();
		converterRei2SP = new Reification2SP();
		converterNano2SP = new NanoPub2SP();
		converterTriple2SP = new Triple2SP();
		converter = new ContextualRepresentationConverter();
	}

	public SPProcessor(String _rep, long uuidInitNum, String uuidInitStr) {
		rep = _rep;
		converterNG2SP = new NamedGraph2SP(uuidInitNum, uuidInitStr);
		converterRei2SP = new Reification2SP(uuidInitNum, uuidInitStr);
		converterNano2SP = new NanoPub2SP(uuidInitNum, uuidInitStr);
		converterTriple2SP = new Triple2SP(uuidInitNum, uuidInitStr);
		converter = new ContextualRepresentationConverter(uuidInitNum, uuidInitStr);
	}
	
	public void start(){
		start = System.currentTimeMillis();
	}
	
	public SPTriple process(Quad quad){
//		System.out.println("Processing quad: " + quad.toString());
		if (isInfer){
			// infer new triples and add them to the list
			return reasoner.infer(convert(quad));
		} else {
			return convert(quad);
		}
	}
	
	public SPTriple process(Triple triple){
//		System.out.println("Processing quad: " + quad.toString());
		if (isInfer){
			// infer new triples and add them to the list
			return reasoner.infer(convert(triple));
		} else {
			return convert(triple);
		}

	}
	
	public String processQuad(Quad quad){
		// Write the credentials
//			System.out.println("Processing quad: " + quad.toString());
		List<SPTriple> triples = new ArrayList<SPTriple>();
		if (isInfer){
			// infer new triples and add them to the list
			triples.add(reasoner.infer(convert(quad)));
		} else {
			triples.add(convert(quad));
		}
		return RDFWriteUtils.printTriples(triples, prefixMapping, trie, ext, this.isShortenURI());
	}
	
	public String processTriple(Triple triple){
//		System.out.println("Processing quad: " + quad.toString());
		List<SPTriple> triples = new ArrayList<SPTriple>();
		if (isInfer){
			// infer new triples and add them to the list
			triples.add(reasoner.infer(convert(triple)));
		} else {
			triples.add(convert(triple));
		}
		return RDFWriteUtils.printTriples(triples, prefixMapping, trie, ext, this.shortenURI);

	}
	
	protected SPTriple convert(Quad quad){
		SPTriple sptriple = null;
		switch (rep.toUpperCase()){
		
		case Constants.NANO_REP:
			sptriple = converterNano2SP.transformQuad(quad);
			break;
		case Constants.NG_REP:
			sptriple = converterNG2SP.transformQuad(quad);
			break;
		default:
			sptriple = converterNG2SP.transformQuad(quad);
			break;
		}
		return sptriple;

	}
	protected SPTriple convert(Triple triple){
		SPTriple sptriple = null;
		switch (rep.toUpperCase()){
		
		case Constants.REI_REP:
			sptriple = converterRei2SP.transformTriple(triple);
			break;
		case Constants.TRIPLE_REP:
			sptriple = converterTriple2SP.transformTriple(triple);
			break;
		case Constants.NONE_REP:
			sptriple = converter.transformTriple(triple);
			break;
		default:
			sptriple = converterTriple2SP.transformTriple(triple);
			break;
		}
		return sptriple;

	}
	
	
	public List<SPTriple> finish(){
		List<SPTriple> triples = new ArrayList<SPTriple>();
 		if (isInfer) {
       		// Generate the generic property triples
   			triples.addAll(reasoner.generateGenericPropertyTriplesPerFile());
   			return triples;
   		}
 		return null;
	}
	
	public ContextualInference getReasoner() {
		return reasoner;
	}

	public void setReasoner(ContextualInference reasoner) {
		this.reasoner = reasoner;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
	}

	public String getRep() {
		return rep;
	}

	public void setRep(String rep) {
		this.rep = rep;
	}

	public boolean isIsInfer() {
		return isInfer;
	}

	public void setIsinfer(boolean _isInfer) {
		this.isInfer = _isInfer;
	}

	public String getDsName() {
		return dsName;
	}

	public void setDsName(String dsName) {
		this.dsName = dsName;
	}

	public String getOntoDir() {
		return ontoDir;
	}

	public void setOntoDir(String ontoDir) {
		this.ontoDir = ontoDir;
	}

	public boolean isInfer() {
		return isInfer;
	}

	public void setInfer(boolean isInfer) {
		this.isInfer = isInfer;
	}

	public boolean isShortenURI() {
		return shortenURI;
	}

	public void setShortenURI(boolean shortenURI) {
		this.shortenURI = shortenURI;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}


}
