package omnia;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import omnia.snmp.SnmpAuthorization;
import org.apache.commons.net.util.SubnetUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;

/**
 * This class is an abstraction layer for accessing the contents of the
 * configuration. All other classes must write and read configuration
 * information through this class.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class ConfigurationHandler {

    private static String configurationFileName = "configuration.xml";
    private Element rootElement;
    private Map<Address, Integer> devices;
    /*
     * Default values, if the configuration file is corrupted
     */
    private static final String DEFAULT_SNMP_PORT = "161";
    private static final String DEFAULT_SNMP_PROTOCOL = "udp";
    private static final String DEFAULT_SNMP_TIMEOUT = "1500";
    private static final String DEFAULT_SNMP_RETRIES = "2";
    private static final String DEFAULT_SNMP_CYCLETIME = "300000";
    private static final String DEFAULT_SNMP_PDUSIZE = "3";
    private static final String DEFAULT_DIR_MIBS = "mibs";
    private static final String DEFAULT_DIR_PLUGINS = "plugins";
    private static final String DEFAULT_PLUGIN_DEFAULT = "default.xml";

    /**
     * Default constructor. Connects to the configuration file and sets the root
     * element.
     */
    public ConfigurationHandler() {
        try {
            File configurationFile = new File(configurationFileName);
            SAXBuilder builder = new SAXBuilder();
            Document configurationDoc = builder.build(configurationFile);
            rootElement = configurationDoc.getRootElement();
            devices = new HashMap<Address, Integer>();
            setDevices();
        } catch (Exception e) {
            //TODO Handle errors
            System.out.println("Exception : " + e.getMessage());
        }
    }

    /**
     * Returns all devices in the configuration.
     *
     * @return an array of InetAddress containing the devices.
     */
    public Address[] getDevices() {
        setDevices();
        return devices.keySet().toArray(new Address[0]);
    }

    private Iterator<Element> getAuthorizationIterator() {
        return rootElement.getChild("authorizationList").getChildren(
                "authorization").iterator();
    }

    private Iterator<Element> getDeviceIterator() {
        return rootElement.getChild("deviceList").getChildren(
                "deviceDeclaration").iterator();
    }

    private String getIdValue(Element declaration) {
        return declaration.getAttribute("id").getValue();
    }

    private Element getNext(Iterator iterator) {
        return (Element) iterator.next();
    }

    private String getSnmpElement(String element, String defaultValue) {
        String xmlString = getSnmp().getChildTextNormalize(element);
        if (xmlString == null || xmlString.isEmpty()) {
            return defaultValue;
        } else {
            return xmlString.toLowerCase();
        }
    }

    private String getDirectoriesElement(String element, String defaultValue) {
        String xmlString = getDirectories().getChildTextNormalize(element);
        if (xmlString == null || xmlString.isEmpty()) {
            return defaultValue;
        } else {
            return xmlString.toLowerCase();
        }
    }

    private String getPluginsElement(String element,
                                     String defaultValue) {
        String xmlString = getPlugins().getChildTextNormalize(element);
        if (xmlString == null || xmlString.isEmpty()) {
            return defaultValue;
        } else {
            return xmlString.toLowerCase();
        }

    }

    private Element getSnmp() {
        return rootElement.getChild("snmp");
    }

    private Element getDirectories() {
        return rootElement.getChild("directories");
    }

    private Element getPlugins() {
        return rootElement.getChild("plugins");
    }

    private void setDevices() {
        Iterator deviceDeclarations = getDeviceIterator();
        while (deviceDeclarations.hasNext()) {
            Element declaration = getNext(deviceDeclarations);
            Iterator definition = declaration.getChildren().iterator();
            while (definition.hasNext()) {
                Element currentElement = getNext(definition);
                String name = currentElement.getName();
                if (name.matches("cidr")) {
                    parseCidr(currentElement, declaration);
                }
            }
        }
    }

    private void parseCidr(Element cidrElement, Element declaration) {
        String cidr = cidrElement.getTextNormalize();
        SubnetUtils utils = new SubnetUtils(cidr);
        String[] cidrAddresses = utils.getInfo().getAllAddresses();
        for (int i = 0; i < cidrAddresses.length; i++) {
            try {
                Integer deviceId = new Integer(getIdValue(declaration));
                devices.put(GenericAddress.parse(getProtocol()
                                                 + ":"
                                                 + cidrAddresses[i]
                                                 + "/" + getPort()),
                            deviceId);
            } catch (IllegalArgumentException ex) {
                //TODO handle error
                Logger.getLogger(ConfigurationHandler.class.getName()).log(
                        Level.SEVERE,
                                                                           null,
                                                                           ex);
            }
        }
    }

    /**
     * Returns an array of SNMP authorizations in preferred order for a device
     *
     * @param device the device
     *
     * @return an array of SnmpAuthorization containing the SNMP authorizations
     */
    public SnmpAuthorization[] getSnmpAuthorizationForDevice(Address device) {
        Integer deviceId = devices.get(device);
        Integer foundId = null;
        Element deviceElement = null;
        Iterator deviceIterator = getDeviceIterator();
        while (deviceIterator.hasNext()) {
            deviceElement = getNext(deviceIterator);
            foundId = new Integer(getIdValue(deviceElement));
            if (foundId.equals(deviceId)) {
                break;
            }
            foundId = null;
        }
        if (foundId == null) {
            //TODO handle error
            Logger.getLogger(ConfigurationHandler.class.getName()).log(
                    Level.WARNING,
                                                                       null,
                                                                       "Device does not exist");
            return new SnmpAuthorization[0];
        }
        Iterator deviceAuthorizationIterator = deviceElement.getChildren(
                "authorization").iterator();
        ArrayList<String> deviceAuthorizations = new ArrayList<String>();
        while (deviceAuthorizationIterator.hasNext()) {
            deviceAuthorizations.add(
                    (getNext(deviceAuthorizationIterator)).getTextNormalize());
        }
        Iterator authorizationIterator = getAuthorizationIterator();
        SnmpAuthorization[] snmpAuthorizations =
                new SnmpAuthorization[deviceAuthorizations.size()];
        while (authorizationIterator.hasNext()) {
            Element authorization = getNext(authorizationIterator);
            int deviceAuthorizationIndex =
                    deviceAuthorizations.indexOf(getIdValue(authorization));
            if (deviceAuthorizationIndex >= 0) {
                Element versionElement = authorization.getChild("version");
                Element communityElement = authorization.getChild(
                        "readCommunity");
                if (communityElement == null) {
                    //TODO handle error
                    Logger.getLogger(ConfigurationHandler.class.getName()).log(
                            Level.SEVERE,
                                                                               null,
                                                                               "No Community");
                }
                if (versionElement == null) {
                    //TODO handle error
                    Logger.getLogger(ConfigurationHandler.class.getName()).log(
                            Level.SEVERE,
                                                                               null,
                                                                               "No Version");

                } else {
                    String versionString = versionElement.getTextNormalize();
                    int versionInt = 0;
                    if (versionString.equals("1")) {
                        versionInt = SnmpConstants.version1;
                    } else if (versionString.equals("2c")) {
                        versionInt = SnmpConstants.version2c;
                    } else if (versionString.equals("3")) {
                        versionInt = SnmpConstants.version3;
                    } else {
                        //TODO handle error
                    }
                    snmpAuthorizations[deviceAuthorizationIndex] =
                            new SnmpAuthorization(versionInt,
                                                  communityElement.getTextNormalize());
                }
            }
        }
        return snmpAuthorizations;
    }

    /**
     * Returns the rerun time for an SNMP collection cycle.
     *
     * @return an
     * <code>int</code> containing the time in ms or the default value if not
     * found.
     */
    public int getSnmpCycleTime() {
        return Integer.parseInt(getSnmpElement("cycletime",
                                               DEFAULT_SNMP_CYCLETIME));
    }

    /**
     * Returns the number of retries for an SNMP operations.
     *
     * @return an
     * <code>int</code> containing the number of retries or the default value if
     * not found.
     */
    public int getSnmpRetries() {
        return Integer.parseInt(getSnmpElement("retries",
                                               DEFAULT_SNMP_RETRIES));
    }

    /**
     * Returns the timeout for SNMP operations.
     *
     * @return an
     * <code>int</code> containing the timeout in ms or the default value if not
     * found.
     */
    public int getSnmpTimeout() {
        return Integer.parseInt(getSnmpElement("timeout",
                                               DEFAULT_SNMP_TIMEOUT));
    }

    /**
     * Returns the protocol for SNMP operations.
     *
     * @return a String containing the protocol or the default value if not
     *         found.
     */
    public String getProtocol() {
        return getSnmpElement("protocol", DEFAULT_SNMP_PROTOCOL);
    }

    /**
     * Returns the port for SNMP operations.
     *
     * @return a String containing the portor the default value if not found.
     */
    public String getPort() {
        return getSnmpElement("port", DEFAULT_SNMP_PORT);
    }

    /**
     * Returns the mibs directory.
     *
     * @return a String containing the directory or the default value if not
     *         found.
     */
    public String getMibsDir() {
        return getDirectoriesElement("mibs", DEFAULT_DIR_MIBS);
    }

    /**
     * Returns the plugins directory.
     *
     * @return a String containing the directory or the default value if not
     *         found.
     */
    public String getPluginsDir() {
        return getDirectoriesElement("plugins", DEFAULT_DIR_PLUGINS);
    }

    public int getPduSize() {
        return Integer.parseInt(getSnmpElement("pduSize",
                                               DEFAULT_SNMP_PDUSIZE));
    }

    public String getDefaultPlugin() {
        return getPluginsElement("default", DEFAULT_PLUGIN_DEFAULT);
    }
}
