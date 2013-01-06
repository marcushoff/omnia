package omnia.analyzer;

import omnia.snmp.InterfaceTemplate;

/**
 * The interface analyzer. For analyzing Interface templates.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class InterfaceAnalyzer extends Analyzer {

    public void analyze(InterfaceTemplate template) {
        int thisDevice = operation.getOrCreateDevice(null, template.getDevice());
        thisDevice = updateDeviceCycleTime(thisDevice, null, template);
        //TODO check name difference between name and nameX
        int thisInterface = operation.getOrCreateInterface(thisDevice,
                                                           template.getValue(
                InterfaceTemplate.INDEX), null, null, null);
        operation.update(thisInterface, "cycleTime", template.getTime());
        operation.setHas(thisDevice, thisInterface);
        updateAllElements(template, thisInterface);
        operation.close();
    }
}
