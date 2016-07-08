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

import de.joevoi.whobringsthebeer.form.EventForm;

/**
 * Event class stores event information.
 */
@Entity
@Cache
public class Event {

    @Id
    private long id;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Group> groupKey;
    
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String groupId;
    
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;

    @Index
    private List<String> memberKeys = new ArrayList<>(0);

    @Index private String location;

    private Date eventDate;

   

    /**
     * Just making the default constructor private.
     */
    private Event() {}

    public Event(long id, EventForm eventForm, String groupId, String organizerUserId) {
        this.id = id;
        this.location = eventForm.getLocation();
        this.eventDate = eventForm.getEventDate();
        this.groupKey = Key.create(Group.class, groupId);
        this.groupId = groupId;
        this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
    }
    
    public long getId() {
        return id;
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

    /**
     * Returns organizer's display name.
     *
     * @return organizer's display name. If there is no Profile, return his/her userId.
     */
    public String getOrganizerDisplayName() {
        // Profile organizer = ofy().load().key(Key.create(Profile.class, organizerUserId)).now();
        Profile organizer = ofy().load().key(getProfileKey()).now();
        if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getDisplayName();
        }
    }

	public String getGroupId() {
		return groupId;
	}

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


}
