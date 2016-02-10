/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

generalSsbAppDirectives.directive('apAccountInfo',[function () {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/apAccountInformation.html'
    };
}]);

generalSsbAppDirectives.directive('apAccountInfoDesktop',[function () {
    return{
        restrict: 'A',
        templateUrl: '../generalSsbApp/ddListing/apAccountInformationDesktop.html'
    };
}]);

generalSsbAppDirectives.directive('accountType',[function () {
    return{
        restrict: 'E',
        template: "{{(account.accountType === 'C' ? 'directDeposit.account.type.checking' : 'directDeposit.account.type.savings')|i18n}}",
        scope: {
            account: '='
        }
    };
}]);

generalSsbAppDirectives.directive('accountStatus', ['$filter', function ($filter) {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/accountStatus.html',
        scope: {
            account: '=',
            type: '@'
        },
        link: function(scope, element, attrs) {
            // Observe "account" to be sure it has been (re)loaded when a change is made, e.g. when a new account created
            attrs.$observe('account', function() {
                var isPrenote = scope.account.status === 'P',
                    statusProp = isPrenote ? 'directDeposit.account.status.prenote' : 'directDeposit.account.status.active';

                scope.statusText = $filter('i18n')(statusProp);
                scope.statusClass = isPrenote ? 'status-prenote' : 'status-active';
            });
        }
    };
}]);

generalSsbAppDirectives.directive('payListingPanelPopulatedMostRecent',[function () {
    return{
        restrict: 'E',
        link: function(scope) {
            var type = scope.isDesktopView ? 'Desktop' : '';
            scope.mostRecentPayPanelPopulatedTemplate = '../generalSsbApp/ddListing/payListingPanelPopulatedMostRecent' + type + '.html'
        },
        template: '<div ng-include="mostRecentPayPanelPopulatedTemplate"></div>'
    };
}]);

generalSsbAppDirectives.directive('payAccountInfoMostRecent',[function () {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/payAccountInformationMostRecent.html',
        scope: {
            payHistoryDist: '='
        }
    };
}]);

/* 
 * relies on the dist variable from the ng-repeat in payListingPanelPopulatedMostRecentDesktop.html 
 */
generalSsbAppDirectives.directive('payAccountInfoMostRecentDesktop',[function () {
    return{
        restrict: 'A',
        templateUrl: '../generalSsbApp/ddListing/payAccountInformationMostRecentDesktop.html'
    };
}]);

generalSsbAppDirectives.directive('payListingPanelPopulatedProposed',[function () {
    return{
        restrict: 'E',
        link: function(scope) {
            var type = scope.isDesktopView ? 'Desktop' : '';
            scope.proposedPayPanelPopulatedTemplate = '../generalSsbApp/ddListing/payListingPanelPopulatedProposed' + type + '.html'
        },
        template: '<div ng-include="proposedPayPanelPopulatedTemplate"></div>'
    };
}]);

generalSsbAppDirectives.directive('payAccountInfoProposed', ['ddEditAccountService', function (ddEditAccountService) {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/payAccountInformationProposed.html',
        controller: 'ddListingController',
        link: function(scope, elem, attrs, ctrl){
            scope.alloc = scope.allocation;
            
            scope.showEditPayroll = function(){
                scope.showEditAccount(scope.alloc, 'HR');
            };
        }
    };
}]);

/* 
 * relies on the allocation variable from the ng-repeat in payListingPanelPopulatedProposedDesktop.html 
 */
generalSsbAppDirectives.directive('payAccountInfoProposedDesktop',['directDepositService', 'ddEditAccountService',
    'ddListingService', '$filter', 'notificationCenterService',
    function (directDepositService, ddEditAccountService, ddListingService, $filter, notificationCenterService) {

    return{
        restrict: 'A',
        templateUrl: '../generalSsbApp/ddListing/payAccountInformationProposedDesktop.html',
        controller: 'ddListingController',
        link: function(scope, elem, attrs, ctrl){
            scope.alloc = scope.allocation;
            
            scope.amtDropdownOpen = false;

            scope.previousAmount = null; // Holds previous amount info in case it needs to be restored

            scope.preserveNotifications = false;

            scope.setAllocationAcctType = function(type){
                scope.alloc.accountType = type;
            };

            scope.displayAllocationVal = function () {
                if(directDepositService.isRemaining(scope.alloc)){
                    scope.alloc.allocation = $filter('i18n')('directDeposit.account.label.remaining');
                }
                else if(scope.alloc.amountType === 'percentage'){
                    scope.alloc.allocation = $filter('number')(scope.alloc.percent ? scope.alloc.percent : '0') + '%';
                }
                else if(scope.alloc.amountType === 'amount'){
                    scope.alloc.allocation = $filter('currency')((scope.alloc.amount ? scope.alloc.amount : '0'), scope.currencySymbol);
                }
                return scope.alloc.allocation;
            };

            scope.priorities = ddEditAccountService.priorities;

            scope.setAccountPriority = function (priority) {
                if(scope.alloc.priority != priority) {
                    if (ddListingService.hasMultipleRemainingAmountAllocations()) {
                        notificationCenterService.displayNotifications($filter('i18n')('directDeposit.invalid.amount.remaining', "error"));
                    } else {
                        ddEditAccountService.doReorder = 'all';
                        ddEditAccountService.setAccountPriority(scope.alloc, priority);

                        // Reprioritization can change allocation amounts -- recalculate
                        ddListingService.calculateAmountsBasedOnPayHistory();
                    }
                }
            };

            scope.restorePreviousAmount = function() {
                var alloc = scope.alloc;

                alloc.amountType = scope.previousAmount.amountType;
                alloc.amount = scope.previousAmount.amount;
                alloc.percent = scope.previousAmount.percent;
                alloc.allocation = scope.previousAmount.allocation;

                // Just above we *automatically* returned an invalid state to a valid one.  However, we
                // still need to notify the user that their selected state was invalid.  We set this flag here so
                // that a subsequent call to this function, seeing that this *automatically fixed* amount is now
                // valid won't prematurely clear the notification.
                scope.preserveNotifications = true;
            };

            scope.validateAmounts = function (){
                if (ddListingService.hasMultipleRemainingAmountAllocations()) {
                    // In addition to a user *attempting* (in *bold* because we won't allow them to do it) to create
                    // two "Remaining" accounts, this case can also happen when more than one "Remaining" account was
                    // created outside of this app (e.g. INB)
                    scope.restorePreviousAmount();
                    notificationCenterService.displayNotifications($filter('i18n')('directDeposit.invalid.amount.remaining', "error"));
                } else {
                    // Current overall allocation state is valid.  Specifically check the current account.
                    var isValid = ddListingService.validateAmountForAccount(scope, scope.alloc);

                    if(isValid) {
                        if (!scope.preserveNotifications) {
                            notificationCenterService.clearNotifications();
                        }

                        scope.amountErr = false;
                    } else if (scope.amountErr === 'rem') {
                        // If user set it to "Remaining" in an invalid state, return to previous amount values
                        // to avoid issues with a "Remaining" item residing at an invalid position in the allocation list.
                        scope.restorePreviousAmount();
                    }
                }
            };

            scope.isValidRemainingAmountAllocation = function(account){
                return directDepositService.isLastPriority(account, ddEditAccountService.accounts) && directDepositService.isRemaining(account);
            };

            // validate the amounts when the drop down closes
            scope.$watch('amtDropdownOpen', function(newVal, oldVal) {
                // The "newVal != oldVal" phrase keeps this from running on page initialization,
                // (i.e. the state has not changed on the dropdown).
                if (newVal != oldVal) {
                    if (newVal) { // Dropdown has OPENED
                        // Save previous amount in case invalid "Remaining" is entered and we
                        // need to return to previous values.
                        scope.previousAmount = {
                            amountType: scope.alloc.amountType,
                            amount:     scope.alloc.amount ? Number(scope.alloc.amount) : null,
                            percent:    scope.alloc.percent ? Number(scope.alloc.percent) : null,
                            allocation: scope.alloc.allocation
                        };
                    } else { // Dropdown has CLOSED
                        scope.validateAmounts();

                        // Notifications can be cleared at will now; we're past the point where we
                        // need to make sure they hang around.
                        scope.preserveNotifications = false;

                        // If there is one "Remaining" account out of order, whether one just
                        // edited or one already existing that was pulled from the database, fix it.
                        ddEditAccountService.fixOrderForAccountWithRemainingAmount();

                        // Status of an account with "Remaining" status may have changed, so update
                        ddListingService.updateWhetherHasPayrollRemainingAmount();

                        // The amounts for each proposed distribution, based on
                        // most recent pay, may have now changed -- recalculate them.
                        ddListingService.calculateAmountsBasedOnPayHistory();
                    }
                }
            });
        }
    };
}]);

generalSsbAppDirectives.directive('stopClick', [function () {
    return {
        restrict: 'A',
        link: function (scope, elem, attrs) {

            elem.on('click', function(event) {
                event.stopPropagation();
            });
        }
    };
}]);

generalSsbAppDirectives.directive('payListingPanelNonpopulatedProposed',[function () {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/payListingPanelNonpopulatedProposed.html'
    };
}]);

generalSsbAppDirectives.directive('apListingPanelPopulated',['ddEditAccountService', function (ddEditAccountService) {
    return{
        restrict: 'E',
        link: function(scope) {
            var type = scope.isDesktopView ? 'Desktop' : '';
            scope.apListingPanelPopulatedTemplate = '../generalSsbApp/ddListing/apListingPanelPopulated' + type + '.html'

            scope.showEditAP = function(){
                scope.showEditAccount(scope.apAccount, 'AP');
            };
        },
        template: '<div ng-include="apListingPanelPopulatedTemplate"></div>'
    };
}]);

generalSsbAppDirectives.directive('apListingPanelNonpopulated',[function () {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/apListingPanelNonpopulated.html',
    };
}]);

generalSsbAppDirectives.directive('notificationBox',[function () {
    return{
        restrict: 'E',
        templateUrl: '../generalSsbApp/ddListing/ddNotificationBox.html',
        scope: {
            notificationText: '@'
        }
    };
}]);
