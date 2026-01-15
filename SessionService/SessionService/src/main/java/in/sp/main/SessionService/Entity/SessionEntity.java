package in.sp.main.SessionService.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@AllArgsConstructor
@NoArgsConstructor
@Data  // A convenient aggregate annotation that combines @Getter, @Setter, @RequiredArgsConstructor, @ToString, @EqualsAndHashCode
@Builder
@RedisHash("sessions")
public class SessionEntity {
    @Id
    private String sessionId;
    private boolean senderConnected;
    private boolean receiverConnected;
    private long createdAt;
    private long expiresAt;


}
