package org.knoesis.rdf.sp.converter;

import org.junit.Before;
import org.junit.Test;
import org.knoesis.rdf.sp.converter.NanoPub2SP;

public class NanoPub2SPTest {

	NanoPub2SP con1 = null;
	NanoPub2SP con2 = null;
	@Before
	public void setUp() throws Exception {
		con1 = new NanoPub2SP();
		con2 = new NanoPub2SP(10, "crc_", "___");
	}

	@Test
	public void testTransform() {
	}

	@Test
	public void testConvert() {
		con1.convert("src/test/resources/test-nano", "nt", "nano");
		con1.convert("src/test/resources/test-nano", "ttl", "nano");
	}

	@Test
	public void testConvertFile() {
		con1.convert("src/test/resources/test-file/test2_nano.nq", "ttl", "nano");
		con1.convert("src/test/resources/test-file/test2_nano.nq", "nt", "nano");
	}

	@Test
	public void testGenFileOut() {
	}

}