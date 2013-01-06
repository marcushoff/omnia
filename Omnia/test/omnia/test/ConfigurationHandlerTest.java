package omnia.test;

import java.net.InetAddress;
import omnia.ConfigurationHandler;
import static org.junit.Assert.assertTrue;
import org.junit.*;
import org.snmp4j.mp.SnmpConstants;

/**
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class ConfigurationHandlerTest {

    ConfigurationHandler CH;
    InetAddress[] inetArray;

    public ConfigurationHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        CH = new ConfigurationHandler();
        inetArray = CH.getDevices();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGetDevicesAsInet() {
        System.out.println("Test if getDevicesAsInet returns array of inet objects");
        try {
            for (int i = 0; i < inetArray.length; i++) {
                assertTrue(inetArray[i] instanceof InetAddress);
            }
        } catch (AssertionError ae) {
            System.out.println("getDevicesAsInet does not return array of InetAddress");
        }
    }

    @Test
    public void testGetSnmpCommunitiesForDevice() {
        System.out.println("Test if getSnmpCommunitiesForDevice returns array of SNMP Communities");
        String[] communities = CH.getSnmpCommunitiesForDevice(inetArray[0], SnmpConstants.version2c);
        try {
            for (int i = 0; i < communities.length; i++) {
                assertTrue(communities[i] instanceof String);
            }
        } catch (AssertionError ae) {
            System.out.println("getSNMPCommunitiesForDevice does not return array of String");
        }
    }

    @Test
    public void testGetSnmpVersionsForDevice() {
        System.out.println("Test if getSnmpVersionsForDevice returns array of SNMP Versions");
        int[] versions = CH.getSnmpVersionsForDevice(inetArray[0]);
        try {
            for (int i = 0; i < versions.length; i++) {
                assertTrue((versions[i] == SnmpConstants.version1)
                        || (versions[i] == SnmpConstants.version2c)
                        || (versions[i] == SnmpConstants.version3));
            }
        } catch (AssertionError ae) {
            System.out.println("getSNMPVersionsForDevice does not return array of valid Verions");
        }
    }

    @Test
    public void testGetSnmpCycleTime() {
        System.out.println("Test if getSnmpCycleTime returns positive integer");
        try {
            assertTrue(CH.getSnmpCycleTime() > 0);
        } catch (AssertionError ae) {
            System.out.println("getSnmpCycleTime does not return positive integer");
        }
    }

    @Test
    public void testGetSnmpRetries() {
        System.out.println("Test if getSnmpRetries returns a non-negative integer");
        try {
            assertTrue(CH.getSnmpRetries() >= 0);
        } catch (AssertionError ae) {
            System.out.println("getSnmpRetries does not return non-negative integer");
        }
    }

    @Test
    public void testGetSnmpTimeout() {
        System.out.println("Test if getSnmpTimeout returns a positive integer");
        try {
            assertTrue(CH.getSnmpTimeout() > 0);
        } catch (AssertionError ae) {
            System.out.println("getSnmpTimeout does not return a positive integer");
        }
    }
}
