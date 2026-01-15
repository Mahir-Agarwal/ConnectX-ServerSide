package in.sp.main.SessionService.Service;

import in.sp.main.SessionService.Entity.SessionEntity;
import in.sp.main.SessionService.Repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@EnableScheduling
public class SessionCleanupService {

    @Autowired
    private SessionRepository sessionRepository;

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();

        List<SessionEntity> sessions = sessionRepository.findAll();

        for (SessionEntity s : sessions) {
            if (s.getExpiresAt() < now) {
                sessionRepository.delete(s.getSessionId());
                System.out.println("Deleted expired session: " + s.getSessionId());
            }
        }
    }
}
