package omnia;

/**
 * This class does the collection of SNMP data from the devices. The collection
 * is based on the configuration and SNMP plugins. It runs an endless loop
 * through all devices in the configuration and creates new SNMP operations and
 * then parses the results to analyzer.
 *
 * The class is multi threaded. The main thread is started calling the run
 * method, which forks of new threads for each asynchronous SNMP call.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import omnia.snmp.*;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;

public class Collector implements Runnable, OperationListener {

    /**
     * Shorthand for the configurationHandler.
     */
    private final ConfigurationHandler configurationHandler =
            Omnia.configurationHandler;
    /**
     * Shorthand for the pluginHandler.
     */
    private final SnmpPluginHandler pluginHandler =
            Omnia.snmpPluginHandler;
    /**
     * Mapping of the capabilities of each device.
     */
    private Map<Address, CapabilityTemplate> deviceCapabilities;
    /**
     * The devices to collect data from.
     */
    private Address[] devices;

    /**
     * Default constructor. Initializes SNMP transport.
     */
    public Collector() {
        this.deviceCapabilities =
                Collections.synchronizedMap(
                new HashMap<Address, CapabilityTemplate>());
    }

    /**
     * Start the main thread. Cycles through all configured devices and spawns
     * of new asynchronous SNMP requests. It repeats after cycle time. The cycle
     * time is stored in the configuration. The thread can be stopped by calling
     * an close.
     */
    @Override
    public void run() {
        try {
            while (true) {
                Date date = new Date();
                long startTime = date.getTime();
                this.devices = configurationHandler.getDevices();
                //TODO handle device timeouts, when new requested array is initialized
                for (int i = 0; i < this.devices.length; i++) {
                    createOperation(new CapabilityTemplate(startTime),
                                    devices[i]);
                }
                long doneTime = date.getTime();
                long runTime = doneTime - startTime;
                long remainingTime = configurationHandler.getSnmpCycleTime()
                                     - runTime;
                if (remainingTime > 0) {
                    Thread.sleep(remainingTime);
                } else if (remainingTime < 0) {
                    Logger.getLogger(Collector.class.getName()).log(
                            Level.CONFIG,
                            "Cycle time to low for number of devices");
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private CapabilityTemplate getCapabilities(Address address) {
        return this.deviceCapabilities.get(address);
    }

    private CommunityTarget[] createTargets(Address device) {
        CommunityTarget[] targets;
        SnmpAuthorization[] snmpAuthorizations;
        snmpAuthorizations = configurationHandler.getSnmpAuthorizationForDevice(
                device);
        targets = new CommunityTarget[snmpAuthorizations.length];
        for (int i = 0; i < snmpAuthorizations.length; i++) {
            CommunityTarget target = new CommunityTarget();
            target.setAddress(device);
            target.setCommunity(new OctetString(
                    snmpAuthorizations[i].getCommunity()));
            target.setVersion(snmpAuthorizations[i].getVersion());
            target.setRetries(configurationHandler.getSnmpRetries());
            target.setTimeout(configurationHandler.getSnmpTimeout());
            targets[i] = target;
        }
        return targets;
    }

    private void createOperation(ElementTemplate template, Address address) {
        SnmpOperation operation = new SnmpOperation(createTargets(address),
                                                    template, this);
        SnmpParser parser = new SnmpParser();
        parser.parseOperation(operation, getCapabilities(address));
        Thread operationThread = new Thread(operation);
        operationThread.start();

    }

    /**
     * Is called on an asynchronous response. Dispatches the response for
     * handling based on the request of the operation.
     */
    @Override
    public void onStop(SnmpOperation operation) {
        if (operation.hasResponses()) {
            //TODO: her  need to implement message passing between threads with new allocation of objects, so thread can be interrupted.
            Address peer = operation.getAddress();
            ElementTemplate[] templates;
            ElementTemplate template = operation.getTemplate().clone();
            SnmpParser parser = new SnmpParser();
            PDU[] responses = operation.getResponses();
            templates = parser.parseTemplate(responses, template, peer, getCapabilities(peer));
            long time = template.getTime();
            if (template instanceof CapabilityTemplate) {
                capabilityResponse(peer, time, templates);
            } else {
                for (int i = 0; i < templates.length; i++) {
                    templates[i].analyze();
               }
            }
        } else {
            //TODO catch response errors and non responders
        }
    }

    /**
     * Handles the response of a capability request. This must be the first
     * request. It stores the capabilities and creates all subsequent
     * operations.
     *
     * @param operation the finished operation.
     */
    private void capabilityResponse(Address peer, long time,
                                    ElementTemplate[] parsedTemplates) {
//        Address peer = operation.getAddress();
        this.deviceCapabilities.put(peer,
                                    (CapabilityTemplate) parsedTemplates[0]);
        pluginHandler.setPlugin((CapabilityTemplate) parsedTemplates[0]);
        createOperation(new DeviceTemplate(time), peer);
        createOperation(new InterfaceTemplate(time), peer);
        createOperation(new LldpLocalPortTemplate(time), peer);
        createOperation(new LldpRemotePortTemplate(time), peer);
        createOperation(new LldpRemoteSystemTemplate(time), peer);
        //TODO implement template and analyzer for entity (ENTITY-MIB)
    }
}