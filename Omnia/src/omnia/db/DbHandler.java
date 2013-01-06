package omnia.db;

/**
 * This class is an abstraction layer for accessing the database. All other
 * classes must write and read to the database through this class.
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 * @version 1.0
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.neo4j.cypher.EntityNotFoundException;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.tooling.GlobalGraphOperations;

public class DbHandler {

    private final EmbeddedGraphDatabase db;
//    private AutoIndexer<Node> nodeAutoIndexer;
    private final String DEVICESERIALINDEX = "serial";
    private final WrappingNeoServerBootstrapper srv;
    private Index<Node> devices;
    //  private Index<Node> interfaces;

    protected EmbeddedGraphDatabase getDb() {
        return db;
    }

    Index getIndex(String index) {
        return db.index().forNodes(index);
    }

    Transaction beginTx() {
        return db.beginTx();
    }

    protected static enum RelTypes implements RelationshipType {

        HAS,
        CABLE
    }

    public DbHandler() {
        db =
                (EmbeddedGraphDatabase) new GraphDatabaseFactory().newEmbeddedDatabase(
                "db");
        registerShutdownHook(db);
        Iterator allNodes = db.getAllNodes().iterator();
        long nodecount = 0;
        while (allNodes.hasNext()) {
            nodecount++;
        }
        System.out.println("Node count before drop:" + nodecount);
        dropDb(db);
        dropIndexs(db);
        allNodes = db.getAllNodes().iterator();
        nodecount = 0;
        while (allNodes.hasNext()) {
            nodecount++;
        }
        System.out.println("Node count after drop:" + nodecount);
        Configurator config;
        config = new ServerConfigurator(db);

        config.configuration().setProperty(
                Configurator.WEBSERVER_PORT_PROPERTY_KEY, 7575);
        srv = new WrappingNeoServerBootstrapper(db, config);
        srv.start();
//        nodeAutoIndexer = db.index().getNodeAutoIndexer();
//        nodeAutoIndexer.startAutoIndexingProperty(DEVICESERIALINDEX);
//        nodeAutoIndexer.setEnabled(true);
        devices = db.index().forNodes("devices");
//        interfaces = index.forNodes("interfaces");
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        // Deletes all entries to the database.
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                dropDb(graphDb);
                dropIndexs(graphDb);
                graphDb.shutdown();
            }
        });
    }

    private static <T> Object[] copyIterator(Iterator<T> iterator) {
        List<T> copy = new ArrayList<T>();
        try {
            while (iterator.hasNext()) {
                copy.add(iterator.next());
            }
        } catch (EntityNotFoundException ex) {
            return copy.toArray();

        }
        return copy.toArray();
    }

    protected static void delete(GraphDatabaseService graphDb, Node node) {
        Long id = node.getId();
        Transaction tx = graphDb.beginTx();
        try {
            Iterator<Relationship> relationships =
                    node.getRelationships().iterator();
            while (relationships.hasNext()) {
                relationships.next().delete();
            }
            node.delete();
            tx.success();
        } finally {
            tx.finish();
        }
        System.out.println("Deleted node " + id);
    }

    private static void dropDb(GraphDatabaseService graphDb) {
        System.out.println("Dropping database...");
        Iterator<Node> allNodes =
                GlobalGraphOperations.at(graphDb).getAllNodes().
                iterator();
        while (allNodes.hasNext()) {
            delete(graphDb, allNodes.next());
        }
        System.out.println("Database dropped");
    }

    private static void dropIndexs(GraphDatabaseService graphDb) {
        System.out.println("Dropping indexes...");
        IndexManager index = graphDb.index();
        String[] indexNames = index.nodeIndexNames();
        for (int i = 0; i < indexNames.length; i++) {
            Index thisIndex = index.forNodes(indexNames[i]);
            if (thisIndex.isWriteable()) {
                Transaction tx = graphDb.beginTx();
                try {
                    System.out.println("Deleting index " + indexNames[i]);
                    thisIndex.delete();
                    tx.success();
                } finally {
                    tx.finish();
                }
            }
        }
        System.out.println("Indexes dropped");
    }

    /**
     * Creates a new node in the database and adds a <it>setHas</it>
     * relationship from an existing node. If the existing node already setHas a
     * <it>setHas</it> relationship with a node with that type/name pair nothing
     * is created. Returns the id of the node new node. See setProperty() for
     * valid types.
     *
     * @param id   the id of the existing node.
     * @param type the node type to create.
     * @param name the name of the new node.
     *
     * @return the id of the created node or of the existing node if found.
     */
//    public Object createHas(Object id, String type, Object name) {
//        Transaction tx = db.beginTx();
//        Node found =
//                nodeAutoIndexer.getAutoIndex().query(type, name).getSingle();
//        Iterable<Relationship> relationships =
//                found.getRelationships(RelTypes.HAS,
//                                       Direction.OUTGOING);
//        for (Relationship relation : relationships) {
//            if (relation.getProperty(type).equals(name)) {
//                //        return relation.getEndNode().getProperty(NODEINDEX);
//            }
//        }
//        Node newNode;
//        try {
//            newNode = db.createNode();
//            newNode.setProperty(type, name);
//            found.createRelationshipTo(newNode, RelTypes.HAS);
//            tx.success();
//        } catch (IllegalArgumentException ex) {
//            //TODO handle failuer
//            return null;
//        } finally {
//            tx.finish();
//        }
//        return newNode;
//    }
    /**
     * Searches a node for <it>has</it> relations with a specific property
     * value.
     *
     * @param node     the node to search from.
     * @param type     the node type.
     * @param property the property of the related nodes.
     * @param value    the value of the property.
     *
     * @return
     */
    public Object[] findHas(Object node, String type, String property,
                            Object value) {
        String query = "start n=node(" + ((Node) node).getId() + ") "
                       + " match (n) -[:HAS]-> (x) "
                       + " where x." + property + " = \"" + value.toString()
                       + "\" return x";
        ExecutionEngine engine = new ExecutionEngine(db);
        ExecutionResult result = engine.execute(query);
        Iterator<Node> xColumn = result.columnAs("x");
        return copyIterator(xColumn);
    }

    private String nodeQuery(Object[] nodes) {
        String query = "start n=node(" + ((Node) nodes[0]).getId();
        for (int i = 1; i < nodes.length; i++) {
            query += ", " + ((Node) nodes[i]).getId();
        }
        query += ") ";
        return query;
    }

    /*
     * Search methods
     */
    public Object[] FindDevicesByChassisId(Object value) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Searches the DB for devices by serial number. Returns the devices
     * matching the criteria. Brand and model may be null.
     *
     * @param brand  the brand of the devices.
     * @param model  the model of the devicea.
     * @param serial the serial of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesBySerial(String brand, String model,
                                        String serial) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = "start n=node(*) where n.type = \"device\" and n.brand = \""
                       + brand + "\" and n.model = \"" + model
                       + "\" and n.serial = \""
                       + serial + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by SNMP IP address number. Returns the
     * devices matching the criteria. Searches within the given devices.
     *
     * @param devices     the devices to search through.
     * @param snmpAddress the SNMP IP address of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesBySnmp(Object[] devices, String snmpAddress) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where  n.type = \"device\" and n.snmpAddress = \""
                 + snmpAddress
                 + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by services. Returns the devices matching the
     * criteria. Searches within the given devices.
     *
     * @param devices  the devices to search through.
     * @param services the services of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindeDevicesByServices(Object[] devices, Integer services) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.services = " + services
                 + " return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by name. Returns the devices matching the
     * criteria. Searches within the given devices.
     *
     * @param devices the devices to search through.
     * @param name    the names of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesByName(Object[] devices, String name) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.name = \"" + name
                 + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by location. Returns the devices matching the
     * criteria. Searches within the given devices.
     *
     * @param devices  the devices to search through.
     * @param location the location of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindeDevicesByLocation(Object[] devices, String location) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.location = \"" + location
                 + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by contact. Returns the devices matching the
     * criteria. Searches within the given devices.
     *
     * @param devices the devices to search through.
     * @param contact the contact of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesByContact(Object[] devices, String contact) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.contact = \"" + contact
                 + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by number of interfaces. Returns the devices
     * matching the criteria. Searches within the given devices.
     *
     * @param devices the devices to search through.
     * @param numOfIf the number of interfaces of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesByNumOfIf(Object[] devices, Integer numOfIf) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.numOfIf = " + numOfIf
                 + " return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by SNMP IP address number. Returns the
     * devices matching the criteria.
     *
     * @param snmpAddress the SNMP IP address of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesBySnmp(String snmpAddress) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = "start n = node(*) where n.type = \"device\" and n.snmpAddress = \""
                       + snmpAddress + "\" return n";
        ExecutionResult result = engine.execute(query);
        System.out.println(result);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }

    /**
     * Searches the DB for devices by model. Returns the devices matching the
     * criteria. Searches within the given devices. Brand may be null.
     *
     * @param devices the devices to search through.
     * @param brand   the brand of the devices.
     * @param model   the model of the devices.
     *
     * @return an array of handles to the search results.
     */
    public Object[] FindDevicesByModel(Object[] devices, String brand,
                                       String model) {
        ExecutionEngine engine = new ExecutionEngine(db);
        String query = nodeQuery(devices);
        query += "where n.type = \"device\" and n.brand = \"" + brand
                 + "\" and n.model = \"" + model + "\" return n";
        ExecutionResult result = engine.execute(query);
        Iterator<Node> nColumn = result.columnAs("n");
        return copyIterator(nColumn);
    }
}
