'use strict';

var profileAppControllers = angular.module('profileControllers',['ui.bootstrap']);

profileAppControllers.controller('MyProfileCtrl',
	    function ($scope, $log, oauth2Provider, HTTP_ERRORS) {
	        $scope.submitted = false;
	        $scope.loading = false;

	        /**
	         * The initial profile retrieved from the server to know the dirty state.
	         * @type {{}}
	         */
	        $scope.initialProfile = {};

	        /**
	         * Candidates for the teeShirtSize select box.
	         * @type {string[]}
	         */
	        $scope.teeShirtSizes = [
	            'XS',
	            'S',
	            'M',
	            'L',
	            'XL',
	            'XXL',
	            'XXXL'
	        ];

	        /**
	         * Initializes the My profile page.
	         * Update the profile if the user's profile has been stored.
	         */
	        $scope.init = function () {
	            var retrieveProfileCallback = function () {
	                $scope.profile = {};
	                $scope.loading = true;
	                gapi.client.conference.getProfile().
	                    execute(function (resp) {
	                        $scope.$apply(function () {
	                            $scope.loading = false;
	                            if (resp.error) {
	                                // Failed to get a user profile.
	                            	console.log("Failed to get a user profile: " + JSON.stringify(resp.error));
	                            } else {
	                                // Succeeded to get the user profile.
	                                $scope.profile.displayName = resp.result.displayName;
	                                $scope.profile.teeShirtSize = resp.result.teeShirtSize;
	                                $scope.initialProfile = resp.result;
	                            }
	                        });
	                    }
	                );
	            };
	            if (!oauth2Provider.signedIn) {
	                var modalInstance = oauth2Provider.showLoginModal();
	                modalInstance.result.then(retrieveProfileCallback);
	            } else {
	                retrieveProfileCallback();
	            }
	        };

	        /**
	         * Invokes the conference.saveProfile API.
	         *
	         */
	        $scope.saveProfile = function () {
	            $scope.submitted = true;
	            $scope.loading = true;
	            gapi.client.conference.saveProfile($scope.profile).
	                execute(function (resp) {
	                    $scope.$apply(function () {
	                        $scope.loading = false;
	                        if (resp.error) {
	                            // The request has failed.
	                            var errorMessage = resp.error.message || '';
	                            $scope.messages = 'Failed to update a profile : ' + errorMessage;
	                            $scope.alertStatus = 'warning';
	                            $log.error($scope.messages + 'Profile : ' + JSON.stringify($scope.profile));

	                            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
	                                oauth2Provider.showLoginModal();
	                                return;
	                            }
	                        } else {
	                            // The request has succeeded.
	                            $scope.messages = 'The profile has been updated';
	                            $scope.alertStatus = 'success';
	                            $scope.submitted = false;
	                            $scope.initialProfile = {
	                                displayName: $scope.profile.displayName,
	                                teeShirtSize: $scope.profile.teeShirtSize
	                            };

	                            $log.info($scope.messages + JSON.stringify(resp.result));
	                        }
	                    });
	                });
	        };
	    })
	;