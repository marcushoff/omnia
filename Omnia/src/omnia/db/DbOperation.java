package omnia.db;

import java.util.ArrayList;
import java.util.Iterator;
import omnia.Omnia;
import omnia.db.DbHandler.RelTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;

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
    private ArrayList<Relationship> lockedRelationships;
    private ArrayList<Node> deletedNodes;
    private ArrayList<Node> lockedNodes;

    public DbOperation() {
        tx = dbHandler.beginTx();
        devices = dbHandler.getIndex("devices");
        deletedNodes = new ArrayList<Node>();
        lockedNodes = new ArrayList<Node>();
        lockedRelationships = new ArrayList<Relationship>();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        System.out.println("Closing operation " + tx.toString());
        deepDelete();
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
        Iterator<Relationship> hasIterator =
                getRelationships(from, RelTypes.HAS, Direction.OUTGOING);

        while (hasNext(hasIterator)) {
            if (getId(getEndNode(next(hasIterator))) == getId(to)) {
                //TODO: catch NotFoundEx
                return;
            }
        }
        createRelationshipBetween(from, to, RelTypes.HAS);
    }

    private boolean isSameNode(int node, int otherNode) {
        return getId(node) != getId(otherNode);
    }

    /**
     * Locks a node. Throws NotFoundException, if the node does not exist.
     *
     * @param node the node to lock
     */
    private void lock(Node node) {
        if (lockedNodes.contains(node)) {
            return;
        }
        tx.acquireWriteLock(node);
        System.out.println("Locked node " + node.getId() + " "
                           + tx.toString());
        lockedNodes.add(node);
    }

    /**
     * Locks a node. Throws NotFoundException, if the node does not exist.
     *
     * @param node the node to lock
     */
    private void lock(Relationship relationship) {
        if (lockedRelationships.contains(relationship)) {
            return;
        }
        tx.acquireWriteLock(relationship);
        System.out.println("Locked relationship " + relationship.getId() + " "
                           + tx.toString());
        lockedRelationships.add(relationship);
    }

    private void success() {
        tx.success();
    }

    private void failure() {
        //TODO: rewrite success and failure, to communicate state to invocing analyze classes
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
        Iterator<Relationship> cableIterator =
                getRelationships(ifOne, DbHandler.RelTypes.CABLE, Direction.BOTH);
        while (hasNext(cableIterator)) {
            int[] cableNodesIs = getNodes(next(cableIterator));
            //TODO: catch NotFoundEx x2
            if ((getId(cableNodesIs[0]) == getId(ifOne) && getId(cableNodesIs[1])
                                                           == getId(ifTwo))
                || (getId(cableNodesIs[0]) == getId(ifTwo)
                    && getId(cableNodesIs[1]) == getId(ifOne))) {
                return;
            }
        }
        createRelationshipBetween(ifOne, ifTwo, DbHandler.RelTypes.CABLE);
    }

    /**
     * Clear a node of all its properties and lockedRelationships.
     *
     * @param node the node to clear.
     */
    public void clear(int node) {
        System.out.println("Clearing node " + getId(node) + " " + tx.toString());
        try {
            dbHandler.clear(lockedNodes.get(node));
        } catch (Exception ex) {
            failure();
        }
    }

    public void delete(int node) {
        deletedNodes.add(lockedNodes.get(node));
    }

    private void deepDelete() {
        for (int i = 0; i < deletedNodes.size(); i++) {
            Node deletedNode = deletedNodes.get(i);
            System.out.println("Deleting node " + deletedNode.getId() + " "
                               + tx.toString());
            try {
                DbHandler.delete(tx, deletedNode);
                success();
            } catch (Exception ex) {
                failure();
            }
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
        return lockedNodes.get(device).hasProperty(property);
    }

    private boolean setProperty(int node, String property, Object value) throws
            IllegalStateException {
        try {
            lockedNodes.get(node).setProperty(property, value);
            System.out.println("Setting property " + property + " for node "
                               + getId(node) + " " + tx.toString());
            success();
            return true;
        } catch (IllegalArgumentException ex) {
            failure();
            return false;
        }
    }

    private int getRelationshipNode(int relationship, int node) {
        Node otherNode =
                lockedRelationships.get(relationship).getOtherNode(lockedNodes.get(
                node));
        lock(otherNode);
        return lockedNodes.indexOf(otherNode);
    }

    private int getEndNode(int relationship) {
        Node endNode = lockedRelationships.get(relationship).getEndNode();
        lock(endNode);
        return lockedNodes.indexOf(endNode);
    }

    private int[] getNodes(int relationship) {
        int[] returnIds = {-1, -1};
        Node[] relNodes = lockedRelationships.get(relationship).getNodes();
        for (int i = 0; i < relNodes.length; i++) {
            lock(relNodes[i]);
            returnIds[i] = lockedNodes.indexOf(relNodes[i]);
        }
        return returnIds;
    }

    private int next(Iterator<Relationship> relationships) {
        if (!hasNext(relationships)) {
            return -1;
        }
        int relId = -1;
        Relationship relationship = relationships.next();
        lock(relationship);
        return lockedRelationships.indexOf(relationship);
    }

    private boolean hasNext(Iterator<Relationship> relationships) {
        return relationships.hasNext();
    }

    private Iterator<Relationship> getRelationships(int node) {
        return lockedNodes.get(node).getRelationships().iterator();
    }

    private Iterator<Relationship> getRelationships(int node, RelTypes type,
                                                    Direction direction) {
        return lockedNodes.get(node).getRelationships(type, direction).iterator();
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
            returnProperty = lockedNodes.get(node).getProperty(property);
        } catch (NotFoundException ex) {
            returnProperty = null;
        }
        return returnProperty;
    }

    private void removeProperty(int node, String property) {
        System.out.println("Removing property " + property + " from node "
                           + getId(node) + " " + tx.toString());
        try {
            lockedNodes.get(node).removeProperty(property);
            success();
        } catch (Exception ex) {
            failure();
        }
    }

    private Iterator<String> getPropertyKeys(int node) {
        return lockedNodes.get(node).getPropertyKeys().iterator();
    }

    private long getId(int node) {
        return lockedNodes.get(node).getId();
    }

    private int getOrCreate(String indexName, String key, Object value) {
        //TODO: Need to test if pessimistic locking works
        Index<Node> index = dbHandler.getIndex(indexName);
        Node node = index.get(key, value).getSingle();
        if (node != null) {
            lock(node);
            //TODO: catch NotFoundEx
            if (deletedNodes.contains(node)) {
                undelete(node);
            }
            return lockedNodes.indexOf(node);
        }
        Node created = dbHandler.createNode();
        lock(created);
        //TODO: catch NotFoundEx
        node = index.putIfAbsent(created, key, value);
        if (node == null) {
            System.out.println("Created node " + created.getId() + " "
                               + tx.toString());
            return lockedNodes.indexOf(created);
        }
        lock(node);
        //TODO: catch NotFoundEx
        delete(lockedNodes.indexOf(created));
        return lockedNodes.indexOf(node);
    }

    private void createRelationshipBetween(int nodeOne, int nodeTwo,
                                           RelTypes type) {
        try {
            lockedNodes.get(nodeOne).createRelationshipTo(lockedNodes.get(nodeTwo), type);
            System.out.println("Created relationship " + type.name()
                               + " between nodes " + getId(nodeOne) + ", "
                               + getId(nodeTwo) + " " + tx.toString());
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
        int device = -1;
        if (chassisId != null && snmpAddress != null) {
            device = getOrCreate("devices", "chassisId", chassisId);
            //TODO: tjek return -1
            int otherDevice = getOrCreate("devices", "snmpAddress", snmpAddress);
            //TODO: tjek return -1
            if (isSameNode(otherDevice, device)) {
                Iterator<String> properties = getPropertyKeys(otherDevice);
                while (properties.hasNext()) {
                    String property = properties.next();
                    if (!hasProperty(device, property)) {
                        setProperty(device, property, getProperty(otherDevice,
                                                                  property));
                    }
                }
                Iterator<Relationship> relIterator = getRelationships(
                        otherDevice);
                while (hasNext(relIterator)) {
                    int relationship = next(relIterator);
                    //TODO: tjek return -1
                    int otherInterface = getRelationshipNode(relationship,
                                                             otherDevice);
                    //TODO: catch NotFoundEx
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
                        int relNode = getRelationshipNode(next(
                                otherRelationships),
                                                          otherInterface);
                        //TODO: catch NotFoundEx x2
                        setCable(thisInterface, relNode);
                    }
                }
                if (hasProperty(otherDevice, "indexName")) {
                    String indexName = (String) getProperty(otherDevice,
                                                            "indexName");
                    dbHandler.getIndex(indexName).delete();
                }
                devices.remove(lockedNodes.get(otherDevice));
                clear(otherDevice);
                devices.add(lockedNodes.get(device), "snmpAddress", snmpAddress);
            }
        } else if (chassisId != null) {
            device = getOrCreate("devices", "chassisId", chassisId);
            //TODO: tjek return -1
//            update(device, "snmpAddress", snmpAddress);
            if (!hasProperty(device, "indexName")) {
                update(device, "indexName", chassisId);
            }
        } else if (snmpAddress != null) {
            device = getOrCreate("devices", "snmpAddress", snmpAddress);
            //TODO: tjek return -1
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
                getProperty(device, "indexName").toString());
        int indexes = 0;
        int aliases = 0;
        int names = 0;
        int portnumbers = 0;
        if (index != null) {
            indexes = deviceIndex.get("index", index).size();
        }
        if (alias != null) {
            aliases = deviceIndex.get("alias", alias).size();
        }
        if (nameX != null) {
            names = deviceIndex.get("nameX", nameX).size();
        }
        if (portnumber != null) {
            portnumbers = deviceIndex.get("portnumber", portnumber).size();
        }
        int all = indexes + aliases + names + portnumbers;

        int returnIf = -1;
        if (index != null && (indexes > 0 || all == 0)) {
            returnIf = getOrCreate(deviceIndex.getName(), "index", index);
            //TODO: tjek return -1
            update(returnIf, "alias", alias);
            update(returnIf, "nameX", nameX);
            update(returnIf, "portnumber", portnumber);
        } else if (alias != null && (aliases > 0 || all == 0)) {
            returnIf = getOrCreate(deviceIndex.getName(), "alias", alias);
            //TODO: tjek return -1
            update(returnIf, "index", index);
            update(returnIf, "nameX", nameX);
            update(returnIf, "portnumber", portnumber);
        } else if (nameX != null && (names > 0 || all == 0)) {
            returnIf = getOrCreate(deviceIndex.getName(), "nameX", nameX);
            //TODO: tjek return -1
            update(returnIf, "alias", alias);
            update(returnIf, "index", index);
            update(returnIf, "portnumber", portnumber);
        } else if (portnumber != null) {
            returnIf = getOrCreate(deviceIndex.getName(), "portnumber",
                                   portnumber);
            //TODO: tjek return -1
            update(returnIf, "alias", alias);
            update(returnIf, "nameX", nameX);
            update(returnIf, "index", index);
        }
        return returnIf;
    }

    private void undelete(Node node) {
        deletedNodes.remove(node);
    }
}
