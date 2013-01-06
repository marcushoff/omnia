package omnia.test;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import omnia.db.DbHandler;
import org.junit.*;

/**
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public class DbHandlerTest {
    DbHandler DBH;

    public DbHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        DBH = new DbHandler();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testUpdateDBInfoForDevice() {
    }
}
