package omnia.snmp;

import omnia.analyzer.LldpLocalPortAnalyzer;

/**
 * The template for the lldpLocalPort element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class LldpLocalPortTemplate extends ElementTemplate {

    /**
     * The subtype element.
     */
    public static final int SUBTYPE = 0;
    /**
     * The id element.
     */
    public static final int ID = 1;
    /**
     * The port number element.
     */
    public static final int PORTNUMBER = 2;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time the unique time id of this template.
     */
    public LldpLocalPortTemplate(long time) {
        super(time);
        size = 3;
        operation = SnmpOperation.GETALL;
        template = "lldpLocalPort";
        initialize();
        elements[SUBTYPE] = "subtype";
        elements[ID] = "id";
        elements[PORTNUMBER] = "portnumber";
    }

    @Override
    public Object getValue(int value) {
        if (value == PORTNUMBER) {
            return asInteger(values[value]);
        }
        return super.getValue(value);
    }

    @Override
    public LldpLocalPortTemplate clone() {
        return (LldpLocalPortTemplate) deepCopy(new LldpLocalPortTemplate(
                this.time));
    }

    @Override
    public void analyze() {
        LldpLocalPortAnalyzer analyzer = new LldpLocalPortAnalyzer();
        analyzer.analyze(this);
    }
}
