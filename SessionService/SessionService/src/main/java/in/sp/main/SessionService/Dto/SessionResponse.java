package in.sp.main.SessionService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {

    private String sessionId;
    private String senderToken;
    private String receiverToken;
    private long expiresAt;
}

