package ru.nsk.test.db.gen;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Projections;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.GenericJDBCException;
import org.postgresql.util.PSQLException;

/**
 *
 * Core database read/write class.
 */
public class Core {

    private static volatile Core instance;
    private static SessionFactory sessionFactory;
    private static Configuration configuration;
    private static Logger logger;
    private static ThreadLocal localSessions = new ThreadLocal() {
        protected Object initialvalue() {
            return null;
        }
    };

    public static void init() {
        Session s = sessionFactory.openSession();
        s.beginTransaction();
        localSessions.set(new LocalSession(s));
    }

    public static void destroy() {

        try {
            if (session().isOpen()) {
                org.hibernate.Transaction tx = session().getTransaction();
                if (tx.isActive()) {
                    if (localSession().getError()) {
                        logger.warn("Session error flag is true. Do rollback...");
                        tx.rollback();
                    } else {
                        logger.trace("Session error flag is false. Do commit...");
                        tx.commit();
                    }
                }
                session().close();

                logger.trace("Session successfully destroyed!");
            }
        } catch (HibernateException he) {
            printException(he);
            throw new RuntimeException(he);
        }
        
    }

    /**
     * Method remove all items from given table type.
     *
     * @param type - The type of entity for removal.
     */
    public void cleanupTable(Class type) {
        try {
            ArrayList result = (ArrayList) session().createCriteria(type).list();
            Iterator i = result.iterator();
            while (i.hasNext()) {
                Object o = i.next();
                session().delete(o);
            }
        } catch (HibernateException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Method return count of records in given table type.
     * @param type
     * @return 
     */
    public Integer getEntitiesCount(Class type) {
        try {
            return (Integer) session().createCriteria(type)
                    .setProjection(Projections.count("id")).uniqueResult();
        } catch (HibernateException ex) {
            logger.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
        
    }

    protected Object setObject(Object obj) throws RuntimeException {
        try {
            Session session = session();
            obj = session.merge(obj);
        } catch (HibernateException hex) {
            logger.error(hex.getMessage(), hex);
            throw new RuntimeException(hex);
        }
        return obj;
    }

    static {
        Core localInstance = instance;
        if (localInstance == null) {
            synchronized (Core.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Core();
                    try {
                        logger = Logger.getLogger(Core.class);
                        logger.info("Static initializer called.");

                        configuration = new AnnotationConfiguration().configure();
                        sessionFactory = configuration.buildSessionFactory();
                    } catch (Throwable ex) {
                        System.err.println("Initial SessionFactory creation failed." + ex);
                        System.err.println("Initial SessionFactory creation failed with ["
                                + ex.getMessage() + "]");
                        ex.printStackTrace();
                        throw new ExceptionInInitializerError(ex);
                    }
                }
            }
        }
    }

    private static Session session() {
        LocalSession ls = (LocalSession) localSessions.get();
        Session result = ls.getSession();
        if (result == null) {
            throw new RuntimeException("Session not found");
        }
        return result;
    }

    private static LocalSession localSession() {
        LocalSession result = (LocalSession) localSessions.get();
        if (result == null) {
            throw new RuntimeException("Local session not found.");
        }
        return result;
    }

    public static void printException(Exception e) {
        logger.error(e.getMessage(), e);
        if (e instanceof GenericJDBCException) {
            SQLException sqle = ((GenericJDBCException) e).getSQLException();
            logger.error(sqle.getMessage(), sqle);
            if (sqle instanceof BatchUpdateException) {
                Exception ee = sqle;
                while (true) {
                    if (ee instanceof BatchUpdateException) {
                        ee = ((BatchUpdateException) ee).getNextException();
                    } else if (ee instanceof PSQLException) {
                        ee = ((PSQLException) ee).getNextException();
                    } else {
                        break;
                    }
                    if (ee == null) {
                        break;
                    }
                    logger.error(ee.getMessage(), ee);
                    if (ee instanceof PSQLException) {
                        logger.error(((PSQLException) ee).getServerErrorMessage());
                    }
                }
            }
        }
        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ce = (ConstraintViolationException) e;
            logger.error("Failed at constaraint [" + ce.getConstraintName() + "].");
            logger.error("SQL error [" + ce.getSQL() + "].", ce.getCause());
            if (ce.getCause() != null && ce.getCause() instanceof BatchUpdateException) {
                BatchUpdateException be = (BatchUpdateException) ce.getCause();
                logger.error(be.getNextException());
            }
        }
    }
}
