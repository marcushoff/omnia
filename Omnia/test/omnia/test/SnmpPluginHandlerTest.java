package omnia.test;

import org.junit.*;
import static org.junit.Assert.*;
import omnia.snmp.SnmpPluginHandler;
import org.snmp4j.smi.OID;

/**
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class SnmpPluginHandlerTest {
    SnmpPluginHandler PH;

    public SnmpPluginHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        PH = new SnmpPluginHandler();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetDefaultOIDs() {
               System.out.println("Test if getDefaultOIDs returns array of OID objects.");
        OID[] oids = PH.getDefaultOids();
        try {
            for(int i = 0; i < oids.length; i++) {
                assertTrue(oids[i] instanceof OID);
            }
        }
        catch (AssertionError ae) {
            System.out.println("getDefaultOIDs does not return array of OID");
        }


    }
}
