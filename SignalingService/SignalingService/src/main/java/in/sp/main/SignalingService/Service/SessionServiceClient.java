package in.sp.main.SignalingService.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SessionServiceClient {

    private final WebClient webClient;

    public SessionServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://SESSION-SERVICE").build();
    }


    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionServiceClient.class);

    public boolean isValidSession(String id){

        try {

            return webClient.get()
                    .uri("/sessions/" + id + "/validate")
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

        }catch (Exception e){
            log.error("Error validating session {}: {}", id, e.getMessage());
            return false;
        }
    }

    public void markSenderConnected(String id){

        webClient.post()
                .uri("/sessions/"+id +"/sender-connected")
                .retrieve()
                .toBodilessEntity()
                .block() ;
    }

    public void markReceiverConnected(String id){

        webClient.post()
                .uri("/sessions/"+id+"/receiver-connected")
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
