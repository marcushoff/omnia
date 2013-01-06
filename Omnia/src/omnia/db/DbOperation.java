package omnia.db;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import omnia.Omnia;
import omnia.db.DbHandler.RelTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.UniqueFactory;

/**
 * This class provides a complete operation on the database. It locks down all
 * elements referenced for this operation, so that other operations cannot work
 * on them. Therefore it is important to call close(), when the operation is
 * finished. Otherwise there might be a deadlock in the database.
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 * @version 1.0
 */
public class DbOperation {

    private final DbHandler dbHandler = Omnia.dbHandler;
    private Transaction tx;
    private Index<Node> devices;
    private ArrayList<Node> nodes;
    private ArrayList<Relationship> relationships;

    public DbOperation() {
        tx = dbHandler.beginTx();
        devices = dbHandler.getIndex("devices");
        nodes = new ArrayList<Node>();
        relationships = new ArrayList<Relationship>();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        System.out.println("Closing operation " + tx.toString());
        tx.finish();
    }

    /**
     * Sets a <it>setHas</it> relationship between two nodes. If the
     * relationship already exists, nothing is done.
     *
     * @param from reference to the <it>from</it> node.
     * @param to   reference to the <it>to</it> node.
     */
    public void setHas(int from, int to) {
        Iterator<Relationship> relationships =
                getRelationships(from, RelTypes.HAS, Direction.OUTGOING);

        while (hasNext(relationships)) {
            if (getId(getEndNode(next(relationships))) == getId(to)) {
                return;
            }
        }
        createRelationshipBetween(from, to, RelTypes.HAS);
    }

    private boolean isSameNode(int node, int otherNode) {
        return getId(node) != getId(otherNode);
    }

    private boolean lock(int node) {
        try {
            tx.acquireWriteLock(nodes.get(node));
            System.out.println("Locked node " + getId(node) + " "
                               + tx.toString());
            return true;
        } catch (NotFoundException ex) {
            System.out.println("Failed lock on node " + getId(node) + " "
                               + tx.toString());
            return false;
        }
    }

    private void success() {
        tx.success();
    }

    private void failure() {
        System.out.println("Transaction failed: " + tx.toString());
        tx.failure();
    }

    /**
     * Sets a <it>setCable</it> relationship between two nodes. If the
     * relationship already exists, nothing is done.
     *
     * @param ifOne reference to the first interface node.
     * @param ifTwo reference to the second interface node.
     */
    public void setCable(int ifOne, int ifTwo) {
        Iterator<Relationship> relationships =
                getRelationships(ifOne, DbHandler.RelTypes.CABLE, Direction.BOTH);
        while (hasNext(relationships)) {
            int[] nodes = getNodes(next(relationships));
            if ((getId(nodes[0]) == getId(ifOne) && getId(nodes[1]) == getId(
                    ifTwo)) || (getId(nodes[0]) == getId(ifTwo) && getId(
                    nodes[1]) == getId(ifOne))) {
                return;
            }
        }
        createRelationshipBetween(ifOne, ifTwo, DbHandler.RelTypes.CABLE);
    }

    /**
     * Deletes a node and all its relationships.
     *
     * @param node the node to delete.
     */
    public void delete(int node) {
        System.out.println("Deleting node " + getId(node) + " " + tx.toString());
        try {
            dbHandler.delete(dbHandler.getDb(), nodes.get(node));
            success();
        } catch (Exception ex) {
            failure();
        }
    }

    /**
     * Updates the property of a node. Sets the property/value pair for a node
     * with a given id. See setProperty() for valid types of the value
     * parameter.
     *
     * @param node     the node handle.
     * @param property the property.
     * @param value    the value of the property.
     */
    public void update(int node, String property, Object value) {
        if (property != null && value != null) {
            setProperty(node, property, value);
            return;
        }
        if (property != null) {
            removeProperty(node, property);
        }
    }

    private boolean hasProperty(int device, String property) {
        System.out.println("Checking property " + property + " on node "
                           + getId(device) + " " + tx.toString());
        return nodes.get(device).hasProperty(property);
    }

    private void setProperty(int node, String property, Object value) {
        try {
            nodes.get(node).setProperty(property, value);
            System.out.println("Setting property " + property + " for node " + getId(node) + " " + tx.toString());
            success();
        } catch (Exception ex) {
            failure();
        }
    }

    private int getOtherNode(int relationship, int node) {
        boolean lock = false;
        int nodeId = -1;
        while (!lock) {
            Node otherNode = relationships.get(relationship).getOtherNode(
                    nodes.get(node));
            nodes.add(otherNode);
            nodeId = nodes.indexOf(otherNode);
            lock = lock(nodeId);
            if (!lock) {
                nodes.remove(nodeId);
            }
        }
        return nodeId;
    }

    private int getEndNode(int relationship) {
        boolean lock = false;
        int nodeId = -1;
        while (!lock) {
            Node endNode = relationships.get(relationship).getEndNode();
            nodes.add(endNode);
            nodeId = nodes.indexOf(endNode);
            lock(nodeId);
            if (!lock) {
                nodes.remove(nodeId);
            }
        }
        return nodeId;
    }

    private int[] getNodes(int relationship) {
        boolean[] locks = {false, false};
        int[] returnIds = {-1, -1};
        while (!locks[0] && !locks[1]) {
            Node[] nodes = relationships.get(relationship).getNodes();
            for (int i = 0; i < nodes.length; i++) {
                this.nodes.add(nodes[i]);
                returnIds[i] = this.nodes.indexOf(nodes[i]);
                locks[i] = lock(returnIds[i]);
                if (!locks[i]) {
                    this.nodes.remove(returnIds[i]);
                }
            }
        }
        return returnIds;
    }

    private int next(Iterator<Relationship> relationships) {
        boolean lock = false;
        int relId = -1;
        while (!lock) {
            Relationship relationship = relationships.next();
            this.relationships.add(relationship);
            relId = this.relationships.indexOf(relationship);
            lock = lock(relId);
            if (!lock) {
                this.relationships.remove(relId);
                if (!hasNext(relationships)) {
                    lock = true;
                    relId = -1;
                }
            }
        }
        return relId;
    }

    private boolean hasNext(Iterator<Relationship> relationships) {
        return relationships.hasNext();
    }

    private Iterator<Relationship> getRelationships(int node) {
        return nodes.get(node).getRelationships().iterator();
    }

    private Iterator<Relationship> getRelationships(int node, RelTypes type,
                                                    Direction direction) {
        return nodes.get(node).getRelationships(type, direction).iterator();
    }

    /**
     * Returns a property of a node.
     *
     * @param node     the node.
     * @param property the property.
     *
     * @return the value of the property or null if not found.
     */
    public Object getProperty(int node, String property) {
        Object returnProperty;
        try {
            returnProperty = nodes.get(node).getProperty(property);
        } catch (NotFoundException ex) {
            returnProperty = null;
        }
        return returnProperty;
    }

    private void removeProperty(int node, String property) {
        System.out.println("Removing property " + property + " from node "
                           + getId(node) + " " + tx.toString());
        try {
            nodes.get(node).removeProperty(property);
            success();
        } catch (Exception ex) {
            failure();
        }
    }

    private Iterator<String> getPropertyKeys(int node) {
        return nodes.get(node).getPropertyKeys().iterator();
    }

    private long getId(int node) {
        return nodes.get(node).getId();
    }

    private int getAndLock(UniqueFactory<Node> factory, String key,
                           Object value) {
        boolean lock = false;
        Node node;
        int nodeId = -1;
        while (!lock) {
            node = factory.getOrCreate(key, value);
            nodes.add(node);
            nodeId = nodes.indexOf(node);
            lock = lock(nodeId);
            if (!lock) {
                nodes.remove(nodeId);
            } else {
                try {
                    //Check if the node is writable ie. has not been deleted
                    setProperty(nodeId, key, value);
                } catch (NotFoundException ex) {
                    lock = false;
                }
            }
        }
        System.out.println("Created node " + getId(nodeId) + " " + tx.toString());
        return nodeId;
    }

    private void createRelationshipBetween(int nodeOne, int nodeTwo, RelTypes type) {
        try {
            nodes.get(nodeOne).createRelationshipTo(nodes.get(nodeTwo), type);
            System.out.println("Created relationship " + type.name() + " between nodes " + getId(nodeOne) + ", " + getId(nodeTwo) + " " + tx.toString());
            success();
        } catch (Exception ex) {
            failure();
        }
    }

    /**
     * Creates a new node in the database. Returns a handle to the new node. See
     * setProperty() for valid types of the name parameter.
     *
     * @param type the node type to create.
     *
     * @return a handle to the node
     */
    public int getOrCreateDevice(String chassisId, String snmpAddress) {
        UniqueFactory<Node> factory =
                new UniqueFactory.UniqueNodeFactory(dbHandler.getDb(), "devices") {

                    @Override
                    protected void initialize(Node created,
                                              Map<String, Object> properties) {
                        Object snmpAddress = properties.get("snmpAddress");
                        if (snmpAddress != null) {
                            created.setProperty("snmpAddress", snmpAddress);
                        }
                        Object chassisId = properties.get("chassisId");
                        if (chassisId != null) {
                            created.setProperty("chassisId", chassisId);
                        }
                    }
                };
        int device = -1;
        if (chassisId != null && snmpAddress != null) {
            device = getAndLock(factory, "chassisId", chassisId);
            int otherDevice = getAndLock(factory, "snmpAddress", snmpAddress);
            if (isSameNode(otherDevice, device)) {
                Iterator<String> properties = getPropertyKeys(otherDevice);
                while (properties.hasNext()) {
                    String property = properties.next();
                    if (!hasProperty(device, property)) {
                        setProperty(device, property, getProperty(otherDevice,
                                                                  property));
                    }
                }
                Iterator<Relationship> relationships = getRelationships(
                        otherDevice);
                while (hasNext(relationships)) {
                    int relationship = next(relationships);
                    int otherInterface = getOtherNode(relationship,
                                                      otherDevice);
                    Object index = null;
                    Object alias = null;
                    Object nameX = null;
                    Object portnumber = null;
                    if (hasProperty(otherInterface, "index")) {
                        index = getProperty(otherInterface, "index");
                    }
                    if (hasProperty(otherInterface, "alias")) {
                        alias = getProperty(otherInterface, "alias");
                    }
                    if (hasProperty(otherInterface, "nameX")) {
                        nameX = getProperty(otherInterface, "nameX");
                    }
                    if (hasProperty(otherInterface, "portnumber")) {
                        portnumber =
                                getProperty(otherInterface, "portnumber");
                    }
                    int thisInterface =
                            getOrCreateInterface(device, index, alias, nameX,
                                                 portnumber);
                    properties = getPropertyKeys(otherInterface);
                    while (properties.hasNext()) {
                        String property = properties.next();
                        if (!hasProperty(thisInterface, property)) {
                            setProperty(device, property, getProperty(
                                    otherInterface, property));
                        }
                    }
                    Iterator<Relationship> otherRelationships =
                            getRelationships(otherInterface);
                    while (hasNext(otherRelationships)) {
                        int relNode = getOtherNode(next(otherRelationships),
                                                   otherInterface);
                        setCable(thisInterface, relNode);
                    }
                }
                if (hasProperty(otherDevice, "indexName")) {
                    String indexName = (String) getProperty(otherDevice,
                                                            "indexName");
                    dbHandler.getDb().index().forNodes(indexName).delete();
                }
                devices.remove(nodes.get(otherDevice));
                delete(otherDevice);
                devices.add(nodes.get(device), "snmpAddress", snmpAddress);
            }

        } else if (chassisId != null) {
            device = getAndLock(factory, "chassisId", chassisId);
//            update(device, "snmpAddress", snmpAddress);
            if (!hasProperty(device, "indexName")) {
                update(device, "indexName", chassisId);
            }
        } else if (snmpAddress != null) {
            device = getAndLock(factory, "snmpAddress", snmpAddress);
//            update(device, "chassisId", chassisId);
            if (!hasProperty(device, "indexName")) {
                update(device, "indexName", snmpAddress);
            }
        }
        return device;
    }

    public int getOrCreateInterface(int device, Object index, Object alias,
                                    Object nameX, Object portnumber) {
        Index deviceIndex = dbHandler.getIndex(
                getProperty(device,
                            "indexName").toString());

        UniqueFactory<Node> factory =
                new UniqueFactory.UniqueNodeFactory(dbHandler.getDb(),
                                                    deviceIndex.getName()) {

                    @Override
                    protected void initialize(Node created,
                                              Map<String, Object> properties) {
                        Object index = properties.get("index");
                        if (index != null) {
                            created.setProperty("index", index);
                        }
                        Object alias = properties.get("alias");
                        if (alias != null) {
                            created.setProperty("alias", alias);
                        }
                        Object nameX = properties.get("nameX");
                        if (nameX != null) {
                            created.setProperty("nameX", nameX);
                        }
                        Object portnumber = properties.get("portnumber");
                        if (portnumber != null) {
                            created.setProperty("portnumber", portnumber);
                        }
                    }
                };
        int indexes;
        int aliases;
        int names;
        int portnumbers;
        if (index != null) {
            indexes = deviceIndex.get("index", index).size();
        } else {
            indexes = 0;
        }
        if (alias != null) {
            aliases = deviceIndex.get("alias", alias).size();
        } else {
            aliases = 0;
        }
        if (nameX != null) {
            names = deviceIndex.get("nameX", nameX).size();
        } else {
            names = 0;
        }
        if (portnumber != null) {
            portnumbers = deviceIndex.get("portnumber", portnumber).size();
        } else {
            portnumbers = 0;
        }
        int all = indexes + aliases + names + portnumbers;

        int returnIf = -1;
        if (index != null && (indexes > 0 || all == 0)) {
            returnIf = getAndLock(factory, "index", index);
            update(returnIf, "alias", alias);
            update(returnIf, "nameX", nameX);
            update(returnIf, "portnumber", portnumber);
        } else if (alias != null && (aliases > 0 || all == 0)) {
            returnIf = getAndLock(factory, "alias", alias);
            update(returnIf, "index", index);
            update(returnIf, "nameX", nameX);
            update(returnIf, "portnumber", portnumber);
        } else if (nameX != null && (names > 0 || all == 0)) {
            returnIf = getAndLock(factory, "nameX", nameX);
            update(returnIf, "alias", alias);
            update(returnIf, "index", index);
            update(returnIf, "portnumber", portnumber);
        } else if (portnumber != null) {
            returnIf = getAndLock(factory, "portnumber", portnumber);
            update(returnIf, "alias", alias);
            update(returnIf, "nameX", nameX);
            update(returnIf, "index", index);
        }
        return returnIf;
    }
}
