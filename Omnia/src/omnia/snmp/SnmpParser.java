package omnia.snmp;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibType;
import net.percederberg.mibble.MibValue;
import net.percederberg.mibble.MibValueSymbol;
import net.percederberg.mibble.snmp.SnmpObjectType;
import net.percederberg.mibble.type.IntegerType;
import net.percederberg.mibble.value.NumberValue;
import omnia.Omnia;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.snmp4j.PDU;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

/**
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 * @version 1.0
 */
public class SnmpParser {
//TODO: make methods static

    /**
     * Format types.
     */
    private final int VALUE = 0;
    private final int OID = 1;
    private final String MIB_INPUT = "mib";
    private final SnmpPluginHandler pluginHandler = Omnia.snmpPluginHandler;

    public SnmpParser() {
    }

    /**
     * Parse an operation, adding the MIBs to it. The parsing is done based on
     * the capabilities of the mibPosition and the request symbolType of the
     * operation. This must be run to prepare the SnmpOperation with the
     * relevant MIBs. Then the SnmpOperation can be run externally. The result
     * of the SnmpOperation must be parsed with parseTemplate() to get the final
     * template.
     *
     * @param operation    the operation to parse.
     * @param capabilities the capabilities of the mibPosition.
     */
    public void parseOperation(SnmpOperation operation,
                               CapabilityTemplate capabilities) {
        ElementTemplate template = operation.getTemplate();
        for (int i = 0; i < template.size(); i++) {
            Element element = getLevel2(template.template,
                                        template.getElement(i), capabilities);
            if (element != null) {
                String context = element.getTextNormalize();

                Attribute input = element.getAttribute(MIB_INPUT);
                if (input != null) {
                    String mibRef = input.getValue();
                    String mib =
                            getLevel2(MIB_INPUT, mibRef, capabilities).getTextNormalize();
                    String oid = operation.addRequest(pluginHandler.load(mib),
                                                      context);
                    template.setOid(i, oid);
                }
            }
            //TODO: handle malformed xml file
        }
    }

    /**
     * Parse an XML element recursively. After SNMP processing this method is
     * called to parse all element recursively including the MIBs resulting from
     * the SNMP operation. If the element is already set in the template is
     * returns the existing element. Returns null if the elementName does not
     * exist or if XML processing like split, match or switch fail.
     *
     * @param template     the template to parse the element against.
     * @param elementName  the template element to parse against.
     * @param element      the element to parse.
     * @param format       the value of format
     * @param capabilities the value of capabilities
     *
     * @return a String final contents of the element or null if processing
     *         fails.
     */
    private String parseElement(ElementTemplate template, int elementName,
                                Element element, PDU response, int format,
                                CapabilityTemplate capabilities) {
        //TODO: handle new requested tag and element tag
        if(template instanceof CapabilityTemplate) {
            System.out.println();
        }
        if (template.getValue(elementName) != null) {
            /*
             * Element is already set.
             */
            if (format == VALUE) {
                return template.getValueAsString(elementName);
            }
            return template.getOid(elementName);
        }
        String context = element.getTextNormalize();
        if (!element.hasAttributes()) {
            /*
             * Parse pure string
             */
            return context;
        }
        Attribute first = element.getAttributes().get(0);
        String dependencyName = first.getName();
        String dependencyValue = first.getValue();
        if (dependencyValue == null) {
            return null;
        }
        if (dependencyName.equals(MIB_INPUT)) {
            /*
             * Parse mib element
             */
            Mib mib =
                    pluginHandler.load(
                    getLevel2(MIB_INPUT, dependencyValue, capabilities).getTextNormalize());
            MibValueSymbol symbol = (MibValueSymbol) mib.getSymbol(context);
            OID requested = new OID(symbol.getValue().toString());
            Variable oidResponse = response.getVariable(requested);
            List<VariableBinding> vbs = response.getBindingList(requested);
            String textualOid = vbs.get(0).getOid().toString();
            template.setOid(elementName, textualOid);
            if (format == OID) {
                return textualOid;
            }
            if (oidResponse == null || oidResponse instanceof Null) {
                return null;
            }
            /*
             * Check if the symbolValue has a textual MIB represenatation and
             * use that instead.
             */
            MibType symbolType = symbol.getType();
            if (!(symbolType instanceof SnmpObjectType)) {
                return noTextualMib(oidResponse);
            }
            MibType syntax = ((SnmpObjectType) symbolType).getSyntax();
            if (!(syntax instanceof IntegerType)) {
                return noTextualMib(oidResponse);
            }
            IntegerType syntaxInteger = (IntegerType) syntax;
            if (!syntaxInteger.hasSymbols()) {
                return noTextualMib(oidResponse);
            }
            MibValueSymbol[] syntaxSymbols = syntaxInteger.getAllSymbols();
            /*
             * Check if the symbolValue is at the expected mibPosition in the
             * array.
             */
            int mibPosition = oidResponse.toInt();
            MibValue symbolValue;
            String symbolString;
            int symbolPosition;
            if (mibPosition - 1 <= syntaxSymbols.length) {
                symbolValue = syntaxSymbols[mibPosition - 1].getValue();
                if (!(symbolValue instanceof NumberValue)) {
                    return noTextualMib(oidResponse);
                }
                symbolString = ((NumberValue) symbolValue).toString();
                symbolPosition = Integer.parseInt(symbolString);
                //TODO: chatch parsint exception.
                if (symbolPosition == mibPosition) {
                    return syntaxSymbols[mibPosition - 1].getName();
                }
            }
            /*
             * Else run through the entire array to find the symbolValue
             */
            for (int i = 0; i < syntaxSymbols.length; i++) {
                symbolValue = syntaxSymbols[i].getValue();
                if (symbolValue instanceof NumberValue) {
                    symbolString = ((NumberValue) symbolValue).toString();
                    symbolPosition = Integer.parseInt(symbolString);
                    if (symbolPosition == mibPosition) {
                        return syntaxSymbols[i].getName();
                    }
                }
            }
            return noTextualMib(oidResponse);
        }
        /*
         * Parse dependency
         */
        Attribute second = element.getAttributes().get(1);
        String formatName = second.getName();
        String formatValue = second.getValue();
        String dependency;
        int dependencyFormat = VALUE;
        if (formatValue.equals("oid")) {
            dependencyFormat = OID;
        }
        dependency = parseElement(template, elementName,
                                  getLevel2(dependencyName, dependencyValue,
                                            capabilities),
                                  response, dependencyFormat, capabilities);
        return parseProcesses(element, dependency, template, elementName,
                              response, dependencyFormat, null);
    }

    private String parseProcesses(Element element, String dependency,
                                  ElementTemplate template, int elementName,
                                  PDU response, int format,
                                  CapabilityTemplate capabilities)
            throws NumberFormatException {
        Attribute first;
        String dependencyName;
        String dependencyValue;
        /*
         * Parse without process
         */
        if (element.getChildren().isEmpty()) {
            return dependency;
        }
        /*
         * Parse substring process
         */
        Element process = element.getChild("substring");
        if (process != null) {
            if (dependency == null) {
                return null;
            }
            first = process.getAttributes().get(0);
            dependencyName = first.getName();
            if (!dependencyName.equals("start")) {
                return null;
            }
            dependencyValue = first.getValue();
            int start = Integer.parseInt(dependencyValue);
            //TODO catch start out of bounds
            return parseProcesses(process, dependency.substring(start),
                                  template, elementName, response,
                                  format, capabilities);
        }
        /*
         * Parse split process
         */
        process = element.getChild("split");
        if (process != null) {
            if (dependency == null) {
                return null;
            }
            first = process.getAttributes().get(0);
            dependencyName = first.getName();
            dependencyValue = first.getValue();
            int substring = 0;
            if (dependencyName.equals("substring")) {
                substring = Integer.parseInt(dependencyValue);
            }
            String[] substrings = dependency.split(process.getTextNormalize(),
                                                   0);
            if (substring >= substrings.length) {
                return null;
            }
            return parseProcesses(process, substrings[substring], template,
                                  elementName, response, format, capabilities);
        }
        /*
         * Parse switch process
         */
        process = element.getChild("switch");
        if (process != null) {
            Element switchCase = process.getChild(dependency);
            if (switchCase == null) {
                return dependency;
            }
            return parseElement(template, elementName, switchCase, response,
                                format, capabilities);
        }
        /*
         * Parse match process
         */
        process = element.getChild("match");
        if (process != null) {
            Iterator matchChildren = element.getChildren("match").iterator();
            Iterator thenChildren = element.getChildren("then").iterator();
            while (matchChildren.hasNext()) {
                Element match = (Element) matchChildren.next();
                Element then = (Element) thenChildren.next();
                Pattern pattern = Pattern.compile(match.getTextNormalize());
                Matcher matcher = pattern.matcher(dependency);
                if (matcher.find()) {
                    return parseProcesses(process, then.getTextNormalize(),
                                          template, elementName, response,
                                          format, capabilities);
                }
            }
            /*
             * Parse default process
             */
            process = element.getChild("default");
            if (process != null) {
                return parseProcesses(process, process.getTextNormalize(),
                                      template, elementName, response, format,
                                      capabilities);
            }
            return null;
        }
        return null;
    }

    /**
     * Returns the MIB with no textual representation.
     *
     * @param oidResponse the oidResponse to parse.
     *
     * @return a String representation of the MIB.
     */
    private String noTextualMib(Variable variable) {
        try {
            return String.valueOf(variable.toLong());
        } catch (UnsupportedOperationException ex) {
            //TODO: handle exception
            //Logger.getLogger(SnmpOperation.class.getName()).log(Level.SEVERE,
            //                                                  null, ex);
        }
        return variable.toString();
    }

    /**
     * Returns XML level 1 element.
     *
     * @param level1 the element to return.
     *
     * @return the XML level 1 element or null if it doesn't exist.
     */
    private Element getLevel1(String level1, CapabilityTemplate capabilities) {
        if(capabilities == null) {
            return pluginHandler.getRootElement(pluginHandler.getDefault()).getChild(
                    level1);
        }
        Document document = capabilities.getDocument();
        if (document == null) {
            pluginHandler.setPlugin(capabilities);
            document = capabilities.getDocument();
        }
        return pluginHandler.getRootElement(document).getChild(level1);
    }

    /**
     * Returns XML level 2 element.
     *
     * @param level1 the level 1 element.
     * @param level2 the element to return.
     *
     * @return the XML level 2 element or null if it doesn't exist.
     */
    private Element getLevel2(String level1, String level2,
                              CapabilityTemplate capabilities) {
        Element element = getLevel1(level1, capabilities);
        if (element == null) {
            return element;
        }
        return element.getChild(level2);
    }

    public boolean isPhysIf(String ifType) {
        //TODO: implement properly
        /*
         * switch (ifType) { case ethernetCsmacd: return true; case ETHERNET_GE:
         * return true; case ETHERNET_WLAN: return true; case ETHERNET_VLAN:
         * return false; case ppp: return false; case PPPOE: return false; case
         * softwareLoopback: return false; case ieee80211: return true; case
         * bridge: return false; default:
         */
        return true;
//        }
    }

    /**
     * Parse a template based on the operation and capabilities of the
     * mibPosition. Creates the final template for the request of the operation,
     * based on the request symbolType of the operation. Extracts the results of
     * the operation and parses the remaining contents of the template. This
     * method should be called after running the operation from parseOperation()
     *
     * @param operation    the operation that has run.
     * @param capabilities the capabilities of the mibPosition.
     *
     * @return en ElementTemplate[] containing the parsed templates.
     */
    public ElementTemplate[] parseTemplate(SnmpOperation operation,
                                           CapabilityTemplate capbilities) {
        if(operation.getTemplate() instanceof CapabilityTemplate) {
            System.out.println();
        }
        PDU[] responses = operation.getResponses();
        int lengthOfResponses = responses.length;
        ElementTemplate[] returnValue = new ElementTemplate[lengthOfResponses];
        for (int i = 0; i < lengthOfResponses; i++) {
            returnValue[i] = parseAttributes(responses[i],
                                             operation.getTemplate().clone(),
                                             capbilities);
            returnValue[i].setDevice(operation.getAddress());
        }
        return returnValue;
    }

    /**
     * Generically parses all attributes of an ElementTemplate. Parses each
     * element in the template.
     *
     * @param response     the response of the SnmpOperation.
     * @param template     the template to parse.
     * @param capabilities the value of capabilities
     *
     * @return the parsed template
     */
    private ElementTemplate parseAttributes(PDU response,
                                            ElementTemplate template,
                                            CapabilityTemplate capabilities) {
        for (int i = 0; i < template.size(); i++) {
            Element element = getLevel2(template.template,
                                        template.getElement(i), capabilities);
            if (element != null) {
                template.setValue(i, parseElement(template, i, element,
                                                  response, VALUE, capabilities));
            }
        }
        return template;
    }
}
