package omnia.snmp;

import org.jdom2.Document;

/**
 * The template for the capability element.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class CapabilityTemplate extends ElementTemplate {

    /**
     * The objectid element.
     */
    public static final int OBJECTID = 0;
    private Document document;

    /**
     * Default constructor. Sets the template name and adds the elements.
     *
     * @param time
     */
    public CapabilityTemplate(long time) {
        super(time);
        size = 1;
        initialize();
        elements[OBJECTID] = "objectId";
        template = "capability";
        document = null;
    }

    @Override
    public CapabilityTemplate clone() {
        return (CapabilityTemplate) deepCopy(new CapabilityTemplate(time));
    }

    public Document getDocument() {
        return this.document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
