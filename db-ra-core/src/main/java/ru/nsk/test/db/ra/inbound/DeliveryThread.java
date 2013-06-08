package ru.nsk.test.db.ra.inbound;


import java.util.*;
import java.util.logging.*;
import javax.resource.spi.work.*;

/**
 * 
 *
 */

public class DeliveryThread  implements Work 
{
    public static final Logger  logger = 
        Logger.getLogger(DeliveryThread.class.getPackage().getName());
    static ResourceBundle 	resource = 
        java.util.ResourceBundle.getBundle(
            DeliveryThread.class.getPackage().getName()+".LocalStrings"); 

    private EndpointConsumer  endpointConsumer;
    private String group;
    
    /**
     * Constructor.
     */

    public DeliveryThread(EndpointConsumer endpointConsumer, String group)
    {
        this.endpointConsumer = endpointConsumer;
        this.group = group;
        logger.finest("[DeliveryThread::Constructor] Leaving");

    }

    /**
     * release: called by the WorkerManager
     */

    public void release()
    {
        logger.finest("[DT] Worker Manager called release for deliveryThread ");
    }

    /**
     * Run method. Should be started only from WorkerManager.
     */

    public void run()
    { 
	logger.finest("[DT] Worker Manager started delivery thread ");
                
        try
	{
            endpointConsumer.deliverMessages(group);
        } catch (Exception te) {
            logger.severe("deliveryThread::run got an exception");
            te.printStackTrace();
        }
        
	logger.finest("[DT] DeliveryThread leaving");
    }    
}
