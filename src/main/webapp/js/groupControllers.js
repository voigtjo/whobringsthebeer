///////////////////////////////////////////////////////////

///###///
var groupAppControllers = angular.module('groupControllers',['ui.bootstrap']);

groupAppControllers.controller('ShowGroupCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

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
	$scope.groups = [];

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
		$scope.queryGroups();
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
		return Math.ceil($scope.groups.length / $scope.pagination.pageSize);
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
	$scope.queryGroups = function () {
		$scope.submitted = false;
		if ($scope.selectedTab == 'ALL') {
			$scope.queryGroupsAll();
		} else if ($scope.selectedTab == 'YOU_HAVE_CREATED') {
			$scope.getGroupsCreated();
		} else if ($scope.selectedTab == 'YOU_WILL_ATTEND') {
			$scope.getGroupsMemberOf();
		}
	};

	/**
	 * Invokes the group.queryGroups API.
	 */
	$scope.queryGroupsAll = function () {
		$scope.loading = true;
		gapi.client.group.getAllGroups().
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query groups : ' + errorMessage;
					$scope.alertStatus = 'warning';
				} else {
					// The request has succeeded.
					$scope.submitted = false;
					$scope.alertStatus = 'success';
					$log.info($scope.messages);

					$scope.groups = [];
					angular.forEach(resp.items, function (group) {
						$scope.groups.push(group);
					});
				}
				$scope.submitted = true;
			});
		});
	}

	/**
	 * Invokes the conference.getConferencesCreated method.
	 */
	$scope.getGroupsCreated = function () {
		$scope.loading = true;
		gapi.client.group.getGroupsCreated().
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to query the groups created : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.submitted = false;
					$scope.messages = 'Query succeeded : Groups you have created';
					$scope.alertStatus = 'success';
					$log.info($scope.messages);

					$scope.groups = [];
					angular.forEach(resp.items, function (group) {
						$scope.groups.push(group);
					});
				}
				$scope.submitted = true;
			});
		});
	};

	/**
	 * Retrieves the conferences to attend by calling the conference.getProfile method and
	 * invokes the conference.getConference method n times where n == the number of the conferences to attend.
	 */
	$scope.getGroupsMemberOf = function () {
		$scope.loading = true;
		gapi.client.group.getGroupsMemberOf().
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
});

///###///
groupAppControllers.controller('GroupDetailCtrl', function ($scope, $log, $routeParams, HTTP_ERRORS) {
	$scope.group = {};

	$scope.isUserAttending = false;

	/**
	 * Initializes the conference detail page.
	 * Invokes the conference.getConference method and sets the returned conference in the $scope.
	 *
	 */
	$scope.init = function () {
		$scope.loading = true;
		gapi.client.group.getGroup({
			websafeGroupKey: $routeParams.websafeGroupKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to get the group : ' + $routeParams.websafeGroupKey
					+ ' ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);
				} else {
					// The request has succeeded.
					$scope.alertStatus = 'success';
					$scope.group = resp.result;
				}
			});
		});

		$scope.loading = true;
		// If the user is attending the group, updates the status message and available function.
		gapi.client.conference.getProfile().execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// Failed to get a user profile.
				} else {
					var profile = resp.result;
					for (var i = 0; i < profile.groupKeysMemberOf.length; i++) {
						if ($routeParams.websafeGroupKey == profile.groupKeysMemberOf[i]) {
							// The user is attending the conference.
							$scope.alertStatus = 'info';
							$scope.messages = 'You are member of this group';
							$scope.isUserAttending = true;
						}
					}
				}
			});
		});
	};


	/**
	 * Invokes the conference.registerForConference method.
	 */
	$scope.addMemberToGroup = function () {
		$scope.loading = true;
		gapi.client.group.addMemberToGroup({
			websafeGroupKey: $routeParams.websafeGroupKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to register for the group : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Register succeeded.
						$scope.messages = 'Registered for the group';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = true;
					} else {
						$scope.messages = 'Failed to register for the group';
						$scope.alertStatus = 'warning';
					}
				}
			});
		});
	};

	/**
	 * Invokes the conference.unregisterForConference method.
	 */
	$scope.removeMemberFromGroup = function () {
		$scope.loading = true;
		gapi.client.group.removeMemberFromGroup({
			websafeGroupKey: $routeParams.websafeGroupKey
		}).execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to unregister from the group : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages);
					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					if (resp.result) {
						// Unregister succeeded.
						$scope.messages = 'Unregistered from the conference';
						$scope.alertStatus = 'success';
						$scope.isUserAttending = false;
						$log.info($scope.messages);
					} else {
						var errorMessage = resp.error.message || '';
						$scope.messages = 'Failed to unregister from the group : ' + $routeParams.websafeGroupKey +
						' : ' + errorMessage;
						$scope.messages = 'Failed to unregister from the group';
						$scope.alertStatus = 'warning';
						$log.error($scope.messages);
					}
				}
			});
		});
	};
});

///###///
groupAppControllers.controller('CreateGroupCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

	/**
	 * The conference object being edited in the page.
	 * @type {{}|*}
	 */
	$scope.group = $scope.group || {};

	/**
	 * Holds the default values for the input candidates for city select.
	 * @type {string[]}
	 */
	$scope.cities = [
	                 'Chicago',
	                 'London',
	                 'Paris',
	                 'San Francisco',
	                 'Tokyo'
	                 ];

	/**
	 * Holds the default values for the input candidates for topics select.
	 * @type {string[]}
	 */
	$scope.topics = [
	                 'Medical Innovations',
	                 'Programming Languages',
	                 'Web Technologies',
	                 'Movie Making',
	                 'Health and Nutrition'
	                 ];

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
	$scope.isValidGroup = function (groupForm) {
		return !groupForm.$invalid;
	}

	/**
	 * Invokes the conference.createConference API.
	 *
	 * @param conferenceForm the form object.
	 */
	$scope.saveGroup = function (groupForm) {
		if (!$scope.isValidGroup(groupForm)) {
			return;
		}

		$scope.loading = true;
		console.log("$scope.group.name= " + $scope.group.name + ", $scope.group.description=" + $scope.group.description);
		gapi.client.group.saveGroup($scope.group).
		execute(function (resp) {
			$scope.$apply(function () {
				$scope.loading = false;
				if (resp.error) {
					// The request has failed.
					var errorMessage = resp.error.message || '';
					$scope.messages = 'Failed to create a group : ' + errorMessage;
					$scope.alertStatus = 'warning';
					$log.error($scope.messages + ' Group : ' + JSON.stringify($scope.group));

					if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
						oauth2Provider.showLoginModal();
						return;
					}
				} else {
					// The request has succeeded.
					$scope.messages = 'The group has been created : ' + resp.result.name;
					$scope.alertStatus = 'success';
					$scope.submitted = false;
					$scope.group = {};
					$log.info($scope.messages + ' : ' + JSON.stringify(resp.result));
				}
			});
		});
	};
});