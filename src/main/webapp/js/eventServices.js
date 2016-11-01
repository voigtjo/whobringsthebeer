var eventServices = angular.module('eventServices', [])
eventServices.factory('EventService', function() {
	var debug = true;
	return {
		
		queryEventsAll : function(getEventsCallback) {
			return gapi.client.event.getAllEvents().execute(getEventsCallback);
		},
		
		getEventsCreated : function(getEventsCallback) {
			return gapi.client.event.getEventsCreated().execute(getEventsCallback);
		},
		
		getEventsToAttend : function(getEventsCallback) {
			return gapi.client.event.getEventsToAttend().execute(getEventsCallback);
		},
		
		getEvent : function(getEventCallback, websafeEventKey){
			return gapi.client.event.getEvent(websafeEventKey).execute(getEventCallback);
		},
		
		registerForEvent : function(getRegisterCallback, websafeEventKey){
			return gapi.client.event.registerForEvent(websafeEventKey).execute(getRegisterCallback);
		},
		
		unregisterFromEvent : function(getRegisterCallback, websafeEventKey){
			return gapi.client.event.registerForEvent(websafeEventKey).execute(getRegisterCallback);
		},
		
		getGroupsMemberOf : function(getGroupsCallback) {
			return gapi.client.event.getGroupsMemberOf().execute(getGroupsCallback);
		},
		
		saveEvent : function(eventStructureForm, saveEventCallback) {
			return gapi.client.event.saveEvent(eventStructureForm).execute(saveEventCallback);
		}
	}
});