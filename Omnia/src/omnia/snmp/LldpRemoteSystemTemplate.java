package omnia.snmp;

import omnia.analyzer.LldpRemoteSystemAnalyzer;

/**
 * The template for the lldpRemoteSystem element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class LldpRemoteSystemTemplate extends ElementTemplate {

    /**
     * The ifSubtype element.
     */
    public static final int IFSUBTYPE = 0;
    /**
     * The ifId element.
     */
    public static final int IFID = 1;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time the unique time id of this template.
     */
    public LldpRemoteSystemTemplate(long time) {
        super(time);
        size = 2;
        operation = SnmpOperation.GETALL;
        template = "lldpRemoteSystem";
        initialize();
        elements[IFSUBTYPE] = "ifSubtype";
        elements[IFID] = "ifId";
    }

    @Override
    public Object getValue(int value) {
        if (value == IFID) {
            return asInteger(values[value]);
        }
        return super.getValue(value);
    }

    @Override
    public LldpRemoteSystemTemplate clone() {
        return (LldpRemoteSystemTemplate) deepCopy(new LldpRemoteSystemTemplate(this.time));
    }

    @Override
    public void analyze() {
        LldpRemoteSystemAnalyzer.analyze(this);
    }
}
