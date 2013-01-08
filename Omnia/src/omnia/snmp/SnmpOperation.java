package omnia.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibValueSymbol;
import omnia.Collector;
import omnia.ConfigurationHandler;
import omnia.Omnia;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * This class does the actual SNMP operation. The operation is loaded with the
 * PDU, targets, transport mapping and the operation type. When these are loaded
 * run() is called to run the actual SNMP operation. When the operation finishes
 * it calls the onStop() of the OperationListener.
 *
 * Targets is an array of IP and SNMP authorizations. This class will iterate
 * through them until it finds a match.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class SnmpOperation implements Runnable {

    /**
     * Shorthand for the configurationHandler.
     */
    private final ConfigurationHandler configurationHandler =
            Omnia.configurationHandler;
    /**
     * The operation. See operation types.
     */
    private int operation;
    /**
     * The template type to use.
     */
    private ElementTemplate template;
    /**
     * The request PDU.
     */
    private PDU request;
    /**
     * The PDU responses.
     */
    private PDU[] responses;
    /**
     * The transport mapping.
     */
    private TransportMapping transport;
    /**
     * The SNMP.
     */
    private Snmp snmp;
    /**
     * The community targets for the SNMP.
     */
    private CommunityTarget[] targets;
    /**
     * The operation listener to return to when finished.
     */
    private OperationListener listener;
    /**
     * Operation type GET.
     */
    public static final int GET = 1;
    /**
     * Operation type GETNEXT.
     */
    public static final int GETNEXT = 2;
    /**
     * Operations type GETALL, runs a GETNEXT on a table.
     */
    public static final int GETALL = 3;

    /**
     * Short constructor. Sets the targets, template and listener and
     * initializes the operation.
     *
     * @param targets  the targets.
     * @param template the template.
     * @param listener the listener.
     */
    public SnmpOperation(CommunityTarget[] targets, ElementTemplate template,
                         OperationListener listener) {
        this.targets = targets;
        this.template = template;
        this.listener = listener;
        this.operation = template.getOperation();
        initialize();
    }

    /**
     * Long constructor. Sets the operation, request, targets and listener and
     * initializes the operation.
     *
     * @param operation the operation type.
     * @param template  the template.
     * @param targets   the targets.
     * @param listener  the listener.
     */
    public SnmpOperation(int operation, ElementTemplate template,
                         CommunityTarget[] targets, OperationListener listener) {
        this.operation = operation;
        this.listener = listener;
        this.template = template;
        this.operation = template.getOperation();
        this.targets = targets;
        initialize();
    }

    /**
     * Initializes the operation. Creates a new PDU, clears the responses and
     * sets up a listener on the SNMP transport.
     */
    private void initialize() {
        this.request = new PDU();
        this.responses = null;
        try {
            this.transport = new DefaultUdpTransportMapping();
            this.snmp = new Snmp(transport);
            this.transport.listen();
        } catch (IOException ex) {
            Logger.getLogger(SnmpOperation.class.getName()).log(Level.SEVERE,
                                                                null, ex);
        }
    }

    /**
     * Returns the responses.
     *
     * @return a PDU[] containing the responses.
     */
    public PDU[] getResponses() {
        return this.responses;
    }

    /**
     * Returns the targets.
     *
     * @return the CommunityTarget[] containing the targets.
     */
    public CommunityTarget[] getTargets() {
        return this.targets;
    }

    /**
     * Returns the address of the device.
     *
     * @return an Address of the device.
     */
    public Address getAddress() {
        return this.targets[0].getAddress();
    }

    /**
     * Sets the targets of the device. This is a list of IP and authorizations.
     * All targets should be to the same IP.
     *
     * @param targets the targets.
     */
    public void setTargets(CommunityTarget[] targets) {
        this.targets = targets;
    }

    /**
     * Returns the template.
     *
     * @return the template.
     */
    public ElementTemplate getTemplate() {
        return this.template;
    }

    /**
     * Sets the template.
     *
     * @param template the template.
     */
    public void setTemplate(ElementTemplate template) {
        this.template = template;
    }

    /**
     * Sets the operation to perform. Valid operations are GET, GETNEXT and
     * GETALL.
     *
     * @param operation the operation.
     */
    public void setOperation(int operation) {
        this.operation = operation;
    }

    /**
     * Returns the operation.
     *
     * @return an
     * <code>int</code> containing the operation.
     */
    public int getOperation() {
        return this.operation;
    }

    /**
     * Runs the operation. When everything has been added to the PDU and the
     * operation type has been set. Run this method to execute the operation.
     */
    @Override
    public void run() {
        if (this.request != null) {
            switch (this.operation) {
                case GET:
                    this.request.setType(PDU.GET);
                    break;
                case GETNEXT:
                    this.request.setType(PDU.GETNEXT);
                    break;
                case GETALL:
                    this.request.setType(PDU.GETNEXT);
                    break;
                default:

                //TODO handle error
            }
        }
        runGetOID();
        this.listener.onStop(this);
    }

    private void runGetOID() {
        boolean authorizationFailure = true;
        PDU nextPdu = this.request;
        ArrayList<PDU> allResponses = new ArrayList<PDU>();
        try {
            boolean moreOIDs = false;
            if (this.operation == GETALL) {
                moreOIDs = true;
            }
            do {
                int pointer = 0;
                PDU[] requests = split(nextPdu);
                PDU[] localResponses = new PDU[requests.length];
                for (int i = 0; i < targets.length && authorizationFailure; i++) {
                    for (int j = 0; j < requests.length; j++) {
                        ResponseEvent response = this.snmp.send(requests[j],
                                                                this.targets[i]);
                        if (response.getPeerAddress() == null) {
                            moreOIDs = false;
                            break;
                        }
                        PDU localResponse = response.getResponse();
                        if (this.operation == GETALL) {
                            VariableBinding[] bindings = localResponse.toArray();
                            if (bindings.length <= 0) {
                                moreOIDs = false;
                                break;
                            }
                            for (int k = 0; k < bindings.length; k++) {
                                OID responded = bindings[k].getOid();
                                OID requested =
                                        this.request.toArray()[pointer].getOid();
                                pointer++;
                                if (!responded.startsWith(requested)) {
                                    localResponse.set(k, new VariableBinding(
                                            requested));
                                }
                            }
                        }
                        localResponses[j] = localResponse;
                        if (localResponses[0] != null) {
                            authorizationFailure = false;
                        }
                    }
                }
                if (!moreOIDs && this.operation == GETALL) {
                    break;
                }
                if (localResponses[0] == null && this.operation != GETALL) {
                    return;
                }
                PDU combinedResponse = combine(localResponses);
                if (this.operation == GETALL) {
                    boolean allNull = true;
                    VariableBinding[] bindings = combinedResponse.toArray();
                    for (int i = 0; i < bindings.length; i++) {
                        if (!(bindings[i].getVariable() instanceof Null)) {
                            allNull = false;
                            break;
                        }
                    }
                    if (allNull) {
                        break;
                    }
                }
                allResponses.add(combinedResponse);
                if (this.operation == GETALL) {
                    nextPdu = new PDU(nextPdu);
                    nextPdu.clear();
                    OID[] previous = new OID[combinedResponse.toArray().length];
                    for (int i = 0; i < previous.length; i++) {
                        previous[i] = combinedResponse.toArray()[i].getOid();
                    }
                    nextPdu.addAll(VariableBinding.createFromOIDs(previous));
                }
            } while (moreOIDs);
        } catch (IOException ex) {
            Logger.getLogger(Collector.class.getName()).log(Level.SEVERE,
                                                            null, ex);
        }
        if (!allResponses.isEmpty()) {
            this.responses = allResponses.toArray(new PDU[0]);
        }
    }

    /**
     * Adds a VariableBinding to the request PDU. The VariableBinding is
     * retrieved from the symbol in the MIB. If the symbol is not found, and
     * empty VariableBinding is added to the PDU.
     *
     * @param mib    the MIB.
     * @param symbol the symbol.
     *
     * @return the OID of the symbol.
     */
    public String addRequest(Mib mib, String symbol) {
        OID oid;
        String returnString;
        if (symbol != null) {
            MibValueSymbol valueSymbol = (MibValueSymbol) mib.getSymbol(symbol);
            returnString = valueSymbol.getValue().toString();
            oid = new OID(returnString);

            if (valueSymbol.isScalar()) {
                oid.append(0);
                returnString += ".0";
            }
        } else {
            oid = new OID();
            returnString = null;
        }
        this.request.add(new VariableBinding(oid));
        return returnString;
    }

    /**
     * Returns an array of the response. This is equivalent to calling
     * getResponse().toArry() on the response element.
     *
     * @param response the response
     *
     * @return a VariableBinding[] of the response or null if the operation
     *         hasn't run yet.
     */
    public VariableBinding[] toArray(int response) {
        if (responses != null) {
            return this.responses[response].toArray();
        }
        return null;
    }

    private PDU[] split(PDU toSplit) {
        int pduSize = toSplit.size();
        PDU[] subPdus;
        int maxPduSize = configurationHandler.getPduSize();
        if (pduSize > maxPduSize) {
            int division = pduSize / maxPduSize;
            int arraySize = division;
            int remainder = pduSize % maxPduSize;
            if (remainder > 0) {
                arraySize++;
            }
            subPdus = new PDU[arraySize];
            int i;
            for (i = 0; i < division; i++) {
                subPdus[i] = new PDU(toSplit);
                subPdus[i].clear();
                int pointer = i * maxPduSize;
                for (int j = 0; j < maxPduSize; j++) {
                    subPdus[i].add(toSplit.get(pointer + j));
                }
            }
            if (remainder > 0) {
                subPdus[i] = new PDU(toSplit);
                subPdus[i].clear();
                int pointer = i * maxPduSize;
                for (int j = 0; j < remainder; j++) {
                    subPdus[i].add(toSplit.get(pointer + j));
                }
            }
        } else {
            subPdus = new PDU[1];
            subPdus[0] = toSplit;
        }
        return subPdus;
    }

    private PDU combine(PDU[] subPdus) {
        PDU returnValue = new PDU(subPdus[0]);
        returnValue.clear();
        returnValue.setRequestID(this.request.getRequestID());
        for (int i = 0; i < subPdus.length; i++) {
            for (int j = 0; j < subPdus[i].size(); j++) {
                returnValue.add(subPdus[i].get(j));
            }
        }
        return returnValue;
    }

    /**
     * Returns true if the operation has a response and false otherwise.
     *
     * @return a <code>boolean</code> indicating if there is a response.
     */
    public boolean hasResponses() {
        if (this.responses == null) {
            return false;
        }
        return true;
    }
}
