package in.sp.main.SignalingService.Handler;

import in.sp.main.SignalingService.Model.SignalingMsg;
import in.sp.main.SignalingService.Security.JwtUtil;
import in.sp.main.SignalingService.Service.SessionServiceClient;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Final SignalingWebSocketHandler
 * - Validates JWT on connect
 * - Validates session existence via SessionServiceClient
 * - Handles JOIN/OFFER/ANSWER/ICE/FILE_METADATA/APPROVE flows
 * - Forwards messages between sender and receiver
 * - Cleans up connections
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SessionServiceClient sessionClient;

    private final ObjectMapper mapper = new ObjectMapper();

    // Store live WebSocket connections keyed by sessionId
    private final Map<String, WebSocketSession> senderMap = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> receiverMap = new ConcurrentHashMap<>();

    // -------------------------------------------
    // Called when a new WebSocket connection is established
    // -------------------------------------------
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        // Extract token from URL: ws://.../ws?token=XYZ
        String query = session.getUri() != null ? session.getUri().getQuery() : null; // "token=XYZ"
        if (query == null || !query.startsWith("token=")) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }

        String token = query.substring("token=".length());

        // Validate token and extract claims
        Claims claims;
        try {
            claims = jwtUtil.extractClaims(token);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or expired token"));
            return;
        }

        String role = claims.get("role", String.class);
        String sessionId = claims.get("sessionId", String.class);

        if (sessionId == null || role == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token claims"));
            return;
        }

        // Verify session exists in SessionService
        boolean exists = false;
        try {
            exists = sessionClient.isValidSession(sessionId);
        } catch (Exception e) {
            log.error("Error calling SessionService for session {} : {}", sessionId, e.getMessage());
            session.close(CloseStatus.SERVER_ERROR.withReason("Session validation failed"));
            return;
        }

        if (!exists) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Session does not exist or expired"));
            return;
        }

        // Save into WebSocket attributes for later use
        session.getAttributes().put("role", role);
        session.getAttributes().put("sessionId", sessionId);

        log.info("WS authenticated → role: {}, session: {}, wsId: {}", role, sessionId, session.getId());
    }

    // -------------------------------------------
    // Handle incoming messages (text) after connection
    // -------------------------------------------
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage msg) throws Exception {

        SignalingMsg message;
        try {
            message = mapper.readValue(msg.getPayload(), SignalingMsg.class);
        } catch (IOException e) {
            log.warn("Invalid JSON payload: {}", e.getMessage());
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Invalid message format\"}"));
            return;
        }

        String role = (String) session.getAttributes().get("role");
        String sessionId = (String) session.getAttributes().get("sessionId");

        // Basic guard: token sessionId must match payload sessionId
        if (message.getSessionId() == null || !message.getSessionId().equals(sessionId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Session mismatch"));
            return;
        }

        String type = message.getType();
        if (type == null) {
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Missing message type\"}"));
            return;
        }

        switch (type) {
            case "JOIN_AS_SENDER":
                handleSenderJoin(sessionId, session);
                break;

            case "JOIN_AS_RECEIVER":
                handleReceiverJoin(sessionId, session);
                break;

            case "OFFER":
                forwardToReceiver(sessionId, message);
                break;

            case "ANSWER":
                forwardToSender(sessionId, message);
                break;

            case "ICE":
            case "ICE_CANDIDATE": // allow both names
                forwardIce(session, sessionId, message);
                break;

            case "FILE_METADATA":
                // forward metadata so receiver can show preview/accept
                forwardToReceiver(sessionId, message);
                break;

            case "CONNECTION_APPROVED":
            case "CONNECTION_REJECTED":
                // approval flow between sender <-> receiver
                forwardToSender(sessionId, message);
                break;

            default:
                log.warn("Unknown message type: {}", type);
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Unknown message type\"}"));
        }
    }

    // -------------------------------------------
    // Sender joins and maps sessionId -> WebSocketSession
    // -------------------------------------------
    private void handleSenderJoin(String sessionId, WebSocketSession session) {
        senderMap.put(sessionId, session);
        log.info("Sender joined for session {}", sessionId);
    }

    // -------------------------------------------
    // Receiver joins and notifies sender (if present)
    // -------------------------------------------
    private void handleReceiverJoin(String sessionId, WebSocketSession session) throws IOException {
        receiverMap.put(sessionId, session);
        log.info("Receiver joined for session {}", sessionId);

        // Send ACK to Receiver
        String ackMsg = String.format("{\"type\":\"JOIN_ACK\",\"role\":\"receiver\",\"sessionId\":\"%s\",\"status\":\"CONNECTED\"}", sessionId);
        session.sendMessage(new TextMessage(ackMsg));

        WebSocketSession sender = senderMap.get(sessionId);
        if (sender != null && sender.isOpen()) {
            // Notify sender that receiver joined (UI can show accept/reject)
            sender.sendMessage(new TextMessage("{\"type\":\"RECEIVER_JOINED\",\"sessionId\":\"" + sessionId + "\"}"));
        } else {
            // If sender not present, inform receiver
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Sender not connected\"}"));
        }
    }

    // -------------------------------------------
    // Forward OFFER from sender -> receiver
    // -------------------------------------------
    private void forwardToReceiver(String sessionId, SignalingMsg msg) throws IOException {
        WebSocketSession receiver = receiverMap.get(sessionId);
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            log.debug("Forwarded {} to receiver for session {}", msg.getType(), sessionId);
        } else {
            WebSocketSession sender = senderMap.get(sessionId);
            if (sender != null && sender.isOpen()) {
                sender.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Receiver not connected\"}"));
            }
            log.warn("Receiver not available for session {}", sessionId);
        }
    }

    // -------------------------------------------
    // Forward ANSWER from receiver -> sender
    // -------------------------------------------
    private void forwardToSender(String sessionId, SignalingMsg msg) throws IOException {
        WebSocketSession sender = senderMap.get(sessionId);
        if (sender != null && sender.isOpen()) {
            sender.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            log.debug("Forwarded {} to sender for session {}", msg.getType(), sessionId);
        } else {
            WebSocketSession receiver = receiverMap.get(sessionId);
            if (receiver != null && receiver.isOpen()) {
                receiver.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"message\":\"Sender not connected\"}"));
            }
            log.warn("Sender not available for session {}", sessionId);
        }
    }

    // -------------------------------------------
    // Forward ICE candidates based on role
    // -------------------------------------------
    private void forwardIce(WebSocketSession session, String sessionId, SignalingMsg msg) throws IOException {
        String role = (String) session.getAttributes().get("role");

        if ("sender".equals(role)) {
            WebSocketSession receiver = receiverMap.get(sessionId);
            if (receiver != null && receiver.isOpen()) {
                receiver.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            } else {
                log.warn("ICE: receiver not present for session {}", sessionId);
            }
        } else { // receiver
            WebSocketSession sender = senderMap.get(sessionId);
            if (sender != null && sender.isOpen()) {
                sender.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            } else {
                log.warn("ICE: sender not present for session {}", sessionId);
            }
        }
    }

    // -------------------------------------------
    // Cleanup on disconnect
    // -------------------------------------------
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // remove session entries that match this WebSocketSession
        senderMap.values().removeIf(s -> s != null && s.getId().equals(session.getId()));
        receiverMap.values().removeIf(s -> s != null && s.getId().equals(session.getId()));
        log.info("[WS] Disconnected wsId={} reason={}", session.getId(), status);
    }
}
