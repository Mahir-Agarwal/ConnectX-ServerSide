package in.sp.main.SignalingService.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMsg {

    private  String type ; // JOIN_AS_SENDER, OFFER, ANSWER, ICE...

    private String sessionId;
    private Object payload;  // SDP / ICE JSON
}
