package omnia.snmp;

import omnia.analyzer.Analyzer;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;

/**
 * This class is an abstract for all the templates. Subclasses must implement
 * the clone method by calling deepCopy() with a new instance of the subclass
 * and casting the result to subclass. Any subclasses must only store values of
 * primitive types. (Integer, Long, Double, Float, String).
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class ElementTemplate implements Cloneable {

    /**
     * An array of the element names.
     */
    protected String[] elements;
    /**
     * An array of the element values.
     */
    protected String[] values;
    /**
     * An array of OID values.
     */
    protected String[] oids;
    /**
     * Number of elements.
     */
    protected int size;
    /**
     * Template name, matching XML sections.
     */
    protected String template;
    /**
     * The address of the device.
     */
    protected String device;
    /**
     * The SNMP operation.
     */
    protected int operation;
    /**
     * The unique time id.
     */
    protected final long time;

    /**
     * Default constructor. Sets the size, template name and initializes the
     * arrays.
     *
     * @param time the unique time id of this template.
     */
    public ElementTemplate(long time) {
        size = 0;
        operation = SnmpOperation.GET;
        initialize();
        template = null;
        this.time = time;
    }

    /**
     * Initializes the elements and values.
     *
     */
    protected final void initialize() {
        this.elements = new String[size];
        this.values = new String[size];
        this.oids = new String[size];
    }

    /**
     * Returns the template name.
     *
     * @return a String containing the template name.
     */
    public String template() {
        return template;
    }

    /**
     * Returns the number of elements in the template.
     *
     * @return an
     * <code>int</code> containing the number of elements.
     */
    public int size() {
        return size;
    }

    /**
     * Returns an element as a String.
     *
     * @param element the element.
     * @return a String containing the element.
     */
    public String getElement(int element) {
        return elements[element];
    }

    /**
     * Returns the value of an element. If the element isn't set, null is
     * returned. Use instanceof to determine the type returned.
     *
     * @param value the element.
     * @return an Object containing the value of the element or null.
     */
    public Object getValue(int value) {
        return this.values[value];
    }

    /**
     * Returns the value of an element. If the element isn't set null is
     * returned.
     *
     * @param value the element.
     * @return a String containing the value of the element or null.
     */
    public String getValueAsString(int value) {
        return this.values[value];
    }

    /**
     * Stores a value for an element.
     *
     * @param element the element for which to store the value.
     * @param value   the value to store.
     */
    public void setValue(int element, String value) {
        this.values[element] = value;
    }

    /**
     * Returns the OID for an element.
     *
     * @param element the element.
     * @return a String containing the OID or null if it does not exist.
     */
    public String getOid(int element) {
        return oids[element];
    }

    /**
     * Stores an OID for an element.
     *
     * @param element the element for which to store the OID.
     * @param oid     the OID to store.
     */
    public void setOid(int element, String oid) {
        this.oids[element] = oid;
    }

    /**
     * Returns the device.
     *
     * @return an IpAddress representing the device.
     */
    public String getDevice() {
        return device;
    }

    /**
     * Stores the device.
     *
     * @param device the device.
     */
    public void setDevice(IpAddress device) {
        this.device = device.toString();
    }

    /**
     * Stores the device.
     *
     * @param device the device.
     */
    public void setDevice(Address device) {
        this.device = (new IpAddress(device.toByteArray())).toString();
    }

    /**
     * Calls the analyzer with this template.
     */
    public void analyze() {
        Analyzer.analyze(this);
    }

    public int getOperation() {
        return operation;
    }

    protected ElementTemplate deepCopy(ElementTemplate copy) {
        copy.size = this.size;
        copy.operation = this.getOperation();
        copy.template = this.template;
        copy.device = this.device;
        copy.initialize();
        for (int i = 0; i < size; i++) {
            if (this.getElement(i) != null) {
                copy.elements[i] = this.getElement(i);
            }
            if (this.getValue(i) != null) {
                copy.values[i] = this.getValueAsString(i);
            }
            if (this.getOid(i) != null) {
                copy.oids[i] = this.oids[i];
            }
        }
        return copy;
    }

    @Override
    public ElementTemplate clone() {
        return deepCopy(new ElementTemplate(this.time));
    }

    /**
     * Returns the values as an array.
     *
     * @return a String array of all values.
     */
    public String[] getValues() {
        return this.values;
    }

    /**
     * Tests if a value has been assigned to an element.
     *
     * @param value the value to test.
     * @return false if the value is null, otherwise true.
     */
    public boolean hasValue(int value) {
        if (values[value] == null) {
            return false;
        }
        return true;
    }

    /**
     * Returns the unique time id of this template.
     *
     * @return a
     * <code>long</code> representing the time id.
     */
    public long getTime() {
        return this.time;
    }

    protected Long asLong(String s) {
        try {
            return new Long(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected Integer asInteger(String s) {
        try {
            return new Integer(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
