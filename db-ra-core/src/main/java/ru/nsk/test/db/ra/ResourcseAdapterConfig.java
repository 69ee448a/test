package ru.nsk.test.db.ra;

/**
 *
 * Class contains configuration records provided in ra.xml
 */
public class ResourcseAdapterConfig {
    /**
     * Count of database pooling threads.
     */
    private Integer poolSize;

    /**
     * @return the poolSize
     */
    public Integer getPoolSize() {
        return poolSize;
    }

    /**
     * @param poolSize the poolSize to set
     */
    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
    }
}
