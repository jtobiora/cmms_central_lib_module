package ng.upperlink.nibss.cmms.config.cache.emandate.repository;


import java.util.Map;

public interface RedisRepository<T> {

    /**
     * Return all object saved in redis
     */
    Map<T, T> findAllMovies();

    /**
     * Add key-value pair to Redis.
     */
    void add(T obj,String sessionId);

    /**
     * Delete a key-value pair in Redis.
     */
    void delete(String sessionId);
    
    /**
     * find an object
     */
    T findObj(String sessionId);
    
}
