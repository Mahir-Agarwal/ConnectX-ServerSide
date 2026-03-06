package in.sp.main.SignalingService.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class TurnController {

    @GetMapping("/turn-credentials")
    public Map<String, Object> getTurnCredentials() {
        // Return public STUN server for now. 
        // In production, fetch ephemeral TURN credentials here.
        List<Map<String, String>> iceServers = new ArrayList<>();
        
        // Google's Public STUNs
        iceServers.add(Map.of("urls", "stun:stun.l.google.com:19302"));
        iceServers.add(Map.of("urls", "stun:stun1.l.google.com:19302"));
        iceServers.add(Map.of("urls", "stun:stun2.l.google.com:19302"));
        iceServers.add(Map.of("urls", "stun:stun3.l.google.com:19302"));
        iceServers.add(Map.of("urls", "stun:stun4.l.google.com:19302"));
        
        // Other Public STUNs (Twilio, Framasoft, Ekiga, etc.)
        iceServers.add(Map.of("urls", "stun:global.stun.twilio.com:3478"));
        iceServers.add(Map.of("urls", "stun:stun.framasoft.org:3478"));
        iceServers.add(Map.of("urls", "stun:stun.ekiga.net:3478"));
        iceServers.add(Map.of("urls", "stun:stun.voipbuster.com:3478"));
        iceServers.add(Map.of("urls", "stun:stun.voipstunt.com:3478"));
        iceServers.add(Map.of("urls", "stun:stun.xten.com:3478"));
        iceServers.add(Map.of("urls", "stun:stun.sipgate.net:3478"));
        
        // Example TURN entry (Commented out):
        // iceServers.add(Map.of(
        //    "urls", "turn:your-turn-server.com:3478",
        //    "username", "user",
        //    "credential", "password"
        // ));

        // Return configuration object compatible with RTCPeerConnection
        return Map.of(
            "iceServers", iceServers,
            "iceTransportPolicy", "all", // Force allow TCP/UDP
            "bundlePolicy", "max-bundle"
        );
    }
}
