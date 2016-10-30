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
 * Event class stores event information.
 */
@Entity
@Cache
public class Event {

    @Id
    private long id;
    
    @Index
    private String name;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Group> groupKey;
    
    private String websafeGroupKey;
    
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
	
	public void addInvitationToEvent(String invitationKey) {
		invitationKeys.add(invitationKey);
    }

    @Index private String location;

    @Index private Date eventDate;

   

    /**
     * Just making the default constructor private.
     */
    private Event() {}

    public Event(long id, String location, Date eventDate, String websafeGroupKey, String organizerUserId) {
        this.id = id;
        this.location = location;
        this.eventDate = eventDate == null ? null : new Date(eventDate.getTime());
        this.websafeGroupKey = websafeGroupKey;
        this.groupKey = Key.create(websafeGroupKey);
        this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
    }
    
    public long getId() {
        return id;
    }

	public String getName() {
		return name;
	}

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Group> getGroupKey() {
        return groupKey;
    }

    // Get a String version of the key
    public String getWebsafeKey() {
        return Key.create(groupKey, Event.class, id).getString();
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

    public String getWebsafeGroupKey() {
		return websafeGroupKey;
	}
    
    public String getGroupDisplayName() {
        Group group = ofy().load().key(getGroupKey()).now();
        if (group == null) {
            return getWebsafeGroupKey();
        } else {
            return group.getName();
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

	public Date getEventDate() {
		return eventDate == null ? null : new Date(eventDate.getTime());
	}

	public void addMemberToEvent(String memberKey) {
    	memberKeys.add(memberKey);
    }
	
	public void unregisterMemberFromEvent(String memberKey) {
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
        		.append("websafeGroupKey: ").append(websafeGroupKey).append("\n")
                .append("groupKey: ").append(groupKey).append("\n")
                .append("profileKey: ").append(profileKey).append("\n");
        if (location != null) {
            stringBuilder.append("location: ").append(location).append("\n");
        }
        if (eventDate != null) {
            stringBuilder.append("EndDate: ").append(eventDate.toString()).append("\n");
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
