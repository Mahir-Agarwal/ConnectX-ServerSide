package in.sp.main.SessionService.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QRResponse {
    private String sessionId;
    private String receiverToken;
    private long expiresAt;
}
