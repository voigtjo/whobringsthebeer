package de.joevoi.whobringsthebeer.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import de.joevoi.whobringsthebeer.form.ProfileForm.TeeShirtSize;


// TODO indicate that this class is an Entity
/**
 * @author j.voigt
 *
 */
@Entity
@Cache
public class Profile {
    String displayName;
    String mainEmail;
    TeeShirtSize teeShirtSize;

    // TODO indicate that the userId is to be used in the Entity's key
    @Id String userId;

    /**
     * Keys of the conferences that this user registers to attend.
     */
    private List<String> eventKeysToAttend = new ArrayList<>(0);
    private List<String> invitationKeysToAttend = new ArrayList<>(0);
    private List<String> conferenceKeysToAttend = new ArrayList<>(0);
    private List<String> groupKeysMemberOf = new ArrayList<>(0);

    /**
     * Public constructor for Profile.
     * @param userId The user id, obtained from the email
     * @param displayName Any string user wants us to display him/her on this system.
     * @param mainEmail User's main e-mail address.
     * @param teeShirtSize The User's tee shirt size
     *
     */
    public Profile (String userId, String displayName, String mainEmail, TeeShirtSize teeShirtSize) {
        this.userId = userId;
        this.displayName = displayName;
        this.mainEmail = mainEmail;
        this.teeShirtSize = teeShirtSize;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMainEmail() {
        return mainEmail;
    }

    public TeeShirtSize getTeeShirtSize() {
        return teeShirtSize;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Getter for conferenceIdsToAttend.
     * @return an immutable copy of conferenceIdsToAttend.
     */
    public List<String> getConferenceKeysToAttend() {
        return ImmutableList.copyOf(conferenceKeysToAttend);
    }
    
    public List<String> getEventKeysToAttend() {
        return ImmutableList.copyOf(eventKeysToAttend);
    }
    
    public List<String> getGroupKeysMemberOf() {
        return ImmutableList.copyOf(groupKeysMemberOf);
    }

    /**
     * Just making the default constructor private.
     */
    private Profile() {}

    /**
     * Update the Profile with the given displayName and teeShirtSize
     *
     * @param displayName
     * @param teeShirtSize
     */
    public void update(String displayName, TeeShirtSize teeShirtSize) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (teeShirtSize != null) {
            this.teeShirtSize = teeShirtSize;
        }
    }

    /**
     * Adds a ConferenceId to conferenceIdsToAttend.
     *
     * The method initConferenceIdsToAttend is not thread-safe, but we need a transaction for
     * calling this method after all, so it is not a practical issue.
     *
     * @param conferenceKey a websafe String representation of the Conference Key.
     */
    public void addToConferenceKeysToAttend(String conferenceKey) {
        conferenceKeysToAttend.add(conferenceKey);
    }
    
    public void addToEventKeysToAttend(String eventKey) {
        eventKeysToAttend.add(eventKey);
    }

    /**
     * Remove the conferenceId from conferenceIdsToAttend.
     *
     * @param conferenceKey a websafe String representation of the Conference Key.
     */
    public void unregisterFromConference(String conferenceKey) {
        if (conferenceKeysToAttend.contains(conferenceKey)) {
            conferenceKeysToAttend.remove(conferenceKey);
        } else {
            throw new IllegalArgumentException("Invalid conferenceKey: " + conferenceKey);
        }
    }
    
    public void unregisterFromEvent(String eventKey) {
        if (eventKeysToAttend.contains(eventKey)) {
        	eventKeysToAttend.remove(eventKey);
        } else {
            throw new IllegalArgumentException("Invalid eventKey: " + eventKey);
        }
    }
    
    public void addToGroupKeysMemberOf(String groupKey) {
    	groupKeysMemberOf.add(groupKey);
    }
    
    public void removeFromGroupKeysMemberOf(String groupKey) {
        if (groupKeysMemberOf.contains(groupKey)) {
        	groupKeysMemberOf.remove(groupKey);
        } else {
            throw new IllegalArgumentException("Invalid groupKey: " + groupKey);
        }
    }

	@Override
	public String toString() {
		return "Profile [displayName=" + displayName + ", mainEmail=" + mainEmail + ", teeShirtSize=" + teeShirtSize
				+ ", userId=" + userId + "]";
	}

	public List<String> getInvitationKeysToAttend() {
		return ImmutableList.copyOf(invitationKeysToAttend);
	}

	public void addToInvitationKeysToAttend(String invitationKey) {
		eventKeysToAttend.add(invitationKey);
	}

	public void unregisterFromInvitation(String invitationKey) {
		if (invitationKeysToAttend.contains(invitationKey)) {
			invitationKeysToAttend.remove(invitationKey);
        } else {
            throw new IllegalArgumentException("Invalid invitationKey: " + invitationKey);
        }
	}
    
    

}
