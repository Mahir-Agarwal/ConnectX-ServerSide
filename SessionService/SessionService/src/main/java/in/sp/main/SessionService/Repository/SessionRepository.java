package in.sp.main.SessionService.Repository;

import in.sp.main.SessionService.Entity.SessionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class SessionRepository {

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private static final String PREFIX = "session:";


    // Save session with default TTL = 5 minutes
    public void save(SessionEntity session){
        save(session, 300);
    }

    // Save session with custom TTL (in seconds)
    public void save(SessionEntity session, long timeoutSeconds){
        redisTemplate.opsForValue()
                .set(PREFIX + session.getSessionId(), session, timeoutSeconds, TimeUnit.SECONDS);
    }

    // Find a single session
    public SessionEntity find(String sessionId){
        return (SessionEntity) redisTemplate.opsForValue().get(PREFIX + sessionId);
    }

    // Delete specific session
    public void delete(String sessionId){
        redisTemplate.delete(PREFIX + sessionId);
    }


    //  NEW: Get all session keys
    public Set<String> getAllSessionKeys() {
        return redisTemplate.keys(PREFIX + "*");
    }


    //  NEW: Find all sessions
    public List<SessionEntity> findAll() {

        Set<String> keys = getAllSessionKeys();
        List<SessionEntity> sessions = new ArrayList<>();

        if (keys != null) {
            keys.forEach(key -> {
                SessionEntity session = (SessionEntity) redisTemplate.opsForValue().get(key);
                if (session != null) {
                    sessions.add(session);
                }
            });
        }

        return sessions;
    }
}
