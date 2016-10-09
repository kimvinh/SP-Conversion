package org.knoesis.rdf.sp.converter;

import org.junit.Before;
import org.junit.Test;
import org.knoesis.rdf.sp.parser.SPParser;
import org.knoesis.rdf.sp.utils.Constants;
import org.knoesis.rdf.sp.utils.ConstantsTest;

public class Reification2SPTest {

	SPParser con1 = null;
	SPParser con2 = null;
	String rep = Constants.REI_REP;
	@Before
	public void setUp() throws Exception {
		con1 = new SPParser(rep);
//		con1.setZip(true);
		con1.setOntoDir(ConstantsTest.test_data_onto);
		con1.setInfer(false);
		con2 = new SPParser(rep, 10, "str1");
//		con2.setZip(true);
		con2.setOntoDir(ConstantsTest.test_data_onto);
		con2.setInfer(true);
		con2.setShortenURI(true);
	}

	@Test
	public void testConvert() {
		con1.parse(ConstantsTest.test_data_dir + "/" + ConstantsTest.test_rei, "nt", rep);
		con2.parse(ConstantsTest.test_data_dir + "/" + ConstantsTest.test_rei, "ttl", rep);
//		con1.parse("src/test/resources/test-file/test2_rei.ttl", "nt", rep);
//		con2.parse("src/test/resources/test-file/test2_rei.ttl", "ttl", rep);
	}

	@Test
	public void testConvertFile() {
	}

}
