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
import de.joevoi.whobringsthebeer.domain.Invitation;
import de.joevoi.whobringsthebeer.domain.Event;
import de.joevoi.whobringsthebeer.domain.Profile;
import de.joevoi.whobringsthebeer.form.InvitationStructureForm;

/**
 * Defines conference APIs.
 */
@Api(name = "invitation", version = "v1",
        clientIds = {
        Constants.WEB_CLIENT_ID,
        Constants.API_EXPLORER_CLIENT_ID },
        description = "API for the WhoBringsTheBeer Backend application.")
public class InvitationApi extends BasicApi{
	
	private static final Logger LOG = Logger.getLogger(
			InvitationApi.class.getName());

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static final Boolean True = null;
    private static final Boolean False = null;

    

    /**
     * Creates a new Invitation object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException 
     */
    @ApiMethod(name = "saveInvitation", path = "saveInvitation", httpMethod = HttpMethod.POST)
    public Invitation saveInvitation(final User user, final InvitationStructureForm invitationForm)
        throws UnauthorizedException, NotFoundException {
    	LOG.info("Create Invitation ...");
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        LOG.info("Create Invitation: user= " + user.getUserId());
        
        // Allocate Id first, in order to make the transaction idempotent.
        final String userId = user.getUserId();
        
        final String websafeEventKey = invitationForm.getWebsafeEventKey();
        final Date invitationDate = invitationForm.getInvitationDate();
        LOG.info("Create Invitation: websafeEventKey= " + websafeEventKey + ", invitationDate= " + invitationDate);
        
        Key<Event> eventKey = Key.create(websafeEventKey);
        final Event event = ofy().load().key(eventKey).now();
		if (event == null) {
			throw new NotFoundException("No Event found with key: " + websafeEventKey);
		}
        final Key<Invitation> invitationKey = factory().allocateId(eventKey, Invitation.class);
        final long invitationId = invitationKey.getId();

        
        // Start a transaction.
        Invitation invitationDb = ofy().transact(new Work<Invitation>() {
            @Override
            public Invitation run() {
            	System.out.println();
            	LOG.warning("Create Invitation: run ...");
                // Fetch user's Profile.
                Invitation invitation = new Invitation(invitationId, invitationDate, websafeEventKey, userId);
                String websafeInvitationKey = invitation.getWebsafeKey();
                event.addInvitationToEvent(websafeInvitationKey);
                // Save Invitation
                ofy().save().entities(invitation, event).now();
                return invitation;
            }
        });
        return invitationDb;
    }

    @ApiMethod(
            name = "getAllInvitations",
            path = "getAllInvitations",
            httpMethod = HttpMethod.POST
    )
    public List<Invitation> getAllInvitations() {
        // Find all entities of type Invitation
        Query<Invitation> query = ofy().load().type(Invitation.class).order("invitationDate");
        List<Invitation> invitations = query.list();
        int size = invitations.size();
        LOG.info("all invitations: size = " + size);
        int i = 0;
        for (Invitation invitation : invitations){
        	LOG.info("Invitation[" + i + "/" + size+ "]= " + invitation.toString());
        }
        return invitations;
    }
    
    @ApiMethod(name = "getInvitationsCreated", path = "getInvitationsCreated", httpMethod = HttpMethod.POST)
	public List<Invitation> getInvitationsCreated(final User user) throws UnauthorizedException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		String userId = user.getUserId();
		Key<Profile> userKey = Key.create(Profile.class, userId);
		return ofy().load().type(Invitation.class)
				.order("invitationDate").ancestor(userKey).list();
	}


    @ApiMethod(
            name = "getInvitationsOfEvent",
            path = "getInvitationsOfEvent",
            httpMethod = HttpMethod.POST
    )
    public List<Invitation> getInvitationsOfEvent(final User user, @Named("websafeEventKey") final String websafeEventKey) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Key<Event> eventKey = Key.create(websafeEventKey);
        List<Invitation> invitations = ofy().load().type(Invitation.class).ancestor(eventKey).order("invitationDate").list();
        int size = invitations.size();
        LOG.info("invitations of event: size = " + size);
        int i = 0;
        for (Invitation invitation : invitations){
        	LOG.info("Invitation[" + i + "/" + size+ "]= " + invitation.toString());
        }
        return invitations;
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
            name = "getInvitation",
            path = "invitation/{websafeInvitationKey}",
            httpMethod = HttpMethod.GET
    )
    public Invitation getInvitation(
            @Named("websafeInvitationKey") final String websafeInvitationKey)
            throws NotFoundException {
        Key<Invitation> invitationKey = Key.create(websafeInvitationKey);
        Invitation invitation = ofy().load().key(invitationKey).now();
        if (invitation == null) {
            throw new NotFoundException("No Invitation found with key: " + websafeInvitationKey);
        }
        return invitation;
    }

    @ApiMethod(
            name = "registerForInvitation",
            path = "invitation/{websafeInvitationKey}/registration",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForInvitation(final User user,
            @Named("websafeInvitationKey") final String websafeInvitationKey)
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

                Key<Invitation> invitationKey = Key.create(websafeInvitationKey);

                // Get the Conference entity from the datastore
                Invitation invitation = ofy().load().key(invitationKey).now();

                // 404 when there is no Conference with the given conferenceId.
                if (invitation == null) {
                    return new WrappedBoolean (false,
                            "No Invitation found with key: "
                                    + websafeInvitationKey);
                }

                // Get the user's Profile entity
                Profile profile = getProfileFromUser(user);

                // Has the user already registered to attend this conference?
                if (profile.getInvitationKeysToAttend().contains(
                        websafeInvitationKey)) {
                    return new WrappedBoolean (false, "Already registered");
                } else {
                    // All looks good, go ahead and book the seat
                    profile.addToInvitationKeysToAttend(websafeInvitationKey);
                    invitation.addMemberToInvitation(userId);

                    // Save the Invitation and Profile entities
                    ofy().save().entities(profile, invitation).now();
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
            if (result.getReason().contains("No Invitation found with key")) {
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
            name = "unregisterFromInvitation",
            path = "invitation/{websafeInvitationKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromInvitation(final User user,
                                            @Named("websafeInvitationKey")
                                            final String websafeInvitationKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        final String userId = user.getUserId();
        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                Key<Invitation> invitationKey = Key.create(websafeInvitationKey);
                Invitation invitation = ofy().load().key(invitationKey).now();
                // 404 when there is no Invitation with the given invitationId.
                if (invitation == null) {
                    return new  WrappedBoolean(false,
                            "No Invitation found with key: " + websafeInvitationKey);
                }

                // Un-registering from the Conference.
                Profile profile = getProfileFromUser(user);
                if (profile.getInvitationKeysToAttend().contains(websafeInvitationKey)) {
                    profile.unregisterFromInvitation(websafeInvitationKey);
                    invitation.unregisterMemberFromInvitation(userId);
                    ofy().save().entities(profile, invitation).now();
                    return new WrappedBoolean(true);
                } else {
                    return new WrappedBoolean(false, "You are not registered for this invitation");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Invitation found with key")) {
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
            name = "getInvitationsToAttend",
            path = "getInvitationsToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Invitation> getInvitationsToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getInvitationKeysToAttend();
        List<Key<Invitation>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Invitation>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }

 
  
    
    @ApiMethod(name = "getEventsMemberOf", path = "getEventsMemberOf", httpMethod = HttpMethod.GET)
    public Collection<Event> getEventsMemberOf(final User user) throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> eventKeyStringsToAttend = profile.getEventKeysToAttend();
        LOG.info("getEventsMemberOf: eventKeyStringsToAttend.size()=" + eventKeyStringsToAttend.size());
        List<Key<Event>> eventKeysMemberOf = new ArrayList<>();
        for (String eventKeyString : eventKeyStringsToAttend) {
        	LOG.info("getEventsMemberOf: eventKeyString=" + eventKeyString);
        	eventKeysMemberOf.add(Key.<Event>create(eventKeyString));
        }
        Collection<Event> events = ofy().load().keys(eventKeysMemberOf).values();
        return events;
    }

}
