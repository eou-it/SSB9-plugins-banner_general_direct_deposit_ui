/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
generalSsbAppControllers.controller('ddListingController',['$scope', '$rootScope', '$state', '$stateParams', '$modal',
    '$filter', '$q', '$timeout', 'ddListingService', 'ddEditAccountService', 'directDepositService',
    'notificationCenterService',
    function ($scope, $rootScope, $state, $stateParams, $modal, $filter, $q, $timeout, ddListingService, ddEditAccountService,
              directDepositService, notificationCenterService){

        // CONSTANTS
        var REMAINING_NONE = directDepositService.REMAINING_NONE,
            REMAINING_ONE = directDepositService.REMAINING_ONE,
            REMAINING_MULTIPLE = directDepositService.REMAINING_MULTIPLE,

            amountsAreValid = function () {
                var result = true,
                    allocs = $scope.distributions.proposed.allocations;

                if($scope.isEmployee && $scope.hasPayAccountsProposed){
                    result = ddListingService.validateAmountsForAllAccountsAndSetNotification(allocs);
                }

                return result;
            },

            formatCurrency = function(amount) {
                return $filter('currency')(amount, $scope.currencySymbol);
            },

            getAmountType = function(acct) {
                if(acct.percent === 100) {
                    return 'remaining';
                }
                else if(acct.amount !== null) {
                    return 'amount';
                }
                else if(acct.percent !== null) {
                    return 'percentage';
                }
            },

            setupAmountTypes = function(allocations) {
                _.each(allocations, function(alloc) {
                    alloc.amountType = getAmountType(alloc);
                });
            },

            /**
             * Show any notifications slated to be shown on state load.
             * (The timeout is needed in cases where the common platform control bar needs time to load. It
             * may be that it's not a typical concern -- would only affect showing notifications on initial
             * page load -- but it's barely noticeable so doesn't hurt to leave it.)
             */
            displayNotificationsOnStateLoad = function() {
                $timeout(function() {
                    _.each($stateParams.onLoadNotifications, function(notification) {
                        notificationCenterService.addNotification(notification.message, notification.messageType, notification.flashType);
                    });
                }, 200);
            };

        // LOCAL FUNCTIONS
        // ---------------
        /**
         * Select the one AP account that will be displayed to user, according to business rules.
         */
        this.getApAccountFromResponse = function(response) {
            var account = null;

            if (response.length) {
                // Probably only one account has been returned, but if more than one, return the one with the
                // highest priority (i.e. lowest integer value).
                _.each(response, function(acctFromResponse) {
                    if (account === null || acctFromResponse.priority < account.priority) {
                        account = acctFromResponse;
                    }
                });

            }

            return account;
        };

        /**
         * Initialize controller
         */
        this.init = function() {

            var self = this,
                allocations;

            // if the listing controller has already been initialized, then abort
            if(ddListingService.isInit()) return;

            var addAlertRoleToNotificationCenter = function(){
                var work = function(){
                    // check if notification center is in DOM yet
                    if($( "div.notification-center-flyout > ul").attr("class") !== undefined) {
                        $("div.notification-center-flyout > ul").attr({
                            role: "alert",
                            'aria-live': "assertive"
                        });

                        return true;
                    }
                    else {

                        return false;
                    }
                };

                $timeout(work,0).then(
                    function(result){
                        // keep trying to add attributes until it works
                        if(!result){
                            addAlertRoleToNotificationCenter();
                        }
                    }
                );
            };
            addAlertRoleToNotificationCenter();

            ddListingService.mainListingControllerScope = $scope;

            var acctPromises = [ddListingService.getApListing().$promise,
                                ddListingService.getMostRecentPayrollListing().$promise,
                                ddListingService.getUserPayrollAllocationListing().$promise];

            // getApListing
            acctPromises[0].then(
                function (response) {
                    // By default, set A/P account as currently active account, as it can be edited inline (in desktop
                    // view), while payroll accounts can not be.
                    $scope.apAccount = self.getApAccountFromResponse(response);
                    $scope.hasApAccount = !!$scope.apAccount;
                    $scope.accountLoaded = true;

                    // Flag whether AP account exists in rootScope, as certain styling for elements
                    // not using this controller (e.g. breadcrumb panel) depends on knowing this.
                    $rootScope.apAccountExists = $scope.hasApAccount;
            });

            $scope.distributions = {
                mostRecent: null,
                proposed: null
            };

            acctPromises[1].then( function (response) {
                if(response.failure) {
                    notificationCenterService.displayNotification(response.message, $scope.notificationErrorType);
                } else {
                    $scope.distributions.mostRecent = response;
                    $scope.distributions.mostRecent.totalNetFormatted = formatCurrency($scope.distributions.mostRecent.totalNet);
                    $scope.hasPayAccountsMostRecent = !!response.docAccts;
                    $scope.payAccountsMostRecentLoaded = true;
                }
            });

            // getUserPayrollAllocationListing
            acctPromises[2].then( function (response) {
                if(response.failure) {
                    notificationCenterService.displayNotification(response.message, $scope.notificationErrorType);
                } else {
                    $scope.distributions.proposed = response;
                    allocations = response.allocations;
                    $scope.hasPayAccountsProposed = !!allocations.length;
                    $scope.payAccountsProposedLoaded = true;

                    ddEditAccountService.setupPriorities(allocations);
                    setupAmountTypes(allocations);
                    $scope.updatePayrollState();

                    amountsAreValid();

                    // If any allocation is flagged for delete (happens via user checking a checkbox, which
                    // in turn sets the deleteMe property to true), set selectedForDelete.payroll to true,
                    // enabling the "Delete" button.
                    $scope.$watch('distributions.proposed', function () {
                        // Determine if any payroll allocations are selected for delete
                        $scope.selectedForDelete.payroll = _.any(allocations, function(alloc) {
                            return alloc.deleteMe;
                        });
                    }, true);
                }
            });
            
            $q.all(acctPromises).then(function() {
                $scope.calculateAmountsBasedOnPayHistory();
                displayNotificationsOnStateLoad();
            });
        };


        // CONTROLLER VARIABLES
        // --------------------
        $scope.payAccountsMostRecentLoaded = false;
        $scope.payAccountsProposedLoaded = false;
        $scope.hasPayAccountsMostRecent = false;
        $scope.hasPayAccountsProposed = false;
        $scope.payPanelMostRecentCollapsed = false;
        $scope.payPanelProposedCollapsed = false;

        $scope.account = null; // Account currently in edit. Could be A/P or payroll.
        $scope.distributions = null;
        $scope.apAccount = null; // Currently active A/P account.
        $scope.accountLoaded = false;
        $scope.hasApAccount = false;
        $scope.panelCollapsed = false;
        $scope.authorizedChanges = false;
        $scope.hasMaxPayrollAccounts = false;
        $scope.hasPayrollRemainingAmount = false;

        $scope.selectedForDelete = {
            payroll: false,
            ap:      false
        };

        $scope.checkAmount = ''; // Amount to be disbursed via paper check

        // Use filter to get resource path.  This is for usages such as ngInclude's src attribute that use
        // an Angular expression and *cannot* use interpolation.
        $scope.webAppResourcePathStr = $filter('webAppResourcePath')('');


        // CONTROLLER FUNCTIONS
        // --------------------

        // Payroll
        $scope.dateStringForPayDistHeader = function () {
            var date = $scope.distributions.mostRecent.payDate;

            return date ? ' as of ' + date : '';
        };

        $scope.totalPayAmounts = function (distribution) {
            var total = 0;

            _.each(distribution.allocations, function(allocation) {
                total += allocation.amount;
            });

            return total;
        };

        // Accounts Payable
        $scope.apListingColumns = [
            { tabindex: '0', title: $filter('i18n')('directDeposit.account.label.bank.name')},
            { title: $filter('i18n')('directDeposit.account.label.routing.num')},
            { title: $filter('i18n')('directDeposit.account.label.account.num')},
            { title: $filter('i18n')('directDeposit.account.label.accountType')},
            { title: $filter('i18n')('directDeposit.account.label.status')}
        ];
        
        // Most Recent Pay
        $scope.mostRecentPayColumns = [
            { tabindex: '0', title: $filter('i18n')('directDeposit.account.label.bank.name')},
            { title: $filter('i18n')('directDeposit.account.label.routing.num')},
            { title: $filter('i18n')('directDeposit.account.label.account.num')},
            { title: $filter('i18n')('directDeposit.account.label.accountType')},
            { title: $filter('i18n')('directDeposit.label.distribution.net.pay')}
        ];
        
        // Proposed Pay
        $scope.proposedPayColumns = [
            { tabindex: '0', title: $filter('i18n')('directDeposit.account.label.bank.name')},
            { title: $filter('i18n')('directDeposit.account.label.routing.num')},
            { title: $filter('i18n')('directDeposit.account.label.account.num')},
            { title: $filter('i18n')('directDeposit.account.label.accountType')},
            { title: $filter('i18n')('directDeposit.account.label.amount')},
            { title: $filter('i18n')('directDeposit.account.label.priority')},
            { title: $filter('i18n')('directDeposit.label.distribution.net.pay')},
            { title: $filter('i18n')('directDeposit.account.label.status')}
        ];

        var openAddOrEditModal = function(typeInd, isAddNew, acctList) {

            $modal.open({
                templateUrl: $filter('webAppResourcePath')('directDepositApp/ddEditAccount/ddEditAccount.html'),
                windowClass: 'edit-account-modal',
                keyboard: true,
                controller: "ddEditAccountController",
                scope: $scope,
                resolve: {
                    editAcctProperties: function () {
                        return { 
                            typeIndicator: typeInd, 
                            creatingNew: !!isAddNew,
                            otherAccounts: acctList || []
                        };
                    }
                }
            });

        };

        // event callback
        var callback = function(result) {
            console.log('callback', result);
        };

        var showSaveCancelMessage = function(){
            var prompt = [{
                label: $filter('i18n')('default.ok.label'),
                action: function () {
                    notificationCenterService.removeNotification('default.savecancel.message');
                }
            }];
            notificationCenterService.displayNotification('default.savecancel.message', 'warning', false, prompt);
        };

        // Display "Add Account" pop up
        $scope.showAddAccount = function (typeInd) {

            if ($scope.editForm.$dirty) {
                showSaveCancelMessage();

            } else {
                //  $rootScope.$broadcast('addNewEvent', callback);
                // If this is an AP account and an AP account already exists, this functionality is disabled.
                if (typeInd === 'AP' && $scope.hasApAccount) {
                    return;
                }

                var acctList = [];

                if($scope.isEmployee){
                    var allocs = $scope.distributions.proposed.allocations;

                    if(typeInd === 'HR' && $scope.apAccount){
                        acctList[0] = $scope.apAccount;
                    }
                    else if (typeInd === 'AP' && allocs.length > 0){
                        acctList = allocs;
                    }
                }

                // Otherwise, open modal
                openAddOrEditModal(typeInd, true, acctList);
            }


        };

        // Display "Edit Account" pop up
        $scope.showEditAccount = function (account, typeInd) {
            // use a copy of the account in modal so changes don't persist in UI
            // if a user cancels their changes
            $scope.account = angular.copy(account);

            // Otherwise, open modal
            openAddOrEditModal(typeInd, false);
        };

        $scope.getNoPayAllocationsNotificationText = function () {
            return ($scope.isDesktopView) ?
                'directDeposit.notification.no.payroll.allocation.click' :
                'directDeposit.notification.no.payroll.allocation.tap';
        };

        $scope.getNoApAllocationsNotificationText = function () {
            return ($scope.isDesktopView) ?
                'directDeposit.notification.no.accounts.payable.allocation.click' :
                'directDeposit.notification.no.accounts.payable.allocation.tap';
        };


        $scope.cancelChanges = function () {
            if ($scope.editForm.$dirty || $scope.selectedForDelete.payroll || $scope.selectedForDelete.ap || $scope.authorizedChanges) {
                $scope.cancelNotification();
                
                var newWarning = new Notification({
                    message: $filter('i18n')('default.cancel.message'),
                    type: "warning"
                });
                newWarning.addPromptAction($filter('i18n')("default.no.label"), function () {
                    notifications.remove(newWarning);
                });
                newWarning.addPromptAction($filter('i18n')("default.yes.label"), function () {
                    notifications.remove(newWarning);
                    $state.go('directDepositListing',
                        {onLoadNotifications: notifications},
                        {reload: true, inherit: false, notify: true}
                    );
                    $scope.editForm.$setPristine();

                });
                notifications.addNotification(newWarning);
            }

        };

        $scope.disableSave = function() {

            var isDisable = false;

            if (!$scope.authorizedChanges ) {
                isDisable = true;
            }
            if (!$scope.editForm.$dirty) {
                isDisable = true;
            }
            return isDisable;
        }


        $scope.disableCancel = function() {

            var isDisable = true;

            if ($scope.editForm.$dirty || $scope.selectedForDelete.payroll || $scope.selectedForDelete.ap || $scope.authorizedChanges) {
                isDisable = false;
            }

            return isDisable;
        }

        $scope.updateAccounts = function () {
            if(!amountsAreValid()) {
                return;
            }

            var allocs = $scope.distributions.proposed.allocations,
                promises = [];

            if(ddEditAccountService.doReorder === 'all'){
                var deferred = $q.defer();

                _.each(allocs, function(alloc){
                    ddEditAccountService.setAmountValues(alloc, alloc.amountType);
                });

                // Temporarily hide priority during this transition. This is because the persisted priority,
                // often different from that displayed to the user, will be set in the account object to save
                // it to the database. However, this results in its being briefly displayed to the user,
                // which could be disconcerting.
                ddListingService.shouldDisplayPriority = false;

                ddEditAccountService.reorderAccounts().$promise.then(function (response) {
                    ddListingService.shouldDisplayPriority = true; // Set priority display back to normal state

                    if(response[0].failure) {
                        notificationCenterService.displayNotification(response[0].message, $scope.notificationErrorType);

                        deferred.reject();
                    }
                    else {
                        ddEditAccountService.doReorder = false;

                        deferred.resolve();
                    }
                });

                promises.push(deferred.promise);
            }
            else {
                if($scope.isEmployee){
                    var i;
                    for(i = 0; i < allocs.length; i++){
                        promises.push(updateAccount($scope.distributions.proposed.allocations[i]));
                    }
                }
                // AP account will already be updated if it has a corresponding Payroll account
                if($scope.hasApAccount && !$scope.getMatchingPayrollForApAccount()){
                    promises.push(updateAccount($scope.apAccount));
                }
            }

            // Handle all promises for updated accounts.
            //
            // NOTE 1: REGARDING REFRESH
            // When all updates are done, a refresh would not be necessary, as the input fields
            // (e.g. Account Type dropdown) will have been already "updated" when the user made the
            // change.  The *exception* to this, and the reason we do indeed refresh here, is because the
            // "Net Pay Distribution" values may need to be recalculated, depending on the change the user made.
            //
            // NOTE 2: REGARDING NOTIFICATIONS
            // If all updates succeed, a page refresh (read $state.go) will be done, with a single "success" message
            // passed in with the $state.go call.
            // If ANY updates fail, the "failure" messages are already displayed.  No page refresh is done, so they
            // will remain displayed to the user.
            $q.all(promises).then(
                // SUCCESSFULLY RESOLVE
                function() {
                    var notifications = [{
                        message: 'default.save.success.message',
                        messageType: $scope.notificationSuccessType,
                        flashType: $scope.flashNotification
                    }];

                    $state.go('directDepositListing',
                        {onLoadNotifications: notifications},
                        {reload: true, inherit: false, notify: true}
                    );
                },
                // REJECTED RESOLVE
                function() {
                    $scope.authorizedChanges = false;
                    ddEditAccountService.setupPriorities($scope.distributions.proposed.allocations);
                }
            );
        };

        var updateAccount = function (acct) {
            var deferred = $q.defer();

            if (acct.hrIndicator === 'A') {
                ddEditAccountService.setAmountValues(acct, acct.amountType);
            }

            ddEditAccountService.saveAccount(acct).$promise.then(function (response) {
                if (response.failure) {
                    // Using addNotification results in displaying stacked error messages if there are more than one.
                    notificationCenterService.addNotification(response.message, "error");

                    deferred.reject();
                } else {
                    // No notification done here as, upon successful resolution of all account updates, the
                    // page will be refreshed, wiping out any notification that would be shown here.  So we
                    // handle any notification in the calling function.
                    deferred.resolve();
                }

            });

            return deferred.promise;
        };

        $scope.toggleApAccountSelectedForDelete = function () {
            $scope.selectedForDelete.ap = !$scope.selectedForDelete.ap;
        };

        $scope.cancelNotification = function () {
            notificationCenterService.clearNotifications();
        };

        $scope.deletePayrollAccount = function () {
            var allocations = $scope.distributions.proposed.allocations,
                accountsToDelete = _.where(allocations, {deleteMe: true}),
                index;

            $scope.cancelNotification();

            ddEditAccountService.deleteAccounts(accountsToDelete).$promise.then(function (response) {
                var notifications = [];

                if (response[0].failure) {
                    notificationCenterService.displayNotification(response[0].message, $scope.notificationErrorType);
                } else {
                    // Refresh account info
                    $scope.distributions.proposed.allocations = _.difference(allocations, accountsToDelete);
                    $scope.updatePayrollState();


                    // Display notification if an account also exists as AP
                    _.find(response, function(item) {
                        if (item.acct) {
                            var msg = $filter('i18n')('directDeposit.account.label.account')+
                                        ' '+ $filter('accountNumMask')(item.acct);

                            if (item.activeType === 'AP'){
                                msg += ' ' + $filter('i18n')('directDeposit.still.active.AP');
                            }

                            notifications.push({message: msg, messageType: "success"});

                            return true;
                        }

                        return false;
                    });

                    $state.go('directDepositListing',
                              {onLoadNotifications: notifications},
                              {reload: true, inherit: false, notify: true}
                    );
                }
            });
        };

        // Display payroll Delete Account confirmation modal
        $scope.confirmPayrollDelete = function () {
            // If no account is selected for deletion, this functionality is disabled
            if (!$scope.selectedForDelete.payroll) return;


            if ($scope.editForm.$dirty) {
                showSaveCancelMessage();
                return;
            }

            var prompts = [
                {
                    label: $filter('i18n')('directDeposit.button.prompt.cancel'),
                    action: $scope.cancelNotification
                },
                {
                    label: $filter('i18n')('directDeposit.button.delete'),
                    action: $scope.deletePayrollAccount
                }
            ];

            notificationCenterService.displayNotification('directDeposit.confirm.payroll.delete.text', 'warning', false, prompts);
        };

        $scope.deleteApAccount = function () {
            var accounts = [];

            $scope.apAccount.apDelete = true;

            accounts.push($scope.apAccount);

            $scope.cancelNotification();

            ddEditAccountService.deleteAccounts(accounts).$promise.then(function (response) {
                var notifications = [];

                if (response[0].failure) {
                    notificationCenterService.displayNotification(response[0].message, $scope.notificationErrorType);
                } else {
                    // Refresh account info
                    $scope.apAccount = null;

                    if (response[0].acct) {
                        var msg = $filter('i18n')('directDeposit.account.label.account')+
                                    ' '+ $filter('accountNumMask')(response[0].acct);

                        if (response[0].activeType === 'PR'){
                            msg += ' '+ $filter('i18n')('directDeposit.still.active.payroll');
                        }

                        notifications.push({message: msg, messageType: "success"});
                    }

                    $state.go('directDepositListing',
                        {onLoadNotifications: notifications},
                        {reload: true, inherit: false, notify: true}
                    );
                }
            });
        };

        // Display accounts payable Delete Account confirmation modal
        $scope.confirmAPDelete = function () {
            // If no account is selected for deletion, this functionality is disabled
            if (!$scope.selectedForDelete.ap) return;

            if ($scope.editForm.$dirty) {
                showSaveCancelMessage();
                return;
            }

            var prompts = [
                {
                    label: $filter('i18n')('directDeposit.button.prompt.cancel'),
                    action: $scope.cancelNotification
                },
                {
                    label: $filter('i18n')('directDeposit.button.delete'),
                    action: $scope.deleteApAccount
                }
            ];

            notificationCenterService.displayNotification('directDeposit.confirm.ap.delete.text', 'warning', false, prompts);
        };

        $scope.toggleAuthorizedChanges = function () {
            $scope.authorizedChanges = !$scope.authorizedChanges;
        };

        $scope.setApAccountType = function (acctType) {
            $scope.apAccount.accountType = acctType;
            this.editForm.$setDirty();

            // Sync with payroll, if applicable
            var matchingPayroll = $scope.getMatchingPayrollForApAccount(),
                prompts = [
                    {
                        label: $filter('i18n')('default.ok.label'),
                        action: $scope.cancelNotification
                    }
                ];

            if (matchingPayroll) {
                matchingPayroll.accountType = acctType;
                notificationCenterService.displayNotification('directDeposit.notification.change.applied.to.both', $scope.notificationWarningType, false, prompts);
            }
        };

        $scope.updateWhetherHasMaxPayrollAccounts = function () {
            if (!$scope.distributions.proposed) {
                $scope.hasMaxPayrollAccounts = false;
            }

            directDepositService.getConfiguration().$promise.then(
                function(response) {
                    var numAllocatons = $scope.distributions.proposed.allocations.length;

                    $scope.hasMaxPayrollAccounts = numAllocatons >= response.MAX_USER_PAYROLL_ALLOCATIONS;
            });
        };

        $scope.hasMultipleRemainingAmountAllocations = function() {
            return directDepositService.getRemainingAmountAllocationStatus($scope.distributions.proposed.allocations) === REMAINING_MULTIPLE;
        };

        $scope.updateWhetherHasPayrollRemainingAmount = function () {
            $scope.hasPayrollRemainingAmount = directDepositService.getRemainingAmountAllocationStatus($scope.distributions.proposed.allocations) !== REMAINING_NONE;
        };

        // When payroll state changes, this can be called to refresh properties based on new state.
        $scope.updatePayrollState = function() {
            $scope.hasPayAccountsProposed = !!$scope.distributions.proposed.allocations.length;

            $scope.updateWhetherHasMaxPayrollAccounts();
            $scope.updateWhetherHasPayrollRemainingAmount();
        };

        $scope.calculateAmountsBasedOnPayHistory = function() {
            var totalNet = $scope.distributions.mostRecent.totalNet,
                totalLeft = totalNet, // The amount left and the amount total are the same at this point
                proposed = $scope.distributions.proposed,
                allocations = proposed.allocations,
                amt, pct, calcAmt, rawAmt,
                allocationByUser,
                totalAmt = 0;

            _.each(allocations, function(alloc) {
                if(alloc.amountType === 'amount') {
                    // Clear out percent in case type changed from percent or Remaining to amount
                    alloc.percent = null;

                    amt = alloc.amount;
                    calcAmt = (amt > totalLeft) ? totalLeft : amt;
                    allocationByUser = formatCurrency(amt);
                } else if (directDepositService.isRemaining(alloc)) {
                    // Clear out amount in case type changed from amount to remaining
                    alloc.amount = null;

                    calcAmt = totalLeft;
                    allocationByUser = '100%';
                } else if (alloc.amountType === 'percentage') {
                    // Clear out amount in case changed from amount to percent
                    alloc.amount = null;

                    pct = alloc.percent;
                    rawAmt = totalLeft * pct / 100;
                    calcAmt = directDepositService.roundAsCurrency(rawAmt);

                    if (calcAmt > totalLeft) {
                        calcAmt = totalLeft;
                    }

                    allocationByUser = pct + '%';
                }

                totalLeft -= calcAmt;
                totalAmt += calcAmt;

                alloc.calculatedAmount = formatCurrency(calcAmt);
                alloc.allocation = allocationByUser;
            });

            $scope.checkAmount = formatCurrency(totalNet - totalAmt); // Amount left to be disbursed via paper check
            proposed.totalAmount = formatCurrency(totalNet);
        };

        // Determine if AP account also exists in proposed allocations
        $scope.getMatchingPayrollForApAccount = function() {
            var allocs = $scope.distributions.proposed.allocations;

            if (!(allocs && $scope.apAccount)) {
                return undefined; // Same as what _.find returns for no hits
            }

            return _.find(allocs, function(alloc) {
                return $scope.apAccount.id === alloc.id;
            });
        };

        $scope.isRemaining = function(alloc) {
            return directDepositService.isRemaining(alloc);
        };


        // INITIALIZE
        // ----------
        this.init();
    }
]);
