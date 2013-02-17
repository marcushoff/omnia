package omnia.snmp;

/**
 * This class handles the SNMP pluginFiles. It provides access to the OIDs for
 * different mibPosition types and brands. It loads the MIBs from the mib
 * directory and the XML files from the pluginFiles directory. To gather
 * information from the devices first call the parseOperation method. It will
 * prepare the SNMP operation with the MIBs. After the operation is run (this
 * must be done externally) call the parseTemplate method with the result of the
 * operation. This returns a ElementTemplate[] with the results from the XML
 * file.
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 * @version 1.0
 */
//TODO: update description
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import omnia.ConfigurationHandler;
import omnia.Omnia;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class SnmpPluginHandler {

    /**
     * The MIB directory, should be loaded from the configuration file.
     */
    private MibLoader loader;
    private final ConfigurationHandler configuration =
            Omnia.configurationHandler;
    private SAXBuilder builder;
    private Document defaultDocument;
//    private Document[] plugins;
    private Map<Pattern, Document> objectIds;

    /**
     * Default constructor. Adds all MIBs from the mibs directory and loads the
     * default.xml from the pluginFiles directory.
     */
    public SnmpPluginHandler() {
        loader = new MibLoader();
        //TODO add try catch for mibDir loading
        loader.addAllDirs(new File(configuration.getMibsDir()));
        objectIds =
                Collections.synchronizedMap(new HashMap<Pattern, Document>());
        builder = new SAXBuilder();
        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                if (string.equals(configuration.getDefaultPlugin())) {
                    return false;
                }
                if (string.endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        };
        File[] pluginFiles = new File(configuration.getPluginsDir()).listFiles(
                filter);
//        this.plugins = new Document[pluginFiles.length];
        for (int i = 0; i < pluginFiles.length; i++) {
            try {
                Document document = builder.build(pluginFiles[i]);
                Pattern objectId =
                        Pattern.compile(document.getRootElement().getChild(
                        "capabilities").getChild(
                        "objectId").getTextNormalize());
                objectIds.put(objectId, document);
            } catch (JDOMException ex) {
                Logger.getLogger(SnmpPluginHandler.class.getName()).log(
                        Level.SEVERE,
                        null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SnmpPluginHandler.class.getName()).log(
                        Level.SEVERE,
                        null, ex);
            }

        }
        try {
            //TODO: load all pluginFiles
            File defaultFile = new File(configuration.getPluginsDir() + "/"
                                        + configuration.getDefaultPlugin());

            defaultDocument = builder.build(defaultFile);

        } catch (Exception e) {
            //TODO Handle errors
            System.out.println("Exception : " + e.getMessage());
        }
    }

    /**
     * Loads a MIB file synchronized. This is equal to calling loader.load(),
     * but synchronized.
     *
     * @param mib the MIB to load.
     *
     * @return the first MIB module or null if the MIB could not be loaded.
     */
    protected synchronized Mib load(String mib) {
        try {
            return loader.load(mib);
        } catch (java.io.IOException ioe) {
            //TODO handle exception
            System.out.println(ioe.toString());
        } catch (MibLoaderException mle) {
            //TODO handle exception
            System.out.println(mle.toString());
        }
        return null;
    }

    /**
     * Returns the root element of a document. If document is null, the default
     * root element will be returned.
     *
     * @param document the document
     * @return the root element of the document or the root element of the
     * default document.
     */
    Element getRootElement(Document document) {
        if(document == null) {
            return this.defaultDocument.getRootElement();
        }
        return document.getRootElement();
    }

    Document getDefault() {
        return this.defaultDocument;
    }

    public void setPlugin(CapabilityTemplate capabilities) {
        String objectId = capabilities.getValueAsString(
                CapabilityTemplate.OBJECTID);

        if(objectId == null) {
            capabilities.setDocument(defaultDocument);
            return;
        }
        Iterator<Pattern> iterator = objectIds.keySet().iterator();
        int longestMatch = 0;
        Document match = null;
        while (iterator.hasNext()) {
            Pattern pattern = iterator.next();
            Matcher matcher = pattern.matcher(objectId);
            if (matcher.find()) {
                MatchResult result = matcher.toMatchResult();
                int matchLength = result.end() - result.start();
                if (matchLength > longestMatch) {
                    longestMatch = matchLength;
                    match = objectIds.get(pattern);
                }
            }
        }
        if (match == null) {
            capabilities.setDocument(defaultDocument);
            return;
        }
        capabilities.setDocument(match);
    }
}
