/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

directDepositApp.service('ddListingService', ['directDepositService', '$resource', '$filter', '$timeout', 'notificationCenterService',
    function (directDepositService, $resource, $filter, $timeout, notificationCenterService) {
    var apListing = $resource('../ssb/:controller/:action',
            {controller: 'AccountListing', action: 'getApAccountsForCurrentUser'}),
        mostRecentPayrollListing = $resource('../ssb/:controller/:action',
            {controller: 'AccountListing', action: 'getLastPayDateInfo'}),
        userPayrollAllocationListing = $resource('../ssb/:controller/:action',
            {controller: 'AccountListing', action: 'getUserPayrollAllocations'});

    // Scope for main listing controller, which contains all allocations (as opposed to *child* listing
    // controllers created by, for example, directive payAccountInfoProposedDesktop, which do not contain
    // all allocations).
    this.mainListingControllerScope = null;

    this.shouldDisplayPriority = true;

    this.getApListing = function (){
        return apListing.query();
    };

    this.getMostRecentPayrollListing = function() {
        return mostRecentPayrollListing.get();
    };

    this.getUserPayrollAllocationListing = function() {
        return userPayrollAllocationListing.get();
    };
    
    // flag to indicate if the listing controller is initialized
    this.init = false;

    // return the state of the listing controller and set the state to initialized
    this.isInit = function(){
        var ret = !!this.init;
        this.init = true;
        return ret;
    };

    // set init to false to force the listing controller to reload
    this.doReload = function(){
        this.init = false;
    };

    this.getApAccount = function() {
        return this.mainListingControllerScope.apAccount;
    };

    this.checkIfTwoDecimalPlaces = function (num) {
        num = String(num);

        var decSpot = num.indexOf('.'),
            result = true;

        if(decSpot >= 0){
            if(num.length - decSpot > 3){
                result = false;
            }
        }

        return result;
    };

    /**
     * Compare two accounts to determine if they're actually the same account
     */
    this.isSameAccount = function(a1, a2) {
        return a1 !== null && a2 !== null && a1.id === a2.id;
    };

    // Validates amount as a valid currency amount and, if not valid, returns error message
    this.getErrorForInvalidCurrencyAmount = function(acct) {
        if(isNaN(acct.amount) || acct.amount <= 0 || !this.checkIfTwoDecimalPlaces(acct.amount) || acct.amount > 99999999.99) {
            return $filter('i18n')('directDeposit.invalid.format.amount');
        }
        return null;
    };

    // Validates percentage and, if not valid, returns error message
    this.getErrorForInvalidPercentage = function(acct) {
        if(isNaN(acct.percent) || acct.percent <= 0 || acct.percent > 100 || !this.checkIfTwoDecimalPlaces(acct.percent)){
            return $filter('i18n')('directDeposit.invalid.format.percent');
        }
        return null;
    };

    /**
     * Determine if more than one account with "Remaining" amount exists.
     * @param acct The account being checked
     * @returns {boolean} True if more than one "Remaining" account exists
     */
    this.accountWithRemainingAmountAlreadyExists = function(acct) {
        var self = this,
            mainListScope = self.mainListingControllerScope,
            allocations,
            found;

        if (!mainListScope.hasPayAccountsProposed) {
            return false;
        }

        allocations = mainListScope.distributions.proposed.allocations;

        found = _.find(allocations, function(alloc) {
            return directDepositService.isRemaining(alloc) && !self.isSameAccount(acct, alloc)
        });

        return !!found;
    };

    this.hasMultipleRemainingAmountAllocations = function() {
        var self = this;

        return self.mainListingControllerScope.hasMultipleRemainingAmountAllocations();
    };

    this.getErrorForInvalidCaseWhereAccountWithRemainingAmountAlreadyExists = function(acct) {
        var self = this;

        if(self.accountWithRemainingAmountAlreadyExists(acct)) {
            return $filter('i18n')('directDeposit.invalid.amount.remaining');
        }

        return null;
    };

    this.getErrorForInvalidRemainingPosition = function(acct) {
        var self = this,
            allocations = self.mainListingControllerScope.distributions.proposed.allocations;

        if (!directDepositService.isLastPriority(acct, allocations)) {
            // It's not in the last position, as is required
            return 'directDeposit.invalid.amount.remaining.placement';
        }

        return null;
    };

    /**
     * Validate amounts in account on scope and confirm that an account with "Remaining" (aka 100%) does not
     * already exist.  Set appropriate error-related data on scope.
     * @param scope Scope on which to set error-related data
     * @param acct Account to check
     * @returns {boolean} True if everything validates
     */
    this.validateAmountForAccount = function (scope, acct) {
        var self = this,
            amountErr = null,
            msg = false;

        if(acct.amountType === 'amount') {
            msg = self.getErrorForInvalidCurrencyAmount(acct);
            if (msg) {
                amountErr = 'amt';
            }
        }
        else if(directDepositService.isRemaining(acct)) {
            msg = self.getErrorForInvalidCaseWhereAccountWithRemainingAmountAlreadyExists(acct);
            if (msg) {
                amountErr = 'rem';
            }
        }
        else if(acct.amountType === 'percentage') {
            msg = self.getErrorForInvalidPercentage(acct);
            if (msg) {
                amountErr = 'pct';
            }
        }
        else {
            amountErr = true;
        }
        
        if (msg) {
            notificationCenterService.addNotification(msg, "error");
        }

        if (amountErr) {
            scope.amountErr = amountErr;
            scope.amountMessage = msg;
        }

        return !amountErr;
    };

    this.validateAmountsForAllAccountsAndSetNotification = function(accounts) {
        var self = this,
            allValid = true,
            displayedMessages = {
                null: true // Don't add a notification if there is no message (i.e. message is null)
            },

            handleValidityCheck = function(msg) {
                // Update overall validity (taking into account all validations thus far)
                // If there is a message, then the validation failed.
                allValid = allValid && !msg;

                // Display message if same message is not already being displayed
                if (!displayedMessages[msg]) {
                    displayedMessages[msg] = true; // Flag as "displayed"

                    notificationCenterService.addNotification(msg, "error");
                }
            };

        // Start fresh
        notificationCenterService.clearNotifications();

        // Validate each account
        _.each(accounts, function(acct) {
            if(acct.amountType === 'amount') {
                handleValidityCheck(self.getErrorForInvalidCurrencyAmount(acct));
            }
            else if(directDepositService.isRemaining(acct)) {
                handleValidityCheck(self.getErrorForInvalidCaseWhereAccountWithRemainingAmountAlreadyExists(acct));
                handleValidityCheck(self.getErrorForInvalidRemainingPosition(acct));
            }
            else if(acct.amountType === 'percentage') {
                handleValidityCheck(self.getErrorForInvalidPercentage(acct));
            }
        });

        return allValid;
    };

    this.updateWhetherHasPayrollRemainingAmount = function() {
        var self = this;

        self.mainListingControllerScope.updateWhetherHasPayrollRemainingAmount();
    };

    this.calculateAmountsBasedOnPayHistory = function() {
        var self =this;

        self.mainListingControllerScope.calculateAmountsBasedOnPayHistory();
    };

}]);
