package com.nexon.yt2spotify.service;

import com.nexon.yt2spotify.model.User;
import com.nexon.yt2spotify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    // ---- Standard OAuth2 Handling ---------

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("LOAD USER (OAUTH 2 Standard) TRIGGERED FOR REGISTRATION: Registration ID : {} ", userRequest.getClientRegistration().getRegistrationId());

        // 1. Delegate to the default implementation to get the OAuth2User

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. Process the user information

        processOAuthUser(userRequest.getClientRegistration().getRegistrationId(), oAuth2User);

        return oAuth2User;

    }

    // ---------- OIDC HANDLING ---- BECAUSE GOOGLE USES IT BY DEFAULT
    // We need a method that matches the OidcUserService functional interface


    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("LOAD USER (OIDC) TRIGGERED for REGISTRATION: Registration ID : {} ", userRequest.getClientRegistration().getRegistrationId());

        // 1. Delegate to the default OIDC Service To get the OidcUser

        OidcUserService delegate = new OidcUserService();
        OidcUser oidcUser = delegate.loadUser(userRequest);

        // 2. Process the user information (Oidc extends OAuth2User)
        processOAuthUser(userRequest.getClientRegistration().getRegistrationId(), oidcUser);


        return oidcUser;
    }

    // ---------- COMMON PROCESSING LOGI -------------
    // To take the somewhat generic user information (OAuth2User) provided by Spring Security after a successful login and
    // extract/normalize the key pieces of data we care about, specific to each provider (Spotify vs. Google).
    // It prepares the data for the database update step.


    private void processOAuthUser(String registrationId, OAuth2User oAuth2User) {

        // Generic data which the oauth service like google or spotify sends back after a successful login
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerUserId = null;
        String displayName = null;
        String email = null;

        // Now the above three parameters can be different for different provider so manage them accordingly
        log.debug("Processing User for registrationID : {} ", registrationId);

        // if the provider is spotify
        if ("spotify".equalsIgnoreCase(registrationId)) {
            providerUserId = oAuth2User.getAttribute("id");
            displayName = oAuth2User.getAttribute("display_name");
            email = oAuth2User.getAttribute("email");

            log.info("Spotify User Details : id= {}, display_name= {}, email= {}", providerUserId, displayName, email);
        }

        // if the provider is google
        else if ("google".equalsIgnoreCase(registrationId)) {
            // Google uses sub as the unique id
            providerUserId = oAuth2User.getAttribute("sub");
            displayName = oAuth2User.getAttribute("name");
            email = oAuth2User.getAttribute("email");

            log.info("Google User Details (OIDC): sub = {} , name = {}, email = {}", providerUserId, displayName, email);
        } else {

            log.warn("Unsupported OAuth2 Provider : {}", registrationId);
            return;
        }

        if (providerUserId == null || providerUserId.trim().isEmpty()) {

            log.error("Could not extract valid provider user ID for registrationID : {}, Attributes: {}", registrationId, attributes);
            return;
        }

        // Call the database update logic
        updateUserDatabase(providerUserId, registrationId, displayName, email);

    }

    // --------- DATABSE UPDATE LOGIC -----------------

    private User updateUserDatabase(String providerUserId, String provider, String displayName, String email) {

        Optional<User> userOptional;

        log.debug("Attempting DB UPDATE for provider : {} , providerUserId  {}", provider, providerUserId);

        if ("spotify".equalsIgnoreCase(provider)) {
            userOptional = userRepository.findBySpotifyId(providerUserId);
        } else if ("google".equalsIgnoreCase(provider)) {
            userOptional = userRepository.findByGoogleId(providerUserId);
        } else {
            log.warn("Provider : {} is not recognized in updateUserDatbase method", provider);
            return null;
        }

        User appUser;
        if (userOptional.isPresent()) {
            // User exixts for this specific ID

            appUser = userOptional.get();
            log.info("Found existing user by providerID , UserID : {} , Provider : {} ", appUser.getId(), provider);
            boolean updated = false;

            if (displayName != null && !displayName.equals(appUser.getDisplayName())) {
                appUser.setDisplayName(displayName);
                updated = true;
            }
            if (email != null && email.length() > 0 && (appUser.getEmail() == null && !email.equals(appUser.getEmail()))) {
                appUser.setEmail(email);
                updated = true;
            }
            // Update provider IDs if somehow they were null before (shouldn't normally happen here)
            if ("spotify".equalsIgnoreCase(provider) && appUser.getSpotifyId() == null) {
                appUser.setSpotifyId(providerUserId);
                updated = true;
            } else if ("google".equalsIgnoreCase(provider) && appUser.getGoogleId() == null) {
                appUser.setGoogleId(providerUserId);
                updated = true;
            }


            if (updated) {
                log.info("Updating existing user ID {} for provider {}", appUser.getId(), provider);
                appUser = userRepository.save(appUser);
            } else {
                log.info("No updates needed for existing user ID {} with provider {}", appUser.getId(), provider);
            }


        } else {
            // NO USER FOUND FOR THIS SPECIFIC provider ID.
            log.info("No user found for providerId: {}. Checking by email: {}", providerUserId, email);
            Optional<User> existingUserByEmail = (email != null && email.length() > 0) ? userRepository.findByEmail(email) : Optional.empty();

            if (existingUserByEmail.isPresent()) {
                // Found user with same email via different provider -> Link accounts
                appUser = existingUserByEmail.get();
                log.info("Found existing user ID {} via email {}. Linking provider {}", appUser.getId(), email, provider);

                // Add the missing provider ID
                if ("spotify".equalsIgnoreCase(provider)) {
                    if (appUser.getSpotifyId() == null) { // Only set if not already set
                        appUser.setSpotifyId(providerUserId);
                    } else if (!providerUserId.equals(appUser.getSpotifyId())) {
                        log.warn("Attempting to link Spotify ID {} but user {} already has Spotify ID {}", providerUserId, appUser.getId(), appUser.getSpotifyId());
                        // Handle this conflict case as needed - maybe throw error or ignore
                    }
                } else if ("google".equalsIgnoreCase(provider)) {
                    if (appUser.getGoogleId() == null) { // Only set if not already set
                        appUser.setGoogleId(providerUserId);
                    } else if (!providerUserId.equals(appUser.getGoogleId())) {
                        log.warn("Attempting to link Google ID {} but user {} already has Google ID {}", providerUserId, appUser.getId(), appUser.getGoogleId());
                        // Handle this conflict case as needed
                    }
                }

                // Update display name if the new provider's one is more complete? (Optional logic)
                if (displayName != null && (appUser.getDisplayName() == null || appUser.getDisplayName().isEmpty() || !displayName.equals(appUser.getDisplayName()))) {
                    appUser.setDisplayName(displayName);
                }
                appUser = userRepository.save(appUser);
            } else {
                // Completely new user - neither provider ID nor email matched
                log.info("Creating completely new user for provider {}: providerId={}, displayName={}, email={}", provider, providerUserId, displayName, email);
                appUser = new User(); // Use default constructor
                appUser.setEmail(email);
                appUser.setDisplayName(displayName);
                if ("spotify".equalsIgnoreCase(provider)) {
                    appUser.setSpotifyId(providerUserId);
                } else if ("google".equalsIgnoreCase(provider)) {
                    appUser.setGoogleId(providerUserId);
                }
                appUser = userRepository.save(appUser);
            }
        }
        return appUser;
    }

}
