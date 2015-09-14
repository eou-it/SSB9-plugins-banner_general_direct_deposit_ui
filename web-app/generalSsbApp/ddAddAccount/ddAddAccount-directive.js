/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

generalSsbAppDirectives.directive('chooseAccount',[function () {
    return{
        restrict: 'E',
        template: "{{( acct.accountType === 'C' ? 'directDeposit.account.type.checking' : " +
                    " ( acct.accountType === 'S' ? 'directDeposit.account.type.savings' : 'directDeposit.account.type.select'))|i18n}}",
        scope: {
            acct: '='
        }
    };
}]);
