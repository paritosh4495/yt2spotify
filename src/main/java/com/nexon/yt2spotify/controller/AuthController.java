package com.nexon.yt2spotify.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/")
    public Map<String,String> home(){
        return Collections.singletonMap("message","Welcome !) ");
    }
    // Spring Security automatically provides /oauth2/authorization/spotify and /oauth2/authorization/google
    // based on your configuration. You can link to these.

    @GetMapping("/user")
    public Map<String,Object> user(@AuthenticationPrincipal OAuth2User principal){
        // This shows basic user info AFTER successful OAuth Login
        // NOTE : This is temporary. We need a proper user entity

        if(principal!=null){
            return Collections.singletonMap("Successfully logged in : ",principal.getAttributes());
        }
        return Collections.singletonMap("error","User Not Authenticated");
    }

    // We will add explicit endpoints later if needed, e.g.: This will be needed
    // @GetMapping("/login/spotify")
    // public RedirectView loginSpotify() { return new RedirectView("/oauth2/authorization/spotify"); }
    // @GetMapping("/login/google")
    // public RedirectView loginGoogle() { return new RedirectView("/oauth2/authorization/google"); }

}
