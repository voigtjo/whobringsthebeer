package de.joevoi.whobringsthebeer.spi;

import static de.joevoi.whobringsthebeer.service.OfyService.ofy;

import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;

import de.joevoi.whobringsthebeer.domain.Profile;
import de.joevoi.whobringsthebeer.form.ProfileForm.TeeShirtSize;

public class BasicApi {
	/**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     * @param user
     * @return user's Profile
     */
    public static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }
    
    protected static String extractDefaultDisplayNameFromEmail(String email) {
 		return email == null ? null : email.substring(0, email.indexOf("@"));
 	}
    

}
