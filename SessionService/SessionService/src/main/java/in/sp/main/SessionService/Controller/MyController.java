package in.sp.main.SessionService.Controller;

import in.sp.main.SessionService.Dto.SessionResponse;
import in.sp.main.SessionService.Entity.SessionEntity;
import in.sp.main.SessionService.Service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
public class MyController {

    @Autowired
    private SessionService service;

    @PostMapping
    public SessionResponse create(){

        return  service.createSession();
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validate(@PathVariable String id){

        return ResponseEntity.ok(service.validateSession(id));

    }
    @GetMapping("/{id}")
    public SessionEntity get(@PathVariable String id){

        return service.getSession(id);
    }
    @PostMapping("/{id}/sender-connected")
    public void SenderConnected(@PathVariable String id){

            service.markSenderConnected(id);
    }
    @PostMapping("/{id}/receiver-connected")
    public void receiverConnected(@PathVariable String id){

        service.markReceiverConnected(id);
    }

}
