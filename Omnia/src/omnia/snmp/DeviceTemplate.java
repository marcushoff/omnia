package omnia.snmp;

import java.util.Calendar;
import omnia.analyzer.DeviceAnalyzer;

/**
 * The template for the device element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class DeviceTemplate extends ElementTemplate {

    /**
     * The description element.
     */
    public static final int DESCRIPTION = 0;
    /**
     * The uptime element.
     */
    public static final int UPTIME = 1;
    /**
     * The contact element.
     */
    public static final int CONTACT = 2;
    /**
     * The name element.
     */
    public static final int NAME = 3;
    /**
     * The location element.
     */
    public static final int LOCATION = 4;
    /**
     * The services element.
     */
    public static final int SERVICES = 5;
    /**
     * The serial element.
     */
    public static final int SERIAL = 6;
    /**
     * The brand element.
     */
    public static final int BRAND = 7;
    /**
     * The model element.
     */
    public static final int MODEL = 8;
    /**
     * The number of interfaces element.
     */
    public static final int NUMBEROFIF = 9;
    /**
     * The chassisSubtype element.
     */
    public static final int CHASSISSUBTYPE = 10;
    /**
     * The chassisId element.
     */
    public static final int CHASSISID = 11;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time the unique time id of this template.
     */
    public DeviceTemplate(long time) {
        super(time);
        size = 12;
        template = "device";
        initialize();
        elements[DESCRIPTION] = "description";
        elements[UPTIME] = "uptime";
        elements[CONTACT] = "contact";
        elements[NAME] = "name";
        elements[LOCATION] = "location";
        elements[SERVICES] = "services";
        elements[SERIAL] = "serial";
        elements[BRAND] = "brand";
        elements[MODEL] = "model";
        elements[NUMBEROFIF] = "numberOfIf";
        elements[CHASSISSUBTYPE] = "chassisSubtype";
        elements[CHASSISID] = "chassisId";
    }

    /**
     * Stores a value for an element. Converts the uptime value (from hundredths
     * of a second) to milliseconds and stores it as an absolute time.
     *
     * @param element the element for which to store the value.
     * @param value   the value to store.
     */
    @Override
    public void setValue(int element, String value) {
        if (element == UPTIME && value != null) {
            long rightNow = Calendar.getInstance().getTimeInMillis();
            long milSecAgo = Long.parseLong(value) * 10;
            this.values[element] = "" + (rightNow - milSecAgo);
        } else {
            super.setValue(element, value);
        }
    }

    @Override
    public Object getValue(int value) {
        if (value == UPTIME) {
            return asLong(values[value]);
        }
        if (value == SERVICES || value == NUMBEROFIF) {
            return asInteger(values[value]);
        }
        return super.getValue(value);
    }

    @Override
    public DeviceTemplate clone() {
        return (DeviceTemplate) deepCopy(new DeviceTemplate(this.time));
    }

    @Override
    public void analyze() {
        DeviceAnalyzer analyzer = new DeviceAnalyzer();
        analyzer.analyze(this);
    }
}
