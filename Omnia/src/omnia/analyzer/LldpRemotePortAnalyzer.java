package omnia.analyzer;

import omnia.snmp.LldpRemotePortTemplate;

/**
 * The LLDP Remote Port analyzer. For analyzing LldpRemotePort templates.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class LldpRemotePortAnalyzer extends Analyzer {
//TODO: run sanitycheck on this

    public static void analyze(LldpRemotePortTemplate template) {
//        Object thisDevice = dbHandler.getOrCreateDevice(null,
//                                                        template.getDevice());
//        thisDevice = updateDeviceCycleTime(thisDevice, null, template);
//        Integer portnumber = (Integer) template.getValue(
//                LldpRemotePortTemplate.LOCALPORT);
//        Object thisInterface = dbHandler.getOrCreateInterface(thisDevice, null,
//                                                              null, null,
//                                                              portnumber);
//        thisInterface = updateInterfaceCycleTime(thisInterface, template,
//                                                 thisDevice, null, null, null,
//                                                 portnumber);
//        dbHandler.setHas(thisDevice, thisInterface);
//        Object remoteDevice =
//                dbHandler.getOrCreateDevice(template.getValueAsString(
//                LldpRemotePortTemplate.CHASSISID), null);
//
//        remoteDevice = updateDeviceCycleTime(remoteDevice,
//                                             template.getValueAsString(
//                LldpRemotePortTemplate.CHASSISID), template);
//
////TODO implement check for portComponent, macAddress, networkAddress, agentCircuitId (DHCP)
//        String subtype = (String) template.getValue(
//                LldpRemotePortTemplate.SUBTYPE);
//        String index = template.getValueAsString(LldpRemotePortTemplate.INDEX);
//        String alias = null;
//        String nameX = null;
//        if (subtype.equals("interfaceAlias")) {
//            alias = template.getValueAsString(LldpRemotePortTemplate.ID);
//        } else if (subtype.equals("interfaceName")) {
//            nameX = template.getValueAsString(LldpRemotePortTemplate.ID);
//        } else if (subtype.equals("local")) {
//            index = template.getValueAsString(LldpRemotePortTemplate.ID);
//        }
//        Object remoteInterface =
//                dbHandler.getOrCreateInterface(remoteDevice, index,
//                                               alias, nameX, null);
//        remoteInterface =
//                updateInterfaceCycleTime(remoteInterface, template, remoteDevice,
//                                         index, alias, nameX, null);
//        dbHandler.setHas(remoteDevice, remoteInterface);
//        dbHandler.update(remoteInterface, "description",
//                         template.getValue(LldpRemotePortTemplate.DESCRIPTION));
//        dbHandler.update(remoteDevice, "name",
//                         template.getValue(LldpRemotePortTemplate.SYSTEMNAME));
//        dbHandler.update(remoteDevice, "description",
//                         template.getValue(
//                LldpRemotePortTemplate.SYSTEMDESCRIPTION));
//        dbHandler.setCable(thisInterface, remoteInterface);
    }
}
