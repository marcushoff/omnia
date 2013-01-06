package omnia.analyzer;

import omnia.snmp.LldpLocalPortTemplate;

/**
 * The LLDP Local Port analyzer. For analyzing LldpLocalPort templates.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class LldpLocalPortAnalyzer extends Analyzer {

    public  void analyze(LldpLocalPortTemplate template) {
        int thisDevice = operation.getOrCreateDevice(null, template.getDevice());
        thisDevice = updateDeviceCycleTime(thisDevice, null, template);
//TODO implement check for portComponent, macAddress, networkAddress, agentCircuitId (DHCP)
        String subtype = (String) template.getValue(
                LldpLocalPortTemplate.SUBTYPE);
        String index = null;
        String alias = null;
        String nameX = null;
        if (subtype.equals("interfaceAlias")) {
            alias = (String) template.getValue(LldpLocalPortTemplate.ID);
        } else if (subtype.equals("interfaceName")) {
            nameX = (String) template.getValue(LldpLocalPortTemplate.ID);
        } else if (subtype.equals("local")) {
            index = (String) template.getValue(LldpLocalPortTemplate.ID);
        }
        Integer portnumber = (Integer) template.getValue(
                LldpLocalPortTemplate.PORTNUMBER);
        if (portnumber == null) {
            portnumber = new Integer(index);
        }
        int thisInterface =
                operation.getOrCreateInterface(thisDevice, index,
                                               alias, nameX, portnumber);
        thisInterface = updateInterfaceCycleTime(thisInterface, template,
                                                 thisDevice, index, alias, nameX,
                                                 portnumber);
        operation.update(thisInterface, "portnumber", portnumber);
        operation.close();
    }
}
