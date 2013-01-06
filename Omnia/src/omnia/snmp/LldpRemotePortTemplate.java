package omnia.snmp;

import omnia.analyzer.LldpRemotePortAnalyzer;

/**
 * The template for the lldpRemotePort element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class LldpRemotePortTemplate extends ElementTemplate {

    /**
     * The localPort element.
     */
    public static final int LOCALPORT = 0;
    /**
     * The index element.
     */
    public static final int INDEX = 1;
    /**
     * The chassisSubtype element.
     */
    public static final int CHASSISSUBTYPE = 2;
    /**
     * The chassisId element.
     */
    public static final int CHASSISID = 3;
    /**
     * The subtype element.
     */
    public static final int SUBTYPE = 4;
    /**
     * The id element.
     */
    public static final int ID = 5;
    /**
     * The description element.
     */
    public static final int DESCRIPTION = 6;
    /**
     * The systemName element.
     */
    public static final int SYSTEMNAME = 7;
    /**
     * The systemDescription element.
     */
    public static final int SYSTEMDESCRIPTION = 8;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time the unique time id of this template.
     */
    public LldpRemotePortTemplate(long time) {
        super(time);
        size = 9;
        operation = SnmpOperation.GETALL;
        template = "lldpRemotePort";
        initialize();
        elements[LOCALPORT] = "localPort";
        elements[INDEX] = "index";
        elements[CHASSISSUBTYPE] = "chassisSubtype";
        elements[CHASSISID] = "chassisId";
        elements[SUBTYPE] = "subtype";
        elements[ID] = "id";
        elements[DESCRIPTION] = "description";
        elements[SYSTEMNAME] = "systemName";
        elements[SYSTEMDESCRIPTION] = "systemDescription";
    }

    @Override
    public Object getValue(int value) {
        if (value == LOCALPORT || value == INDEX) {
            return asInteger(values[value]);
        }
        return super.getValue(value);
    }

    @Override
    public LldpRemotePortTemplate clone() {
        return (LldpRemotePortTemplate) deepCopy(new LldpRemotePortTemplate(this.time));
    }

    @Override
    public void analyze() {
        LldpRemotePortAnalyzer.analyze(this);
    }
}
