package de.joevoi.whobringsthebeer.domain;

import static de.joevoi.whobringsthebeer.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;

@Entity
@Cache
public class Group {
    @Id
    private long id;
    
    @Index
    private String name;
    
    private String description;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;
    
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;
    
    @Index
    private List<String> memberKeys = new ArrayList<>(0);

	public Group() {
		super();
	}

	public Group(long id, String name, String description, String organizerUserId) {
		Preconditions.checkNotNull(name, "The name is required");
		this.id = id;
		this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
		this.name = name;
		this.description = description;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Profile> getProfileKey() {
        return profileKey;
    }

    // Get a String version of the key
    public String getWebsafeKey() {
        return Key.create(profileKey, Group.class, id).getString();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getOrganizerUserId() {
        return organizerUserId;
    }
    
    public String getOrganizerDisplayName() {
        Profile organizer = ofy().load().key(getProfileKey()).now();
        if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getDisplayName();
        }
    }

    /**
     * Returns a defensive copy of memberKeys if not null.
     * @return a defensive copy of memberKeys if not null.
     */
	public List<String> getMemberKeys() {
		return memberKeys == null ? null : ImmutableList.copyOf(memberKeys);
	}
	
    public void update(String name, String description){
    	if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
    }
    
    public void addMemberToGroup(String memberKey) {
    	memberKeys.add(memberKey);
    }

    public void unregisterMemberFromGroup(String memberKey) {
        if (memberKeys.contains(memberKey)) {
        	memberKeys.remove(memberKey);
        } else {
            throw new IllegalArgumentException("Invalid memberKey: " + memberKey);
        }
    }
    
}
