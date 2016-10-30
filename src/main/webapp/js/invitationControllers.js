'use strict';

var invitationAppControllers = angular.module('invitationControllers',['ui.bootstrap']);

invitationAppControllers.controller('ShowInvitationCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {
	
	/**
	 * Holds the status if the query is being executed.
	 * @type {boolean}
	 */
	$scope.submitted = false;

	$scope.selectedTab = 'ALL';

	/**
	 * Holds the filters that will be applied when queryConferencesAll is invoked.
	 * @type {Array}
	 */
	$scope.filters = [
	                  ];

	$scope.filtereableFields = [
	                            {enumValue: 'CITY', displayName: 'City'},
	                            {enumValue: 'TOPIC', displayName: 'Topic'},
	                            {enumValue: 'MONTH', displayName: 'Start month'},
	                            {enumValue: 'MAX_ATTENDEES', displayName: 'Max Attendees'}
	                            ]

	/**
	 * Possible operators.
	 *
	 * @type {{displayName: string, enumValue: string}[]}
	 */
	$scope.operators = [
	                    {displayName: '=', enumValue: 'EQ'},
	                    {displayName: '>', enumValue: 'GT'},
	                    {displayName: '>=', enumValue: 'GTEQ'},
	                    {displayName: '<', enumValue: 'LT'},
	                    {displayName: '<=', enumValue: 'LTEQ'},
	                    {displayName: '!=', enumValue: 'NE'}
	                    ];

	/**
	 * Holds the conferences currently displayed in the page.
	 * @type {Array}
	 */
	$scope.invitations = [];

	/**
	 * Holds the state if offcanvas is enabled.
	 *
	 * @type {boolean}
	 */
	$scope.isOffcanvasEnabled = false;

	/**
	 * Sets the selected tab to 'ALL'
	 */
	$scope.tabAllSelected = function () {
		$scope.selectedTab = 'ALL';
		$scope.queryInvitations();
	};

	/**
	 * Sets the selected tab to 'YOU_HAVE_CREATED'
	 */
	$scope.tabYouHaveCreatedSelected = function () {
		$scope.selectedTab = 'YOU_HAVE_CREATED';
		if (!oauth2Provider.signedIn) {
			oauth2Provider.showLoginModal();
			return;
		}
		$scope.queryEvents();
	};

	/**
	 * Sets the selected tab to 'YOU_WILL_ATTEND'
	 */
	$scope.tabYouWillAttendSelected = function () {
		$scope.selectedTab = 'YOU_WILL_ATTEND';
		if (!oauth2Provider.signedIn) {
			oauth2Provider.showLoginModal();
			return;
		}
		$scope.queryEvents();
	};

	/**
	 * Toggles the status of the offcanvas.
	 */
	$scope.toggleOffcanvas = function () {
		$scope.isOffcanvasEnabled = !$scope.isOffcanvasEnabled;
	};

	/**
	 * Namespace for the pagination.
	 * @type {{}|*}
	 */
	$scope.pagination = $scope.pagination || {};
	$scope.pagination.currentPage = 0;
	$scope.pagination.pageSize = 20;
	/**
	 * Returns the number of the pages in the pagination.
	 *
	 * @returns {number}
	 */
	$scope.pagination.numberOfPages = function () {
		return Math.ceil($scope.invitations.length / $scope.pagination.pageSize);
	};

	/**
	 * Returns an array including the numbers from 1 to the number of the pages.
	 *
	 * @returns {Array}
	 */
	$scope.pagination.pageArray = function () {
		var pages = [];
		var numberOfPages = $scope.pagination.numberOfPages();
		for (var i = 0; i < numberOfPages; i++) {
			pages.push(i);
		}
		return pages;
	};

	/**
	 * Checks if the target element that invokes the click invitation has the "disabled" class.
	 *
	 * @param invitation the click invitation
	 * @returns {boolean} if the target element that has been clicked has the "disabled" class.
	 */
	$scope.pagination.isDisabled = function (invitation) {
		return angular.element(invitation.target).hasClass('disabled');
	}

	/**
	 * Adds a filter and set the default value.
	 */
	$scope.addFilter = function () {
		$scope.filters.push({
			field: $scope.filtereableFields[0],
			operator: $scope.operators[0],
			value: ''
		})
	};

	/**
	 * Clears all filters.
	 */
	$scope.clearFilters = function () {
		$scope.filters = [];
	};

	/**
	 * Removes the filter specified by the index from $scope.filters.
	 *
	 * @param index
	 */
	$scope.removeFilter = function (index) {
		if ($scope.filters[index]) {
			$scope.filters.splice(index, 1);
		}
	};

	/**
	 * Query the events depending on the tab currently selected.
	 *
	 */
	$scope.queryInvitations = function () {
		$scope.submitted = false;
		if ($scope.selectedTab == 'ALL') {
			$scope.queryInvitationsAll();
		} else if ($scope.selectedTab == 'YOU_HAVE_CREATED') {
			$scope.getInvitationsCreated();
		} else if ($scope.selectedTab == 'YOU_WILL_ATTEND') {
			$scope.getInvitationsToAttend();
		}
	};

	/**
	 * Invokes the event.queryEvents API.
	 */
	$scope.queryInvitationsAll = function () {
		$scope.loading = true;
		gapi.client.invitation.getAllInvitations().
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query invitations : ' + errorMessage;
					$scope.alertStatus = 'warning';
				} else {
					// The request has succeeded.
					$scope.submitted = false;
					$scope.alertStatus = 'success';
					$log.info($scope.messages);

					$scope.invitations = [];
					angular.forEach(resp.items, function (invitation) {
						$scope.invitations.push(invitation);
					});
				}
				$scope.submitted = true;
			});
		});
	}

	/**
	 * Invokes the invitation.getInvitationsCreated method.
	 */
	$scope.getInvitationsCreated = function () {
		$scope.loading = true;
		gapi.client.invitation.getInvitationsCreated().
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query the invitations created : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.submitted = false;
					$scope.messages = 'Query succeeded : Invitations you have created';
					$scope.alertStatus = 'success';
					$log.info($scope.messages);

					$scope.invitations = [];
					angular.forEach(resp.items, function (invitation) {
						$scope.invitations.push(event);
					});
				}
				$scope.submitted = true;
			});
		});
	};

	/**
	 * Retrieves the invitations to attend by calling the conference.getProfile method and
	 * invokes the conference.getConference method n times where n == the number of the conferences to attend.
	 */
	$scope.getInvitationsToAttend = function () {
		$scope.loading = true;
		gapi.client.invitation.getInvitationsToAttend().
		execute(function (resp) {
			$scope.$apply(function () {
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query the invitations to attend : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.invitations = resp.result.items;
					$scope.loading = false;
					$scope.messages = 'Query succeeded : Invitations you will attend (or you have attended)';
					$scope.alertStatus = 'success';
					$log.info($scope.messages);
				}
				$scope.submitted = true;
			});
		});
	};
});

///###///
invitationAppControllers.controller('InvitationDetailCtrl', function ($scope, $log, $routeParams, HTTP_ERRORS) {
	$scope.invitation = {};

	$scope.isUserAttending = false;

	/**
	 * Initializes the invitation detail page.
	 * Invokes the invitation.getInvitation method and sets the returned invitation in the $scope.
	 *
	 */
	$scope.init = function () {
		$scope.loading = true;
		gapi.client.invitation.getInvitation({
			websafeInvitationKey: $routeParams.websafeInvitationKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to get the invitation : ' + $routeParams.websafeInvitationKey
					+ ' ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);
				} else {
					// The request has succeeded.
					$scope.alertStatus = 'success';
					$scope.invitation = resp.result;
				}
			});
		});

		$scope.loading = true;
		// If the user is attending the event, updates the status message and available function.
		gapi.client.profile.getProfile().execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// Failed to get a user profile.
					alert("Invitation: Failed to get a user profile: " + JSON.stringify(resp.error));
				} else {
					var profile = resp.result;
					for (var i = 0; i < profile.invitationKeysToAttend.length; i++) {
						if ($routeParams.websafeInvitationKey == profile.invitationKeysToAttend[i]) {
							// The user is attending the conference.
							$scope.alertStatus = 'info';
							$scope.messages = 'You are member of this invitation';
							$scope.isUserAttending = true;
						}
					}
				}
			});
		});
	};


	/**
	 * Invokes the invitation.registerForInvitation method.
	 */
	$scope.registerForInvitation = function () {
		$scope.loading = true;
		gapi.client.invitation.registerForInvitation({
			websafeInvitationKey: $routeParams.websafeInvitationKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to register for the invitation : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Register succeeded.
						$scope.messages = 'Registered for the invitation';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = true;
					} else {
						$scope.messages = 'Failed to register for the invitation';
						$scope.alertStatus = 'warning';
					}
				}
			});
		});
	};

	/**
	 * Invokes the invitation.unregisterFromInvitation method.
	 */
	$scope.unregisterFromInvitation = function () {
		$scope.loading = true;
		gapi.client.invitation.unregisterFromInvitation({
			websafeInvitationKey: $routeParams.websafeInvitationKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to unregister from the invitation : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);
					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Unregister succeeded.
						$scope.messages = 'Unregistered from the invitation';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = false;
						$log.info($scope.messages);
					} else {
						var errorMessage = resp.error.message || '';
						$scope.messages = 'Failed to unregister from the invitation : ' + $routeParams.websafeInvitationKey +
						' : ' + errorMessage;
						$scope.messages = 'Failed to unregister from the invitation';
						$scope.alertStatus = 'warning';
						$log.error($scope.messages);
					}
				}
			});
		});
	};
});

///###///
invitationAppControllers.controller('CreateInvitationCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

	/**
	 * The conference object being edited in the page.
	 * @type {{}|*}
	 */
	$scope.invitation = $scope.invitation || {};

	$scope.events = [];
	$scope.event = {};

	/**
	 * Tests if the arugment is an integer and not negative.
	 * @returns {boolean} true if the argument is an integer, false otherwise.
	 */
	$scope.isValidMaxAttendees = function () {
		if (!$scope.conference.maxAttendees || $scope.conference.maxAttendees.length == 0) {
			return true;
		}
		return /^[\d]+$/.test($scope.conference.maxAttendees) && $scope.conference.maxAttendees >= 0;
	}

	/**
	 * Tests if the conference.startDate and conference.endDate are valid.
	 * @returns {boolean} true if the dates are valid, false otherwise.
	 */
	$scope.isValidDates = function () {
		if (!$scope.conference.startDate && !$scope.conference.endDate) {
			return true;
		}
		if ($scope.conference.startDate && !$scope.conference.endDate) {
			return true;
		}
		return $scope.conference.startDate <= $scope.conference.endDate;
	}

	/**
	 * Tests if $scope.conference is valid.
	 * @param conferenceForm the form object from the create_conferences.html page.
	 * @returns {boolean|*} true if valid, false otherwise.
	 */
	$scope.isValidInvitation = function (invitationForm) {
		return !invitationForm.$invalid;
	}
	
	$scope.init = function () {
		$scope.loading = true;
		gapi.client.invitation.getEventsMemberOf().
		execute(function (resp) {
			$scope.$apply(function () {
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query the events to attend : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.events = resp.result.items;
					$scope.loading = false;
					$scope.messages = 'getEventsMemberOf query succeeded : Events member of= ' + $scope.events.length;
					$scope.alertStatus = 'success';
					$log.info($scope.messages);
				}
				$scope.submitted = true;
			});
		});
	};

	$scope.setWebsafeEventKey = function(){
		$scope.invitation.websafeEventKey = $scope.event.websafeKey;
	};
	
	/**
	 * Invokes the conference.createConference API.
	 *
	 * @param conferenceForm the form object.
	 */
	$scope.saveInvitation = function (invitationForm) {
		if (!$scope.isValidInvitation(invitationForm)) {
			return;
		}

		$scope.loading = true;
		console.log("###1 $scope.invitation.location= " + $scope.invitation.location + ", $scope.invitation.invitationDate=" + $scope.invitation.invitationDate);
		var invitationStructureForm = {};
		invitationStructureForm.websafeEventKey = $scope.invitation.websafeEventKey;
		invitationStructureForm.location = $scope.invitation.location;
		invitationStructureForm.invitationDate = $scope.invitation.invitationDate;
		console.log("###3 invitationStructureForm:");
		console.log(invitationStructureForm);
		gapi.client.invitation.saveInvitation(invitationStructureForm).
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to create an invitation: ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages + ' Invitation : ' + JSON.stringify($scope.invitation));

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.messages = 'The invitation has been created : ' + resp.result.name;
					$scope.alertStatus = 'success';
					$scope.submitted = false;
					$scope.invitation = {};
					$log.info($scope.messages + ' : ' + JSON.stringify(resp.result));
				}
			});
		});
	};
});
