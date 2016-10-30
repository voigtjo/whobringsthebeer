package de.joevoi.whobringsthebeer.spi;

import static de.joevoi.whobringsthebeer.service.OfyService.factory;
import static de.joevoi.whobringsthebeer.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
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
import de.joevoi.whobringsthebeer.domain.Group;
import de.joevoi.whobringsthebeer.domain.Profile;
import de.joevoi.whobringsthebeer.form.ProfileForm.TeeShirtSize;

/**
 * Defines group APIs.
 */
@Api(name = "group", version = "v1",
clientIds = {
		Constants.WEB_CLIENT_ID,
		Constants.API_EXPLORER_CLIENT_ID },
		description = "group API for the WhoBringsTheBeer Backend application.")
public class GroupApi extends BasicApi{
	private static final Logger LOG = Logger.getLogger(GroupApi.class.getName());



	@ApiMethod(name = "saveGroup", path = "saveGroup", httpMethod = HttpMethod.POST)
	public Group saveGroup(final User user, Group group) throws UnauthorizedException {
		LOG.warning("saveGroup ...");
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		// Allocate Id first, in order to make the transaction idempotent.
		final String userId = user.getUserId();
		final String name = group.getName();
		final String description = group.getDescription();
		LOG.info("Create Event: user= " + userId);

		Key<Profile> profileKey = Key.create(Profile.class, userId);
		final Key<Group> groupKey = factory().allocateId(profileKey, Group.class);
		final long groupId = groupKey.getId();

		LOG.info("name= " + name + ", description= " + description);

		// Start a transaction.
		Group groupDb = ofy().transact(new Work<Group>() {
			@Override
			public Group run() {
				LOG.warning("Save group: run ...");
				// Fetch user's Profile.
				Profile profile = getProfileFromUser(user);
				Group group = new Group(groupId, name, description, userId);
				// Save Group and Profile.
				ofy().save().entities(group, profile).now();
				return group;
			}
		});
		return groupDb;
	}

	@ApiMethod(name = "getAllGroups",path = "getAllGroups", httpMethod = HttpMethod.POST)
	public List<Group> getAllGroups() {
		// Find all entities of type Group
		Query<Group> query = ofy().load().type(Group.class).order("name");
		return query.list();
	}

	@ApiMethod(name = "getGroupsCreated", path = "getGroupsCreated", httpMethod = HttpMethod.POST)
	public List<Group> getGroupsCreated(final User user) throws UnauthorizedException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		String userId = user.getUserId();
		Key<Profile> userKey = Key.create(Profile.class, userId);
		return ofy().load().type(Group.class)
				.order("name").ancestor(userKey).list();
	}

	@ApiMethod(name = "getGroup", path = "group/{websafeGroupKey}", httpMethod = HttpMethod.GET)
	public Group getGroup(@Named("websafeGroupKey") final String websafeGroupKey) throws NotFoundException {
		Key<Group> groupKey = Key.create(websafeGroupKey);
		Group group = ofy().load().key(groupKey).now();
		if (group == null) {
			throw new NotFoundException("No Group found with key: " + websafeGroupKey);
		}
		return group;
	}

	@ApiMethod(name = "addMemberToGroup", path = "group/{websafeGroupKey}/addMember", httpMethod = HttpMethod.POST)
	public WrappedBoolean addMemberToGroup(final User user, @Named("websafeGroupKey") final String websafeGroupKey)
			throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
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

					// Get the conference key
					// Will throw ForbiddenException if the key cannot be created
					Key<Group> groupKey = Key.create(websafeGroupKey);

					// Get the Group entity from the datastore
					Group group = ofy().load().key(groupKey).now();

					// 404 when there is no Group with the given groupId.
					if (group == null) {
						return new WrappedBoolean (false, "No Group found with key: " + websafeGroupKey);
					}

					// Get the user's Profile entity
					Profile profile = getProfileFromUser(user);

					// Has the user already registered to attend this conference?
					if (profile.getGroupKeysMemberOf().contains(websafeGroupKey)) {
						return new WrappedBoolean (false, "Already registered");
					} else {
						// All looks good, go ahead ...
						profile.addToGroupKeysMemberOf(websafeGroupKey);
						group.addMemberToGroup(userId);

						// Save the Conference and Profile entities
						ofy().save().entities(profile, group).now();
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
			if (result.getReason().contains("No Group found with key")) {
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

	@ApiMethod(name = "removeMemberFromGroup", path = "group/{websafeGroupKey}/removeMember", httpMethod = HttpMethod.DELETE)
	public WrappedBoolean removeMemberFromGroup(final User user, @Named("websafeGroupKey") final String websafeGroupKey)
			throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
		// If not signed in, throw a 401 error.
		if (user == null) {
			throw new UnauthorizedException("Authorization required");
		}
		// Get the userId
		final String userId = user.getUserId();
		WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
			@Override
			public WrappedBoolean run() {
				Key<Group> groupKey = Key.create(websafeGroupKey);
				Group group = ofy().load().key(groupKey).now();
				// 404 when there is no Conference with the given conferenceId.
				if (groupKey == null) {
					return new  WrappedBoolean(false, "No Group found with key: " + websafeGroupKey);
				}

				// Un-registering from the Group.
				Profile profile = getProfileFromUser(user);
				if (profile.getGroupKeysMemberOf().contains(websafeGroupKey)) {
					profile.removeFromGroupKeysMemberOf(websafeGroupKey);
					group.unregisterMemberFromGroup(userId);
					ofy().save().entities(profile, group).now();
					return new WrappedBoolean(true);
				} else {
					return new WrappedBoolean(false, "You are not registered for this group");
				}
			}
		});
		// if result is false
		if (!result.getResult()) {
			if (result.getReason().contains("No Group found with key")) {
				throw new NotFoundException (result.getReason());
			}
			else {
				throw new ForbiddenException(result.getReason());
			}
		}
		// NotFoundException is actually thrown here.
		return new WrappedBoolean(result.getResult());
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

	static class WrappedBoolean {

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

}
