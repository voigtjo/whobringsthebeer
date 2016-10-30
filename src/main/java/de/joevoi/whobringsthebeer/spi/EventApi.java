package de.joevoi.whobringsthebeer.spi;

import static de.joevoi.whobringsthebeer.service.OfyService.factory;
import static de.joevoi.whobringsthebeer.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

import de.joevoi.whobringsthebeer.Constants;
import de.joevoi.whobringsthebeer.domain.Event;
import de.joevoi.whobringsthebeer.domain.Group;
import de.joevoi.whobringsthebeer.domain.Profile;
import de.joevoi.whobringsthebeer.form.EventStructureForm;

/**
 * Defines conference APIs.
 */
@Api(name = "event", version = "v1",
        clientIds = {
        Constants.WEB_CLIENT_ID,
        Constants.API_EXPLORER_CLIENT_ID },
        description = "API for the WhoBringsTheBeer Backend application.")
public class EventApi extends BasicApi{
	
	private static final Logger LOG = Logger.getLogger(
			EventApi.class.getName());

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static final Boolean True = null;
    private static final Boolean False = null;

    

    /**
     * Creates a new Event object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException 
     */
    @ApiMethod(name = "saveEvent", path = "saveEvent", httpMethod = HttpMethod.POST)
    public Event saveEvent(final User user, final EventStructureForm eventForm)
        throws UnauthorizedException, NotFoundException {
    	LOG.info("Create Event ...");
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        LOG.info("Create Event: user= " + user.getUserId());
        
        // Allocate Id first, in order to make the transaction idempotent.
        final String userId = user.getUserId();
        
        final String websafeGroupKey = eventForm.getWebsafeGroupKey();
        final String location = eventForm.getLocation();
        final Date eventDate = eventForm.getEventDate();
        LOG.info("Create Event: websafeGroupKey= " + websafeGroupKey + ", location= " + location + ", eventDate= " + eventDate);
        
        Key<Group> groupKey = Key.create(websafeGroupKey);
        final Group group = ofy().load().key(groupKey).now();
		if (group == null) {
			throw new NotFoundException("No Group found with key: " + websafeGroupKey);
		}
        final Key<Event> eventKey = factory().allocateId(groupKey, Event.class);
        final long eventId = eventKey.getId();

        
        // Start a transaction.
        Event eventDb = ofy().transact(new Work<Event>() {
            @Override
            public Event run() {
            	System.out.println();
            	LOG.warning("Create Event: run ...");
                // Fetch user's Profile.
                Event event = new Event(eventId, location, eventDate, websafeGroupKey, userId);
                String websafeEventKey = event.getWebsafeKey();
                group.addEventToGroup(websafeEventKey);
                // Save Event
                ofy().save().entities(event, group).now();
                return event;
            }
        });
        return eventDb;
    }

    @ApiMethod(
            name = "getAllEvents",
            path = "getAllEvents",
            httpMethod = HttpMethod.POST
    )
    public List<Event> getAllEvents() {
        // Find all entities of type Event
        Query<Event> query = ofy().load().type(Event.class).order("eventDate");
        List<Event> events = query.list();
        int size = events.size();
        LOG.info("all events: size = " + size);
        int i = 0;
        for (Event event : events){
        	LOG.info("Event[" + i + "/" + size+ "]= " + event.toString());
        }
        return events;
    }
    
    @ApiMethod(name = "getEventsCreated", path = "getEventsCreated", httpMethod = HttpMethod.POST)
	public List<Event> getEventsCreated(final User user) throws UnauthorizedException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		String userId = user.getUserId();
		Key<Profile> userKey = Key.create(Profile.class, userId);
		return ofy().load().type(Event.class)
				.order("eventDate").ancestor(userKey).list();
	}


    @ApiMethod(
            name = "getEventsOfGroup",
            path = "getEventsOfGroup",
            httpMethod = HttpMethod.POST
    )
    public List<Event> getEventsOfGroup(final User user, @Named("websafeGroupKey") final String websafeGroupKey) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Key<Group> groupKey = Key.create(websafeGroupKey);
        List<Event> events = ofy().load().type(Event.class).ancestor(groupKey).order("eventDate").list();
        int size = events.size();
        LOG.info("events of group: size = " + size);
        int i = 0;
        for (Event event : events){
        	LOG.info("Event[" + i + "/" + size+ "]= " + event.toString());
        }
        return events;
    }

    /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    @ApiMethod(
            name = "getEvent",
            path = "event/{websafeEventKey}",
            httpMethod = HttpMethod.GET
    )
    public Event getEvent(
            @Named("websafeEventKey") final String websafeEventKey)
            throws NotFoundException {
        Key<Event> eventKey = Key.create(websafeEventKey);
        Event event = ofy().load().key(eventKey).now();
        if (event == null) {
            throw new NotFoundException("No Event found with key: " + websafeEventKey);
        }
        return event;
    }

    @ApiMethod(
            name = "registerForEvent",
            path = "event/{websafeEventKey}/registration",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForEvent(final User user,
            @Named("websafeEventKey") final String websafeEventKey)
            throws UnauthorizedException, NotFoundException,
            ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = user.getUserId();

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                Key<Event> eventKey = Key.create(websafeEventKey);

                // Get the Conference entity from the datastore
                Event event = ofy().load().key(eventKey).now();

                // 404 when there is no Conference with the given conferenceId.
                if (event == null) {
                    return new WrappedBoolean (false,
                            "No Event found with key: "
                                    + websafeEventKey);
                }

                // Get the user's Profile entity
                Profile profile = getProfileFromUser(user);

                // Has the user already registered to attend this conference?
                if (profile.getEventKeysToAttend().contains(
                        websafeEventKey)) {
                    return new WrappedBoolean (false, "Already registered");
                } else {
                    // All looks good, go ahead and book the seat
                    profile.addToEventKeysToAttend(websafeEventKey);
                    event.addMemberToEvent(userId);

                    // Save the Event and Profile entities
                    ofy().save().entities(profile, event).now();
                    // We are booked!
                    return new WrappedBoolean(true, "Registration successful");
                }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");

                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Event found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }


   
    @ApiMethod(
            name = "unregisterFromEvent",
            path = "event/{websafeEventKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromEvent(final User user,
                                            @Named("websafeEventKey")
                                            final String websafeEventKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = user.getUserId();
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                Key<Event> eventKey = Key.create(websafeEventKey);
                Event event = ofy().load().key(eventKey).now();
                // 404 when there is no Event with the given eventId.
                if (event == null) {
                    return new  WrappedBoolean(false,
                            "No Event found with key: " + websafeEventKey);
                }

                // Un-registering from the Conference.
                Profile profile = getProfileFromUser(user);
                if (profile.getEventKeysToAttend().contains(websafeEventKey)) {
                    profile.unregisterFromEvent(websafeEventKey);
                    event.unregisterMemberFromEvent(userId);
                    ofy().save().entities(profile, event).now();
                    return new WrappedBoolean(true);
                } else {
                    return new WrappedBoolean(false, "You are not registered for this event");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Event found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else {
                throw new ForbiddenException(result.getReason());
            }
        }
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getEventsToAttend",
            path = "getEventsToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Event> getEventsToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getEventKeysToAttend();
        List<Key<Event>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Event>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }

 
  
    
    @ApiMethod(name = "getGroupsMemberOf", path = "getGroupsMemberOf", httpMethod = HttpMethod.GET)
    public Collection<Group> getGroupsMemberOf(final User user) throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> groupKeyStringsMemberOf = profile.getGroupKeysMemberOf();
        LOG.info("getGroupsMemberOf: groupKeyStringsMemberOf.size()=" + groupKeyStringsMemberOf.size());
        List<Key<Group>> groupKeysMemberOf = new ArrayList<>();
        for (String groupKeyString : groupKeyStringsMemberOf) {
        	LOG.info("getGroupsMemberOf: groupKeyString=" + groupKeyString);
        	groupKeysMemberOf.add(Key.<Group>create(groupKeyString));
        }
        Collection<Group> groups = ofy().load().keys(groupKeysMemberOf).values();
        return groups;
    }

}
