package omnia.snmp;

import omnia.analyzer.InterfaceAnalyzer;

/**
 * The template for the interface element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class InterfaceTemplate extends ElementTemplate {

    /**
     * The index element.
     */
    public static final int INDEX = 0;
    /**
     * The adminStatus element.
     */
    public static final int ADMINSTATUS = 1;
    /**
     * The operStatus element.
     */
    public static final int OPERSTATUS = 2;
    /**
     * The name element.
     */
    public static final int NAME = 3;
    /**
     * The description element.
     */
    public static final int DESCRIPTION = 4;
    /**
     * The media element.
     */
    public static final int MEDIA = 5;
    /**
     * The lastChange element.
     */
    public static final int LASTCHANGE = 6;
    /**
     * The mtu element.
     */
    public static final int MTU = 7;
    /**
     * The type element.
     */
    public static final int TYPE = 8;
    /**
     * The alias element.
     */
    public static final int ALIAS = 9;
    /**
     * The if X name element.
     */
    public static final int NAMEX = 10;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time the unique time id of this template.
     */
    public InterfaceTemplate(long time) {
        super(time);
        size = 11;
        operation = SnmpOperation.GETALL;
        template = "interface";
        initialize();
        elements[INDEX] = "index";
        elements[ADMINSTATUS] = "adminStatus";
        elements[OPERSTATUS] = "operStatus";
        elements[NAME] = "name";
        elements[DESCRIPTION] = "description";
        elements[MEDIA] = "media";
        elements[LASTCHANGE] = "lastChange";
        elements[MTU] = "mtu";
        elements[TYPE] = "type";
        elements[ALIAS] = "alias";
        elements[NAMEX] = "nameX";
    }

    /**
     * Stores a value for an element. Converts the lastChange value (from
     * hundredths of a second) to milliseconds.
     *
     * @param element the element for which to store the value.
     * @param value   the value to store.
     */
    @Override
    public void setValue(int element, String value) {
        if (element == LASTCHANGE && value != null) {
            long milSecAgo = Long.parseLong(value) * 10;
            this.values[element] = "" + milSecAgo;
        } else {
            super.setValue(element, value);
        }
    }

    @Override
    public Object getValue(int value) {
        if (value == LASTCHANGE) {
            return asLong(values[value]);
        }
        if (value == INDEX || value == MTU) {
            return asInteger(values[value]);
        }
        return super.getValue(value);
    }

    @Override
    public InterfaceTemplate clone() {
        return (InterfaceTemplate) deepCopy(new InterfaceTemplate(this.time));
    }

    @Override
    public void analyze() {
        InterfaceAnalyzer analyzer = new InterfaceAnalyzer();
        analyzer.analyze(this);
    }
}
