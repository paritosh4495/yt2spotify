package com.nexon.yt2spotify.service;

import com.nexon.yt2spotify.model.User;
import com.nexon.yt2spotify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationHelper {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final UserRepository userRepository;


     /**
     * Gets a valid access token for the specified client registration ID and authenticated user.
     * Handles finding the correct provider ID from the linked User entity and using it
     * to authorize via the OAuth2AuthorizedClientManager.
     *
     * @param authentication       The current user's Authentication object.
     * @param clientRegistrationId The ID of the client ("spotify" or "google").
     * @return The access token string.
     * @throws RuntimeException                 if the user cannot be found, linking is incomplete,
     * or authorization fails for other reasons.
     * @throws ClientAuthorizationRequiredException if re-authorization by the user is required.
     */

     public String getAccessToken(Authentication authentication, String clientRegistrationId) throws ClientAuthorizationRequiredException {

         if(authentication == null){
             log.error("Cannot Authorized : Authentication is null");
             throw new RuntimeException("User must be authenticated to perform this action.");
         }
         String currentPrincipalName = authentication.getName();
         log.debug("Attempting to get token for client : {} and principal : {}", clientRegistrationId, currentPrincipalName);

         // -- FInd the linked  USER entity ----
         // Determine if the current princial name seems like Google ID or spotify ID
         boolean isGooglePrinicpal = currentPrincipalName.matches("\\d+");
         Optional<User> userOptional = isGooglePrinicpal ? userRepository.findByGoogleId(currentPrincipalName) : userRepository.findBySpotifyId(currentPrincipalName);


         if(userOptional.isEmpty()){
             // Attempt to lookup via the "other" ID field just in case the principal name doesnt match the expectations
             // ( THIS Is defensive Ideally the principal name IS the correct ID fromt the last login )

             userOptional = !isGooglePrinicpal ? userRepository.findByGoogleId(currentPrincipalName) : userRepository.findBySpotifyId(currentPrincipalName);
             if(userOptional.isEmpty()){
                 log.error("Could not find any linked user record for prinicapl name : {}", currentPrincipalName);
                 throw new RuntimeException("Could not find associated user data for principal: " + currentPrincipalName);
             }
         }

         User user = userOptional.get();
         String providerId; // THe ID required for the "target" service
         // ---- GET the correct Provider ID for the TARGET Service ----
         if("spotify".equalsIgnoreCase(clientRegistrationId)){
             providerId = user.getSpotifyId();
             if(providerId == null || providerId.isEmpty()){
                 log.warn("User {}  (Principal : {} ) has not linked their Spotify account", user.getId(), currentPrincipalName);
                 throw new RuntimeException("User " + user.getId() + " has not linked their Spotify account");
             }
         }
         else if("google".equalsIgnoreCase(clientRegistrationId)){
             providerId = user.getGoogleId();
             if(providerId == null || providerId.isBlank()){
                 log.warn("User {} (Principal : {} ) has not linked their Google account)", user.getId(), currentPrincipalName);
                 throw new RuntimeException("Google account not linked for this user.");

             }
         }
         else{
             log.error("Unsupported clientRegistrationId requested: {}", clientRegistrationId);
             throw new IllegalArgumentException("Unsupported client registration ID: " + clientRegistrationId);
         }

         log.debug("Using provider ID '{}' for client '{}' lookup, based on linked User ID {}", providerId, clientRegistrationId, user.getId());

         // --- Authorize using the correct Provider ID ---
         // Create a temporary Authentication principal with the correct name (providerId)
         Authentication targetPrincipal = UsernamePasswordAuthenticationToken.authenticated(providerId, null, authentication.getAuthorities());

         OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                 .withClientRegistrationId(clientRegistrationId)
                 .principal(targetPrincipal) // Use principal with the correct name for lookup
                 .build();

         log.debug("Attempting to authorize client {} using principal name {}", clientRegistrationId, targetPrincipal.getName());


         OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

         // --- Validate token ---
         if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
             log.error("Authorization failed for {} using stored ID {} for user {}", clientRegistrationId, providerId, user.getId());
             log.error("Authorization failed for {} using stored ID {} (Principal Name: {}). Manager returned null client or null access token.",
                     clientRegistrationId, providerId, targetPrincipal.getName());

             // The manager might throw ClientAuthorizationRequiredException before this,
             // but this catches other null scenarios.
             throw new RuntimeException("Stored " + clientRegistrationId + " authorization failed or token missing. Please try logging in to " + clientRegistrationId + " again.");
         }

         String accessToken = authorizedClient.getAccessToken().getTokenValue();
         log.debug("Successfully obtained {} access token for user {}", clientRegistrationId, user.getId());
         return accessToken;

     }

}
