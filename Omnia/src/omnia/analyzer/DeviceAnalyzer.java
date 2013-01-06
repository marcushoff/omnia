package omnia.analyzer;

import omnia.snmp.DeviceTemplate;

/**
 * The device analyzer. For analyzing Device templates.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class DeviceAnalyzer extends Analyzer {

    public void analyze(DeviceTemplate template) {
        int thisDevice = operation.getOrCreateDevice(template.getValueAsString(
                DeviceTemplate.CHASSISID), template.getDevice());
        operation.update(thisDevice, "cycleTime", template.getTime());
        updateAllElements(template, thisDevice);
        operation.close();
    }
}
