package ng.upperlink.nibss.cmms.config.cache.emandate.repository;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Map;

@Repository
@NoArgsConstructor
public class RedisRepositoryImpl implements RedisRepository<String> {
    private static final String KEY = "EmandateToken";
    
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations hashOperations;

    @Autowired
    public RedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init(){
        hashOperations = redisTemplate.opsForHash();
    }

    public void add(final String token,final String sessionId) {
        hashOperations.put(KEY, sessionId, token);
    }

    public void delete(final String sessionId) {
        hashOperations.delete(KEY, sessionId);
    }

    public String findObj(final String sessionId){
        return (String) hashOperations.get(KEY, sessionId);
    }
    
    public Map<String, String> findAllMovies(){
        return hashOperations.entries(KEY);
    }


}
