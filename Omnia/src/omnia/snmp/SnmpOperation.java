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
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * This class does the actual SNMP operation. The operation is loaded with the
 * PDU, target, transport mapping and the operation type. When these are loaded
 * run() is called to run the actual SNMP operation. When the operation finishes
 * it calls the onStop() of the OperationListener.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class SnmpOperation implements Runnable {

    /**
     * Shorthand for the configurationHandler.
     */
    private final ConfigurationHandler configurationHandler = Omnia.configurationHandler;
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
     * The community target for the SNMP.
     */
    private CommunityTarget target;
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
     * Short constructor. Sets the target, template and listener and initializes
     * the operation.
     *
     * @param target   the target.
     * @param template the template.
     * @param listener the listener.
     */
    public SnmpOperation(CommunityTarget target, ElementTemplate template,
                         OperationListener listener) {
        this.target = target;
        this.template = template;
        this.listener = listener;
        this.operation = template.getOperation();
        initialize();
    }

    /**
     * Long constructor. Sets the operation, request, target and listener and
     * initializes the operation.
     *
     * @param operation the operation type.
     * @param template  the template.
     * @param target    the target.
     * @param listener  the listener.
     */
    public SnmpOperation(int operation, ElementTemplate template, CommunityTarget target,
                         OperationListener listener) {
        this.operation = operation;
        this.listener = listener;
        this.template = template;
        this.operation = template.getOperation();
        this.target = target;
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
     * Returns the target.
     *
     * @return the CommunityTarget containing the target.
     */
    public CommunityTarget getTarget() {
        return this.target;
    }

    /**
     * Returns the address of the device.
     *
     * @return an Address of the device.
     */
    public Address getAddress() {
        return this.target.getAddress();
    }

    /**
     * Sets the target of the device.
     *
     * @param target the target.
     */
    public void setTarget(CommunityTarget target) {
        this.target = target;
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
     * Sets the operation to perform.
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
        switch (this.operation) {
            case GET:
                runGet();
                break;
            case GETNEXT:
                runGetNext();
                break;
            case GETALL:
                runGetAll();
                break;
            default:

            //TODO handle error
        }
        this.listener.onStop(this);
    }

    private void runGet() {
        if (this.request == null) {
            return;
        }
        this.request.setType(PDU.GET);
        PDU[] requests = split(this.request);
        PDU[] localResponses = new PDU[requests.length];
        try {
            UdpAddress adr = (UdpAddress) getAddress();
            for (int i = 0; i < requests.length; i++) {
                ResponseEvent response = this.snmp.send(requests[i], this.target);
                if (response.getPeerAddress() != null) {
                    localResponses[i] = response.getResponse();
                }
            }
            if (localResponses[0] == null) {
                return;
            }
            this.responses = new PDU[1];
            this.responses[0] = combine(localResponses);
        } catch (Exception ex) {
            //TODO: handle exception
            //  Logger.getLogger(Collector.class.getName()).log(Level.SEVERE,
//                                                                null, ex);
        }

    }

    private void runGetNext() {
        if (this.request == null) {
            return;
        }
        this.request.setType(PDU.GETNEXT);
        PDU[] requests = split(this.request);
        PDU[] localResponses = new PDU[requests.length];
        try {
            for (int i = 0; i < requests.length; i++) {
                ResponseEvent response = this.snmp.send(requests[i], this.target);
                if (response.getPeerAddress() != null) {
                    localResponses[i] = response.getResponse();
                }
            }
            if (localResponses[0] == null) {
                return;
            }
            this.responses = new PDU[1];
            this.responses[0] = combine(localResponses);
        } catch (IOException ex) {
            Logger.getLogger(Collector.class.getName()).log(Level.SEVERE,
                                                            null, ex);
        }
    }

    private void runGetAll() {
        if (this.request == null) {
            return;
        }
        this.request.setType(PDU.GETNEXT);
        PDU nextPdu = this.request;
        ArrayList<PDU> allResponses = new ArrayList<PDU>();
        try {
            boolean more = true;
            do {
                int pointer = 0;
                PDU[] requests = split(nextPdu);
                PDU[] localResponses = new PDU[requests.length];
                for (int i = 0; i < requests.length; i++) {
                    ResponseEvent response = this.snmp.send(requests[i],
                                                            this.target);
                    if (response.getPeerAddress() == null) {
                        more = false;
                        break;
                    }
                    PDU localResponse = response.getResponse();
                    VariableBinding[] bindings = localResponse.toArray();
                    if (bindings.length <= 0) {
                        more = false;
                        break;
                    }
                    for (int j = 0; j < bindings.length; j++) {
                        OID responded = bindings[j].getOid();
                        OID requested = this.request.toArray()[pointer].getOid();
                        pointer++;
                        if (!responded.startsWith(requested)) {
                            localResponse.set(j,
                                              new VariableBinding(requested));
                        }
                    }
                    localResponses[i] = localResponse;
                }
                if (!more) {
                    break;
                }
                PDU combinedResponse = combine(localResponses);
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
                allResponses.add(combinedResponse);
                nextPdu = new PDU(nextPdu);
                nextPdu.clear();
                OID[] previous = new OID[combinedResponse.toArray().length];
                for (int i = 0; i < previous.length; i++) {
                    previous[i] = combinedResponse.toArray()[i].getOid();
                }
                nextPdu.addAll(VariableBinding.createFromOIDs(previous));
            } while (more);
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
     * @return a VariableBinding[] of the response or null the operation hasn't
     *         run yet.
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

    public boolean hasResponses() {
        if (this.responses == null) {
            return false;
        }
        return true;
    }
}
