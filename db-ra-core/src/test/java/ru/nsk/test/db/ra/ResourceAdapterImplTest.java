package ru.nsk.test.db.ra;

import java.util.UUID;
import javax.resource.spi.BootstrapContext;
import junit.framework.TestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
//import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.arquillian.junit.Arquillian;
//import org.jboss.jca.arquillian.embedded.Configuration;
//import org.jboss.jca.arquillian.embedded.Inject;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.nsk.test.db.ra.inbound.DeliveryThread;

/**
 *
 */
@RunWith(Arquillian.class)
//@Configuration(autoActivate = true)
public class ResourceAdapterImplTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Deployment
    public static ResourceAdapterArchive createDeploymentForGlassFish() {

        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, "ArquillianTest.rar");

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID().toString() + ".jar");
        ja.addPackage(ResourceAdapterImpl.class.getPackage());
        ja.addPackage(DeliveryThread.class.getPackage());

        raa.addAsLibrary(ja);
        raa.addAsManifestResource("simple.rar/META-INF/ra.xml", "ra.xml");

        return raa;
    }
//    @Resource(mappedName = "java:/eis/ArquillianTest")
//    private TestConnectionFactory connectionFactory;
//    @Inject(name = "MDR")
//    private MetadataRepository mdr;

    /**
     * Test of start method, of class ResourceAdapterImpl.
     */
    @Test
    public void testStart() throws Exception {
        System.out.println("start");
        BootstrapContext ctx = null;
//        ResourceAdapterImpl instance = new ResourceAdapterImpl();
//        instance.start(ctx);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }
}
