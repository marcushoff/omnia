package omnia.analyzer;

import omnia.db.DbOperation;
import omnia.snmp.ElementTemplate;

/**
 * The analyzer. This is the superclass of all the analyzers.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class Analyzer {

    protected final DbOperation operation = new DbOperation();

    public static void analyze(ElementTemplate template) {
    }

    protected void updateAllElements(ElementTemplate template,
                                            int object) {
        for (int i = 0; i < template.size(); i++) {
            if (template.hasValue(i)) {
                operation.update(object, template.getElement(i),
                                 template.getValue(i));
            }
        }
    }

    protected int updateDeviceCycleTime(int device,
                                                  String chassisId,
                                                  ElementTemplate template) {
        Long cycleTime = (Long) operation.getProperty(device, "cycleTime");
        String snmpAddress = template.getDevice();
        if (cycleTime == null) {
            operation.update(device, "cycleTime", template.getTime());
        } else if (cycleTime < template.getTime()) {
            operation.clear(device);
            device = operation.getOrCreateDevice(chassisId, snmpAddress);
            operation.update(device, "cycleTime", template.getTime());
        }
        return device;
    }

    protected int updateInterfaceCycleTime(int iface,
                                                     ElementTemplate template,
                                                     int device,
                                                     String index, String alias,
                                                     String nameX,
                                                     Integer portnumber) {
        Long cycleTime = (Long) operation.getProperty(iface, "cycleTime");
        if (cycleTime == null) {
            operation.update(iface, "cycleTime", template.getTime());
        } else if (cycleTime < template.getTime()) {
            operation.clear(iface);
            iface = operation.getOrCreateInterface(device, index, alias, nameX,
                                                   portnumber);
            operation.update(iface, "cycleTime", template.getTime());
        }
        return iface;
    }
}
