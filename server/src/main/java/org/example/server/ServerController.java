package org.example.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mtls")
public class ServerController {
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(){
        try {
            Map<String, String> body = new HashMap<>();
            body.put("message", "Connect Succeed!");
            return new ResponseEntity<>(body, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
