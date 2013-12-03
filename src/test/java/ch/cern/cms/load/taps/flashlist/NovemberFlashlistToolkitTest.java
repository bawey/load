package ch.cern.cms.load.taps.flashlist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NovemberFlashlistToolkitTest {

	private final static Logger logger = Logger.getLogger(NovemberFlashlistToolkitTest.class);

	public static final String problem = "\"Out\",\"\",\"0.0\",\"\",\"690&0%691&0%692&0%\",\"N/A\",\"\",\"\",\"http://cmsrc-castor.cms:25000/rcms/gui/servlet/RunningConfigurationServlet\",\"LOCAL\",\"Out\",\"\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"750&0%751&10%752&0%753&10%754&0%755&10%756&0%757&10%760&0%831&5%832&5%833&5%834&5%835&5%836&5%837&5%838&5%839&5%841&5%842&5%843&5%844&5%845&5%846&5%847&5%848&5%849&5%851&5%852&5%853&5%854&5%855&5%856&5%857&5%858&5%859&5%861&5%862&5%863&5%864&5%865&5%866&5%867&5%868&5%869&5%890&5%891&5%892&5%893&5%894&5%895&5%896&5%897&5%898&5%899&5%900&5%901&5%\",\"N/A\",\"\",\"\",\"http://cmsrc-csc.cms:12000/rcms/gui/servlet/RunningConfigurationServlet\",\"In\",\" \",\"0.0\",\" \",\"\",\"TIER0_TRANSFER_ON\",\"[\"TIER0_TRANSFER_ON\", \"TIER0_TRANSFER_OFF\"]\",\"Running\",\"http://cmsrc-daq.cms:11000/rcms/gui/servlet/RunningConfigurationServlet\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"In\",\"Running\",\"0.0\",\"\",\"\",\"cosmic_run\",\"[\"pp_run\",\"cosmic_run\",\"hi_run\",\"hpu_run\"]\",\"Running\",\"http://cmsrc-dqm.cms:22000/rcms/gui/servlet/RunningConfigurationServlet\",\"In\",\" \",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\" \",\"770&0%771&0%772&3%773&3%774&3%780&3%\",\"N/A\",\"\",\"Running\",\"http://cmsrc-dt.cms:13000/rcms/gui/servlet/RunningConfigurationServlet\",\"In\",\"\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"601&3%602&3%603&3%604&3%605&3%606&3%607&3%608&3%609&3%610&3%611&3%612&3%613&3%614&3%615&3%616&3%617&3%618&3%619&3%620&3%621&3%622&3%623&3%624&3%625&3%626&3%627&3%628&0%629&0%630&0%631&0%632&0%633&0%634&0%635&0%636&0%637&0%638&0%639&0%640&0%641&0%642&0%643&0%644&0%645&0%646&0%647&0%648&0%649&0%650&0%651&0%652&0%653&0%654&0%661&7%662&7%663&5%664&5%\",\":SelectiveReadout\",\"[\":SelectiveReadout\",\":SelectiveReadout_test\", \":SelectiveReadout-NoLaser\", \":ZeroSuppression\", \":ZeroSuppression-NoLaser\", \":Beam-SR-HighLaser\", \"Beam-NoTrigger\",\":FullReadout\",\":FullReadout-NoLaser\",\":TPGPatternGenerator\", \":TCC_Patterns\", \":TCC_Patterns_LTC\", \":DelayScan-SR\"]\",\"Running\",\"http://cmsrc-ecal.cms:15000/rcms/gui/servlet/RunningConfigurationServlet\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"Out\",\"\",\"0.0\",\"\",\"520&0%522&0%523&0%524&0%525&0%529&0%530&0%531&0%532&0%534&0%535&0%537&0%539&0%540&0%541&0%542&0%545&0%546&0%547&0%548&0%549&0%551&0%553&0%554&0%555&0%556&0%557&0%560&0%561&0%563&0%564&0%565&0%566&0%568&0%570&0%571&0%572&0%573&0%574&0%\",\"N/A\",\"[\"GR_Phys:HighGain\",\"GR_Phys:LowGain\"]\",\"\",\"http://cmsrc-es.cms:21000/rcms/gui/servlet/RunningConfigurationServlet\",\"http://cmsrc-top.cms:10000/urn:rcms-fm:fullpath=/toppro/PublicGlobal/levelZeroFM,group=levelZeroFM,owner=toppro\",\"In\",\"__________><)))'>_____________________________________________________________\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"700&3%701&3%702&3%703&3%704&3%705&3%706&3%707&3%708&3%709&3%710&3%711&3%712&3%713&3%714&3%715&3%716&3%717&3%724&3%725&3%726&3%727&3%728&3%729&3%730&3%731&3%\",\"ZS\",\"[\"noZS\",\"ZS\",\"test-ZS\"]\",\"Running\",\"http://cmsrc-hcal.cms:16000/rcms/gui/servlet/RunningConfigurationServlet\",\"Out\",\"\",\"0.0\",\"N/A\",\"\",\"718&0%719&0%720&0%721&0%722&0%723&0%\",\"N/A\",\"[\"noZS\",\"ZS\",\"test-ZS\"]\",\"\",\"http://cmsrc-hcal.cms:16000/rcms/gui/servlet/RunningConfigurationServlet\",\"cosmics\",\"l1_hlt_cosmics/v324\",\"L1_20131104_165149_6433\",\"N/A\",\"N/A\",\"N/A\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"Out\",\"\",\"0.0\",\"\",\"0&0%1&0%2&0%3&0%4&0%5&0%6&0%7&0%8&0%9&0%10&0%11&0%12&0%13&0%14&0%15&0%16&0%17&0%18&0%19&0%20&0%21&0%22&0%23&0%24&0%25&0%26&0%27&0%28&0%29&0%30&0%31&0%32&0%33&0%34&0%35&0%36&0%37&0%38&0%39&0%\",\"N/A\",\"\",\"\",\"http://cmsrc-pixel.cms:17000/rcms/gui/servlet/RunningConfigurationServlet\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"N/A\",\"Out\",\"\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"790&10%791&10%792&10%793&5%\",\"N/A\",\"\",\"\",\"http://cmsrc-rpc.cms:14000/rcms/gui/servlet/RunningConfigurationServlet\",\"216163\",\"In\",\"Running.\",\"0.0\",\"\",\"735&3%\",\"N/A\",\"\",\"Running\",\"http://cmsrc-scal.cms:24000/rcms/gui/servlet/RunningConfigurationServlet\",\"225797\",\"Running\",\"2013-11-05T09:09:50.000850Z\",\"In\",\"Running\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"50&3%51&3%52&3%53&3%54&3%55&3%56&3%57&3%58&3%60&3%61&3%62&3%63&3%64&3%65&3%66&3%67&3%68&3%69&3%70&3%71&3%72&3%73&3%74&3%75&3%76&3%77&3%78&3%79&3%80&3%81&3%82&3%83&3%84&3%85&3%86&3%87&3%88&3%89&3%90&3%91&3%92&3%93&3%94&3%95&3%96&3%97&3%98&3%99&3%100&3%101&3%102&3%104&3%105&3%106&3%107&3%108&3%109&3%110&3%111&3%112&3%113&3%114&3%115&3%116&3%117&3%118&3%119&3%120&3%121&3%122&3%123&3%124&3%125&3%126&3%127&3%128&3%129&3%130&3%131&3%132&3%133&3%134&3%135&3%136&3%137&3%138&3%139&3%140&3%141&3%142&3%143&3%144&3%145&3%146&3%147&3%148&3%149&3%150&3%151&3%152&3%153&3%154&3%155&3%156&3%157&3%158&3%159&3%160&3%161&3%162&3%163&3%164&3%165&3%166&3%167&3%168&3%169&3%170&3%171&3%172&3%173&3%174&3%175&3%176&3%177&3%178&3%179&3%180&3%181&3%182&3%183&3%184&3%185&3%186&3%187&3%188&3%189&3%190&3%191&3%192&3%193&3%194&3%195&3%196&3%197&3%198&3%199&3%200&3%201&3%202&3%203&3%204&3%205&3%206&3%207&3%208&3%209&3%210&3%211&3%212&3%213&3%214&3%215&3%216&3%217&3%218&3%219&3%220&3%221&3%222&3%223&3%224&3%225&3%226&3%227&3%228&3%229&3%230&3%231&3%232&3%233&3%234&3%235&3%236&3%237&3%238&3%239&3%240&3%241&3%242&3%243&3%244&3%245&3%246&3%247&3%248&3%249&3%250&3%251&3%252&3%253&3%254&3%255&3%256&3%257&3%258&3%259&3%260&3%261&3%262&3%263&3%264&3%265&3%266&3%267&3%268&3%269&3%270&3%271&3%272&3%273&3%274&3%275&3%276&3%277&3%278&3%279&3%280&3%281&3%282&3%283&3%284&3%285&3%286&3%287&3%288&3%289&3%290&3%291&3%292&3%293&3%294&3%295&3%296&3%297&3%298&3%299&3%300&3%301&3%302&3%303&3%304&3%305&3%306&3%307&3%308&3%309&3%310&3%311&3%312&3%313&3%314&3%315&3%316&3%317&3%318&3%319&3%320&3%321&3%322&3%323&3%324&3%325&3%326&3%327&3%328&3%329&3%330&3%331&3%332&3%333&3%334&3%335&3%336&3%337&3%338&3%339&3%340&3%341&3%342&3%343&3%344&3%345&3%346&3%347&3%348&3%349&3%350&3%351&3%352&3%353&3%354&3%355&3%356&3%357&3%358&3%359&3%360&3%361&3%362&3%363&3%364&3%365&3%366&3%367&3%368&3%369&3%370&3%371&3%372&3%373&3%374&3%375&3%376&3%377&3%378&3%379&3%380&3%381&3%382&3%383&3%384&3%385&3%386&3%387&3%389&3%390&3%391&3%392&3%393&3%394&3%395&3%396&3%397&3%398&3%399&3%400&3%401&3%402&3%403&3%404&3%405&3%406&3%407&3%408&3%409&3%410&3%411&3%412&3%413&3%414&3%415&3%416&3%417&3%418&3%419&3%420&3%421&3%422&3%423&3%424&3%425&3%426&3%427&3%428&3%429&3%430&3%431&3%432&3%433&3%434&3%435&3%436&3%437&3%438&3%439&3%440&3%441&3%442&3%443&3%444&3%445&3%446&3%447&3%448&3%449&3%450&3%451&3%452&3%453&3%454&3%455&3%456&3%457&3%458&3%459&3%460&3%461&3%462&3%463&3%464&3%465&3%466&3%467&3%468&3%469&3%470&3%471&3%472&3%473&3%474&3%475&3%476&3%477&3%478&3%479&3%480&3%481&3%482&3%483&3%484&3%485&3%486&3%487&3%488&3%489&3%\",\"DEFAULT\",\"[\"VR\",\"ZS\",\"DEFAULT\"]\",\"Running\",\"http://cmsrc-tracker.cms:18000/rcms/gui/servlet/RunningConfigurationServlet\",\"In\",\"Rate: <strong> 1058.2 Hz </strong>  (trgnr=950492  orbit=9886306  interval=5s)\",\"0.0\",\"Away\",\"\",\"0.0\",\"\",\"\",\"N/A\",\"\",\"\",\"\",\"\",\"745&0%810&5%812&11%813&11%\",\"Automatic\",\"[ \"Automatic\", \"ExpertMode\" ]\",\"Running\",\"http://cmsrc-trigger.cms:19000/rcms/gui/servlet/RunningConfigurationServlet\",\"bcref-internal-manual\"";

	private static final String rootDir = "/depot/flashlists13.11_2/";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		String[] tokens = Flashlist.smartSplit(problem);
		System.out.println(tokens.length);
	}

	private void descend(File rootDir) throws Exception {
		for (File f : rootDir.listFiles()) {
			if (f.isDirectory()) {
				descend(f);
			} else {
				testParse(f);
			}
		}
	}

	public void testParse(File f) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(f));
		String[] damnline = null;
		int counter = 0;
		while ((damnline = NovemberFlashlistToolkit.getNextValidRow(br, null)) != null) {
			++counter;
			System.out.println("Line[" + (counter) + "] got " + damnline.length + " tokens");
		}
		br.close();
	}

	@Test
	public void testSameLineFromFileWhereTheTextIsUnescaped() throws Exception {
		File root = new File(rootDir);
		logger.info("Testing 'parsability' for " + rootDir);
		descend(root);
	}

	// returns the timestamp + allthefields.

}
