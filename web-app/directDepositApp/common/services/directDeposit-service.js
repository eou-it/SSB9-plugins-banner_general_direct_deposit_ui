/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

directDepositApp.service('directDepositService', ['$rootScope', '$resource', function ($rootScope, $resource) {

    var fetchRoles = $resource('../ssb/:controller/:action',
            {controller: 'General', action: 'getRoles'}, {query: {method:'GET', isArray:false}}),
        fetchConfig = $resource('../ssb/:controller/:action',
            {controller: 'DirectDepositConfiguration', action: 'getConfig'}),
        fetchCurrencySymbol = $resource('../ssb/:controller/:action',
            {controller: 'UpdateAccount', action: 'getCurrency'});


    // CONSTANTS
    this.REMAINING_NONE = 0;
    this.REMAINING_ONE = 1;
    this.REMAINING_MULTIPLE = 2;
    this.AUDIBLE_MSG_UPDATED = 'audible-msg-updated';


    this.getRoles = function () {
        return fetchRoles.query();
    };

    this.config = null;

    this.getConfiguration = function () {
        // Retrieve configuration just once; don't make a round trip each time it's requested.
        if (!this.config) {
            this.config = fetchConfig.get();
        }

        return this.config;
    };

    this.getCurrencySymbol = function () {
        return fetchCurrencySymbol.get();
    }

    // Destroy all popovers (i.e. Bootstrap popovers)
    this.destroyAllPopovers = function (){
        // When created, the actual popover is the next sibling adjacent to the
        // AngularJS popover element.  The actual popover has the '.popover.in'
        // CSS selector.  Here's a diagram:
        //
        //     ANGULARJS ELEMENT              ACTUAL POPOVER VISIBLE TO USER
        //     <dd-pop-over ...></dd-pop-over><div class="popover in" ...> ... </div>
        //
        // Thus the previous sibling (grabbed with prev()) is the
        // AngularJS popover element that needs to have 'destroy' called on it.
        $('body').find('.popover.in').prev().popover('destroy');
    };

    /**
     * Does this account have an amount of "Remaining"?
     * @param account
     * @returns {boolean}
     */
    this.isRemaining = function(account) {
        var amountType = account.amountType;

        // CASE 1: Allocation returned from ddListingService function getUserPayrollAllocationListing in this way.
        // CASE 2: Self-explanatory
        // CASE 3: A percentage of 100% is the same as "Remaining" from a business rules perspective.
        // Note that the type-converting equality comparison (== as opposed to the strict ===) for
        // percent is necessary as the value is a string, and it is being compared with a number.
        return !amountType && account.allocation === "100%" ||          // CASE 1
               amountType === 'remaining' ||                            // CASE 2
               (amountType === 'percentage' && account.percent == 100); // CASE 3
    };

    this.getRemainingAmountAllocationStatus = function(allocations) {
        var self = this,
            hasAccountWithRemaining = false,
            secondAlloc;

        if (!(allocations && allocations.length)) {
            return self.REMAINING_NONE;
        }

        // Check for invalid case: there are multiple allocations with an amount of "Remaining".
        // (Find first duplicate allocation, then stop iterating.)
        secondAlloc = _.find(allocations, function(alloc) {
            if (self.isRemaining(alloc)) {
                if (hasAccountWithRemaining) {
                    // We already found a "Remaining" one; this is a second one!
                    return true;
                } else {
                    // This is first one we've found; one such allocation is fine.
                    hasAccountWithRemaining = true;
                }
            }
        });

        return secondAlloc ? self.REMAINING_MULTIPLE : hasAccountWithRemaining ? self.REMAINING_ONE : self.REMAINING_NONE;
    };

    /**
     * According to its priority, is account the last one in the allocation list?
     * @param acct Account to check
     * @param accounts List of accounts to check it against
     * @returns {boolean}
     */
    this.isLastPriority = function(acct, accounts) {
        var numAccounts = accounts && accounts.length;

        if (!(numAccounts && acct)) {
            return false;
        }

        return acct.priority === numAccounts;
    };

    /**
     * Round to two decimal places
     * @param amt
     * @returns {number}
     */
    this.roundAsCurrency = function(amt) {
        return +(Math.round(amt + "e+2")  + "e-2");
    };

    /**
     * Set up an audible message for a screen reader, if any, to read.
     * @param msg The message to voice
     * @param popoverElement Element to which popover is anchored
     */
    this.setPlayAudibleMessage = function (msg, popoverElement) {
        var self = this;

        // Set up audible message to reset when Bootstrap popover closes
        popoverElement.on('hide.bs.popover', function(event) {
            // Reset message
            $rootScope.playAudibleMessage = null;

            // Broadcast event to notify controllers to update views, as changes to the value of
            // $rootScope.playAudibleMessage are not always implicitly "noticed" by views.
            $rootScope.$broadcast(self.AUDIBLE_MSG_UPDATED);
        });

        $rootScope.playAudibleMessage = msg;
        $rootScope.$broadcast(self.AUDIBLE_MSG_UPDATED);
    };

}]);