/*******************************************************************************
 Copyright 2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

directDepositApp.service('ddAccountDirtyService', [function () {
    var pristineAccounts = {};

    this.initializeAccounts = function (accts) {
        _.each(accts, function(acct){
            pristineAccounts[acct.id] = angular.copy(acct);
        });
    };

    this.isAccountDirty = function(acct) {
        var a = pristineAccounts[acct.id];
        return a.bankRoutingInfo.bankRoutingNum !== acct.bankRoutingInfo.bankRoutingNum ||
            a.status !== acct.status ||
            a.apIndicator !== acct.apIndicator ||
            a.hrIndicator !== acct.hrIndicator ||
            a.bankAccountNum !== acct.bankAccountNum ||
            a.amountType !== acct.amountType ||
            a.amount !== acct.amount ||
            a.percent !== acct.percent ||
            a.accountType !== acct.accountType ||
            a.amountType !== acct.amountType ||
            a.priority !== acct.priority;
    };
}]);