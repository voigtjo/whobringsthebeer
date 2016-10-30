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
		}
	}
});