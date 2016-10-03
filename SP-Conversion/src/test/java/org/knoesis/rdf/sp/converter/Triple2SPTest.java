package org.knoesis.rdf.sp.converter;

import org.junit.Before;
import org.junit.Test;
import org.knoesis.rdf.sp.converter.Triple2SP;

public class Triple2SPTest {

	Triple2SP con1 = null;
	Triple2SP con2 = null;
	@Before
	public void setUp() throws Exception {
		con1 = new Triple2SP();
		con2 = new Triple2SP(10, "crc_", "___", "http://knoesis.org/rdf:singletonPropertyOf");
	}

	@Test
	public void testTransform() {
	}

	@Test
	public void testConvert() {
		con1.convert("src/test/resources/test-triple", "ttl", "triple");
		con1.convert("src/test/resources/test-triple", "nt", "triple");
	}

	@Test
	public void testConvertFile() {
		con1.convert("src/test/resources/test-file/test2_triple.nt", "ttl", "triple");
		con1.convert("src/test/resources/test-file/test2_triple.nt", "nt", "triple");
	}

}