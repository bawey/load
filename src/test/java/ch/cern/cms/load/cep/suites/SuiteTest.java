package ch.cern.cms.load.cep.suites;

import ch.cern.cms.load.EventProcessor;
import ch.cern.cms.load.Load;
import ch.cern.cms.load.FieldTypeResolver;
import ch.cern.cms.load.hwdb.HwInfo;
import ch.cern.cms.load.taps.flashlist.OfflineFlashlistEventsTap;

public class SuiteTest {
	
//	private ExpertController ec;
//	private EventProcessor ep;
//	
//	public void setUp() {
//		super.setUp();
//		try {
//			ec = ExpertController.getInstance();
//			ec.getResolver().setFieldType("deltaT", Double.class);
//			ec.getResolver().setFieldType("deltaN", Double.class);
//			ec.getResolver().setFieldType("fifoAlmostFullCnt", Long.class);
//			ec.getResolver().setFieldType("fractionBusy", Double.class);
//			ec.getResolver().setFieldType("fractionWarning", Double.class);
//			ec.getResolver().setFieldType("clockCount", Double.class);
//			ec.getResolver().setFieldType("linkNumber", Integer.class);
//			ec.getResolver().setFieldType("slotNumber", Integer.class);
//			ec.getResolver().setFieldType("geoslot", Integer.class);
//			ec.getResolver().setFieldType("io", Integer.class);
//			ep = ec.getEventProcessor();
//			ep.getConfiguration().addImport(HwInfo.class.getName());
//			FieldTypeResolver ftr = ec.getResolver();
//			tap = new OfflineFlashlistEventsTap(ec, "/home/bawey/Desktop/flashlists/41/");
//			((OfflineFlashlistEventsTap) tap).setPace(pace);
//			ec.registerTap(tap);
//			createConclusionStreams(ep);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
}
