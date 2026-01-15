package in.sp.main.SessionService.Service;

import in.sp.main.SessionService.Dto.QRResponse;
import in.sp.main.SessionService.Dto.SessionResponse;
import in.sp.main.SessionService.Entity.SessionEntity;
import in.sp.main.SessionService.Repository.SessionRepository;
import in.sp.main.SessionService.Util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SessionService {

    @Autowired
    private SessionRepository repository ;

    @Autowired
    private JwtUtil jwtUtil;



    public SessionResponse createSession(){
        String id = UUID.randomUUID().toString().substring(0,6); //  random session id generate kara di
        long now  = System.currentTimeMillis();
        // Logical expiry for token validation (3 mins)
        SessionEntity s = SessionEntity.builder()
                .createdAt(now)
                .sessionId(id)
                .expiresAt(now+ 3*60_000) 
                .senderConnected(false)
                .receiverConnected(false)
                .build();
        
        // Redis expiry: 5 minutes (300s) to allow time for scanning
        repository.save(s, 300);


        String senderToken = jwtUtil.generateToken("sender", id);
        String receiverToken = jwtUtil.generateToken("receiver", id);

        return new SessionResponse(id, senderToken, receiverToken, s.getExpiresAt());
    }
    public SessionEntity getSession(String id){

        return repository.find(id);
    }

    public void markSenderConnected(String id){
        SessionEntity s = repository.find(id);
        if(s != null) {
            s.setSenderConnected(true);
            repository.save(s); // Keeps existing TTL (or resets to default 5 mins if using single arg) - actually single arg is 5 mins. 
            // Ideally sender connection shouldn't extend too much until receiver joins.
            // Let's keep it 5 mins until receiver joins.
        }
    }
    public void markReceiverConnected(String id){
        SessionEntity s = repository.find(id);
        if(s != null) {
            s.setReceiverConnected(true);
            // Extend session to 1 hour (3600s) when receiver connects
            repository.save(s, 3600);
        }
    }

    public Boolean validateSession(String id) {
        SessionEntity s = repository.find(id);

        if(s == null) return false;

        return s.getExpiresAt() > System.currentTimeMillis() ;
    }
}
