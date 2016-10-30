var profileServices = angular.module('profileServices', [])
profileServices.factory('ProfileService', function() {
	var debug = true;
	return {
		
		getProfile : function(getProfileCallback){
			return gapi.client.profile.getProfile().execute(getProfileCallback);
		}
	}
});