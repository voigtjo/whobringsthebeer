package de.joevoi.whobringsthebeer.domain;

import static de.joevoi.whobringsthebeer.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

/**
 * Invitation class stores invitation information.
 */
@Entity
@Cache
public class Invitation {

    @Id
    private long id;
    
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Event> eventKey;
    
    private String websafeEventKey;
    
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;

    @Index
    private List<String> memberKeys = new ArrayList<>(0);
    
    @Index
    private List<String> invitationKeys = new ArrayList<>(0);
    
	public List<String> getInvitationKeys() {
		return invitationKeys == null ? null : ImmutableList.copyOf(invitationKeys);
	}
	
	public void addInvitationToInvitation(String invitationKey) {
		invitationKeys.add(invitationKey);
    }

    @Index private String location;

    @Index private Date invitationDate;

   

    /**
     * Just making the default constructor private.
     */
    private Invitation() {}

    public Invitation(long id, Date invitationDate, String websafeEventKey, String organizerUserId) {
        this.id = id;
        this.invitationDate = invitationDate == null ? null : new Date(invitationDate.getTime());
        this.websafeEventKey = websafeEventKey;
        this.eventKey = Key.create(websafeEventKey);
        this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
    }
    
    public long getId() {
        return id;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Event> getEventKey() {
        return eventKey;
    }

    // Get a String version of the key
    public String getWebsafeKey() {
        return Key.create(eventKey, Invitation.class, id).getString();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getOrganizerUserId() {
        return organizerUserId;
    }

    public String getOrganizerDisplayName() {
        // Profile organizer = ofy().load().key(Key.create(Profile.class, organizerUserId)).now();
        Profile organizer = ofy().load().key(getProfileKey()).now();
        if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getDisplayName();
        }
    }

    public String getWebsafeEventKey() {
		return websafeEventKey;
	}
    
    public String getEventDisplayName() {
        Event event = ofy().load().key(getEventKey()).now();
        if (event == null) {
            return getWebsafeEventKey();
        } else {
            return event.getName();
        }
    }

	@ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
	public Key<Profile> getProfileKey() {
		return profileKey;
	}

	public List<String> getMemberKeys() {
		return ImmutableList.copyOf(memberKeys);
	}

	public String getLocation() {
		return location;
	}

	public Date getInvitationDate() {
		return invitationDate == null ? null : new Date(invitationDate.getTime());
	}

	public void addMemberToInvitation(String memberKey) {
    	memberKeys.add(memberKey);
    }
	
	public void unregisterMemberFromInvitation(String memberKey) {
        if (memberKeys.contains(memberKey)) {
        	memberKeys.remove(memberKey);
        } else {
            throw new IllegalArgumentException("Invalid memberKey: " + memberKey);
        }
    }

	@Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + id + "\n")
        		.append("WebsafeKey: ").append(getWebsafeKey()).append("\n")
        		.append("websafeEventKey: ").append(websafeEventKey).append("\n")
                .append("eventKey: ").append(eventKey).append("\n")
                .append("profileKey: ").append(profileKey).append("\n");
        if (location != null) {
            stringBuilder.append("location: ").append(location).append("\n");
        }
        if (invitationDate != null) {
            stringBuilder.append("EndDate: ").append(invitationDate.toString()).append("\n");
        }
        if (memberKeys != null && memberKeys.size() > 0) {
            stringBuilder.append("Member Key:\n");
            for (String memberKey : memberKeys) {
                stringBuilder.append("\t").append(memberKey).append("\n");
            }
        }
        return stringBuilder.toString();
    }

}
