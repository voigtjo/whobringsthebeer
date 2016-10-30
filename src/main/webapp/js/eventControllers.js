'use strict';

var eventAppControllers = angular.module('eventControllers',['ui.bootstrap']);

eventAppControllers.controller('ShowEventCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS, EventService, ProfileService) {

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
	$scope.events = [];

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
		$scope.queryEvents();
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
		$scope.queryGroups();
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
		$scope.queryGroups();
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
		return Math.ceil($scope.events.length / $scope.pagination.pageSize);
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
	 * Checks if the target element that invokes the click event has the "disabled" class.
	 *
	 * @param event the click event
	 * @returns {boolean} if the target element that has been clicked has the "disabled" class.
	 */
	$scope.pagination.isDisabled = function (event) {
		return angular.element(event.target).hasClass('disabled');
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
	 * Query the groups depending on the tab currently selected.
	 *
	 */
	$scope.queryEvents = function () {
		$scope.submitted = false;
		if ($scope.selectedTab == 'ALL') {
			$scope.queryEventsAll();
		} else if ($scope.selectedTab == 'YOU_HAVE_CREATED') {
			$scope.getEventsCreated();
		} else if ($scope.selectedTab == 'YOU_WILL_ATTEND') {
			$scope.getEventsToAttend();
		}
	};

	var getEventsCallback = function(resp, text){
		$scope.$apply(function () {
			$scope.loading = false;
			if (resp.error) {
				// The request has failed.
				var errorMessage = resp.error.message || '';
				$scope.messages = 'Failed to query events ' + text + ' : ' + errorMessage;
				$scope.alertStatus = 'warning';
			} else {
				// The request has succeeded.
				$scope.submitted = false;
				$scope.alertStatus = 'success';
				$log.info($scope.messages);

				$scope.events = [];
				angular.forEach(resp.items, function (event) {
					$scope.events.push(event);
				});
			}
			$scope.submitted = true;
		});
	}
	/**
	 * Invokes the group.queryGroups API.
	 */
	$scope.queryEventsAll = function () {
		$scope.loading = true;
		EventService.queryEventsAll(getEventsCallback, '');
	};

	/**
	 * Invokes the event.getEventsCreated method.
	 */
	$scope.getEventsCreated = function () {
		$scope.loading = true;
		EventService.getEventsCreated(getEventsCallback, 'created');
	};

	/**
	 * Retrieves the events to attend by calling the conference.getProfile method and
	 * invokes the conference.getConference method n times where n == the number of the conferences to attend.
	 */
	$scope.getEventsToAttend = function () {
		$scope.loading = true;
		EventService.getEventsCreated(getEventsCallback, 'to attend');
	};
});

///###///
eventAppControllers.controller('EventDetailCtrl', function ($scope, $log, $routeParams, HTTP_ERRORS) {
	$scope.event = {};

	$scope.isUserAttending = false;

	/**
	 * Initializes the event detail page.
	 * Invokes the event.getEvent method and sets the returned event in the $scope.
	 *
	 */
	var getEventCallback = function(resp){
		$scope.$apply(function () {
			$scope.loading = false;
			if (resp.error) {
				// The request has failed.
				var errorMessage = resp.error.message || '';
				$scope.messages = 'Failed to get the event : ' + $routeParams.websafeEventKey
				+ ' ' + errorMessage;
				$scope.alertStatus = 'warning';
				$log.error($scope.messages);
			} else {
				// The request has succeeded.
				$scope.alertStatus = 'success';
				$scope.event = resp.result;
			}
		});
	};
	var getProfileCallback = function(resp){
		$scope.$apply(function () {
			$scope.loading = false;
			if (resp.error) {
				// Failed to get a user profile.
				alert("Event: Failed to get a user profile: " + JSON.stringify(resp.error));
			} else {
				var profile = resp.result;
				for (var i = 0; i < profile.eventKeysToAttend.length; i++) {
					if ($routeParams.websafeEventKey == profile.eventKeysToAttend[i]) {
						// The user is attending the conference.
						$scope.alertStatus = 'info';
						$scope.messages = 'You are member of this event';
						$scope.isUserAttending = true;
					}
				}
			}
		});
	};
	
	$scope.init = function () {
		$scope.loading = true;
		EventService.getEvent(initCallback, {websafeEventKey: $routeParams.websafeEventKey});

		$scope.loading = true;
		// If the user is attending the group, updates the status message and available function.
		ProfileService.getProfile(getProfileCallback);
	};


	/**
	 * Invokes the event.registerForEvent method.
	 */
	$scope.registerForEvent = function () {
		$scope.loading = true;
		gapi.client.event.registerForEvent({
			websafeEventKey: $routeParams.websafeEventKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to register for the event : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Register succeeded.
						$scope.messages = 'Registered for the event';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = true;
					} else {
						$scope.messages = 'Failed to register for the event';
						$scope.alertStatus = 'warning';
					}
				}
			});
		});
	};

	/**
	 * Invokes the event.unregisterFromEvent method.
	 */
	$scope.unregisterFromEvent = function () {
		$scope.loading = true;
		gapi.client.event.unregisterFromEvent({
			websafeEventKey: $routeParams.websafeEventKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to unregister from the event : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);
					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Unregister succeeded.
						$scope.messages = 'Unregistered from the event';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = false;
						$log.info($scope.messages);
					} else {
						var errorMessage = resp.error.message || '';
						$scope.messages = 'Failed to unregister from the event : ' + $routeParams.websafeEventKey +
						' : ' + errorMessage;
						$scope.messages = 'Failed to unregister from the event';
						$scope.alertStatus = 'warning';
						$log.error($scope.messages);
					}
				}
			});
		});
	};
});

///###///
eventAppControllers.controller('CreateEventCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

	/**
	 * The conference object being edited in the page.
	 * @type {{}|*}
	 */
	$scope.event = $scope.event || {};

	$scope.groups = [];
	$scope.group = {};

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
	$scope.isValidEvent = function (eventForm) {
		return !eventForm.$invalid;
	}
	
	$scope.init = function () {
		$scope.loading = true;
		gapi.client.event.getGroupsMemberOf().
		execute(function (resp) {
			$scope.$apply(function () {
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query the groups to attend : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.groups = resp.result.items;
					$scope.loading = false;
					$scope.messages = 'Query succeeded : Groups you will attend (or you have attended)';
					$scope.alertStatus = 'success';
					$log.info($scope.messages);
				}
				$scope.submitted = true;
			});
		});
	};

	$scope.setWebsafeGroupKey = function(){
		$scope.event.websafeGroupKey = $scope.group.websafeKey;
	};
	
	/**
	 * Invokes the conference.createConference API.
	 *
	 * @param conferenceForm the form object.
	 */
	$scope.saveEvent = function (eventForm) {
		if (!$scope.isValidEvent(eventForm)) {
			return;
		}

		$scope.loading = true;
		console.log("###1 $scope.event.location= " + $scope.event.location + ", $scope.event.eventDate=" + $scope.event.eventDate);
		var eventStructureForm = {};
		eventStructureForm.websafeGroupKey = $scope.event.websafeGroupKey;
		eventStructureForm.location = $scope.event.location;
		eventStructureForm.eventDate = $scope.event.eventDate;
		console.log("###3 eventStructureForm:");
		console.log(eventStructureForm);
		gapi.client.event.saveEvent(eventStructureForm).
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to create an event: ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages + ' Event : ' + JSON.stringify($scope.event));

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.messages = 'The event has been created : ' + resp.result.name;
					$scope.alertStatus = 'success';
					$scope.submitted = false;
					$scope.event = {};
					$log.info($scope.messages + ' : ' + JSON.stringify(resp.result));
				}
			});
		});
	};
});
