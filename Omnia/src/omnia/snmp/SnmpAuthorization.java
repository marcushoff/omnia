package omnia.snmp;

/**
 * This class provides a method of exchanging SNMP authorization information
 * between classes. It contains the SNMP community and version.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class SnmpAuthorization {

    /**
     * The SNMP version.
     */
    private int version;
    /**
     * The SNMP community.
     */
    private String community;

    /**
     * Default constructor. Sets the SNMP version to 0 and the community to
     * null.
     */
    public SnmpAuthorization() {
        this.community = null;
        this.version = 0;
    }

    /**
     * Constructor sets the SNMP version and community.
     *
     * @param version   the SNMP version
     * @param community the SNMP community
     */
    public SnmpAuthorization(int version, String community) {
        this.version = version;
        this.community = community;
    }

    /**
     * Returns the SNMP community.
     *
     * @return a String containing the SNMP community
     */
    public String getCommunity() {
        return community;
    }

    /**
     * Sets the SNMP community.
     *
     * @param community the SNMP community
     */
    public void setCommunity(String community) {
        this.community = community;
    }

    /**
     * Returns the SNMP version.
     *
     * @return an
     * <code>int</code> containing the SNMP version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Sets the SNMP version.
     *
     * @param version the SNMP version
     */
    public void setVersion(int version) {
        this.version = version;
    }
}
