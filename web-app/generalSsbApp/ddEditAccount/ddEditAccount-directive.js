/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

generalSsbAppDirectives.directive('chooseAccount',[function () {
    return{
        restrict: 'E',
        scope: {
            account: '='
        },
        template: "{{( account.accountType === 'C' ? 'directDeposit.account.type.checking' : " +
                    " ( account.accountType === 'S' ? 'directDeposit.account.type.savings' : 'directDeposit.account.type.select'))|i18n}}"
    };
}]);

generalSsbAppDirectives.directive('titleForEditModal',[function () {
    return{
        restrict: 'E',
        link: function(scope){
            scope.title = 'directDeposit.label.';
            
            if(scope.creatingNewAccount){
                scope.title += 'add.';
            }
            else {
                scope.title += 'edit.';
            }
            
            if(scope.typeIndicator === 'HR'){
                scope.title += 'hrDeposit';
            }
            else{
                scope.title += 'apDeposit';
            }
        },
        template: "{{title|i18n}}"
    };
}]);

generalSsbAppDirectives.directive('editModalSaveButton',[function () {
    return{
        restrict: 'E',
        template: "{{(creatingNewAccount === true ? 'directDeposit.button.save.new' : 'directDeposit.button.save.changes')|i18n}}"
    };
}]);

generalSsbAppDirectives.directive('selectBankAcct',['$filter', function ($filter) {
    return{
        restrict: 'E',
        link: function(scope, elem){
            scope.getExistingAcctText = function(){
                var existingAcctText;
                
                if(!scope.otherAccountSelected){
                    existingAcctText = $filter('i18n')('directDeposit.label.select.exisiting');
                }
                else{
                    var bankName = scope.otherAccountSelected.bankRoutingInfo.bankName;
                    var acctNum = scope.otherAccountSelected.bankAccountNum;
                    
                    existingAcctText = bankName;
                    existingAcctText += ' ...' + acctNum.substring(acctNum.length-4);
                    
                    var btnWidth = elem.parent().parent().width();
                    
                    // magic formula to truncate bank name to fit text in button based on estimated
                    // icon width and character widths
                    if(btnWidth - ((existingAcctText.length*8) + 45) < 0){
                        var num = (-(btnWidth - ((existingAcctText.length*8) + 45)))/10 - 1;

                        existingAcctText = bankName.substring(0, bankName.length-num);
                        existingAcctText += ' ...' + acctNum.substring(acctNum.length-4);
                    }
                }
                
                return existingAcctText;
            }
        },
        template: "{{getExistingAcctText()}}"
    };
}]);

generalSsbAppDirectives.directive('truncatedBankName',[function () {
    return {
        restrict: 'E',
        link: function(scope, elem){
            scope.getTruncatedBankName = function(){
                var bankName = scope.account.bankRoutingInfo.bankName,
                    routNum = scope.account.bankRoutingInfo.bankRoutingNum,
                    truncated = bankName,
                    inputWidth = 0,
                    factor = 8;

                if(routNum && routNum.length > 9)
                    factor = 9;

                if(bankName){
                    inputWidth = $('#routing-number').width();
                    
                    // magic formula to truncate bank name to fit text in box based on estimated
                    // icon width and character widths
                    if(inputWidth - ((truncated.length*factor) + 100) < 0){
                        var num = (-(inputWidth - ((truncated.length*factor) + 100)))/10;
    
                        truncated = bankName.substring(0, bankName.length-num);
                        truncated += '...';
                    }
                }
                else {
                    truncated = '';
                }
                
                return truncated;
            };
        },
        template: "{{getTruncatedBankName()}}"
    };
}]);

/* 
 * usage:
 * place dropdown-helper="begin" on the element with the data-toggle attribute so that when user keys to the previous focusable element
 * with shift+tab the dropdown menu is closed. Place dropdown-helper="end" on the last item in the menu so when the user keys to the
 * next focusable element with tab the dropdown menu closes.
 */
generalSsbAppDirectives.directive('dropdownHelper', [function () {
    return {
        restrict: 'A',
        link: function (scope, elem, attrs) {

            elem.bind('keydown', function(event) {
                var code = event.keyCode || event.which;

                if (code === 9) {
                    if(attrs.dropdownHelper === 'begin'){
                        if(event.shiftKey){
                            //close dropdown if it is open when shift+tab off element
                            if(elem.parent().hasClass('open')){
                                elem.dropdown('toggle');
                            }
                        }
                    }
                    else if(attrs.dropdownHelper === 'end'){
                        if(!event.shiftKey){
                            //close dropdown when tab off element, toggle button so events fire as expected
                            elem.parents('ul.dropdown-menu').siblings('button.dropdown-btn').dropdown('toggle');
                        }
                    }
                }
            });
            scope.$on('$destroy', function () {
                elem.unbind('keydown');
            });
        }
    };
}]);

generalSsbAppDirectives.directive('amountMenuControls', ['ddEditAccountService', function (ddEditAccountService) {
    var amountTypes = ['remaining', 'amount', 'percentage'];
    var PREV = -1, NEXT = 1;

    return {
        restrict: 'A',
        scope: {
            amtType: '=selection'
        },
        link: function (scope, elem) {
            var listItems = elem.find('li > input[type=radio]');
            var endItemFocused = false;

            var selectItem = function(direction){
                var selected = (scope.amtType === 'remaining' ? 0: (scope.amtType === 'amount' ? 1 : 2));

                if(selected !== 0 && endItemFocused){
                    // use default arrow key behavior if focused on input number box
                    return false;
                }
                else {
                    selected += direction;

                    if (selected > 2) {
                        selected = 0;
                    }
                    else if (selected < 0) {
                        selected = 2;
                    }

                    scope.amtType = amountTypes[selected];
                    listItems.eq(selected).focus();
                    scope.$apply();

                    return true;
                }
            };

            var recvdFocus = function(){
                endItemFocused = true;
            };
            var lostFocus = function(){
                endItemFocused = false;
            };

            listItems.each(function(index){
                if(index === 0){
                    // add focus event handling to Remaining radio button
                    $(this).focus(recvdFocus);
                    $(this).blur(lostFocus);
                }
                else{
                    // add focus event handling to the input number boxes
                    var inputBox = $(this).siblings("div").children("input[type=number]");
                    inputBox.focus(recvdFocus);
                    inputBox.blur(lostFocus);
                }
            });

            elem.bind('keydown', function(event) {
                var code = event.keyCode || event.which;

                switch (code){
                    case 37: //left arrow key
                    case 38: //up arrow key
                        if(selectItem(PREV)){
                            event.preventDefault();
                        }
                        break;
                    case 39: //right arrow key
                    case 40: //down arrow key
                        if(selectItem(NEXT)){
                            event.preventDefault();
                        }
                        break;
                    case 9: //tab key
                        if (!event.shiftKey) {
                            if(endItemFocused) {
                                // If an error occurs we want to focus on the notification center, not do the default
                                // tab key behavior. To accomplish this we keep the event around until amount validation
                                // has finished. We will then know whether we should call preventDefault() or not.
                                ddEditAccountService.setTabEvent(event);

                                //close dropdown when tab off element, toggle button so events fire as expected
                                elem.siblings('button.dropdown-btn').dropdown('toggle');
                            }
                        }
                        break;
                }
            });
            scope.$on('$destroy', function () {
                elem.unbind('keydown');
            });
        }
    };
}]);

generalSsbAppDirectives.directive('menuControls', [function () {
    var PREV = -1, NEXT = 1;

    return {
        restrict: 'A',
        scope: false,
        link: function (scope, elem) {
            var listItems;

            var getSelectedItem = function(){
                var selected = 0;

                listItems = elem.find('li > a');

                listItems.each(function(index){
                    if($(this).is(':focus')){
                        selected = index;
                        return false;
                    }
                });

                return selected;
            };

            var selectItem = function(direction){
                var selected = getSelectedItem();

                selected += direction;

                if (selected >= listItems.length) {
                    selected = 0;
                }
                else if (selected < 0) {
                    selected = listItems.length - 1;
                }

                listItems.eq(selected).focus();
            };

            var clickSelectedItem = function(direction){
                var selected = getSelectedItem();

                listItems.eq(selected).click();

                elem.siblings("button.dropdown-btn").focus();
            };

            elem.bind('keydown', function(event) {
                var code = event.keyCode || event.which;

                switch (code){
                    case 37: //left arrow key
                    case 38: //up arrow key
                        selectItem(PREV);
                        event.preventDefault();
                        event.stopPropagation();
                        break;
                    case 39: //right arrow key
                    case 40: //down arrow key
                        selectItem(NEXT);
                        event.preventDefault();
                        event.stopPropagation();
                        break;
                    case 13: //enter key
                        clickSelectedItem();
                        event.preventDefault();
                        event.stopPropagation();
                        break;
                }
            });
            scope.$on('$destroy', function () {
                elem.unbind('keydown');
            });
        }
    };
}]);

/*
 * place on div that encapsulates the dropdown to bind a variable to the open/close state of the dropdown
 */
generalSsbAppDirectives.directive('dropdownState', [function () {
    return {
        restrict: 'A',
        scope: {
            state: '=dropdownState'
        },
        link: function (scope, elem) {
            elem.on('shown.bs.dropdown hidden.bs.dropdown', function (event) {
                // state will be true when dropdown is open aka shown, false when it closes
                scope.state = (event.type === 'shown');
                scope.$apply();
            });
        }
    };
}]);

// In Internet Explorer pressing the enter key while in the amount input boxes on the desktop listing page spawns
// dropdown click events on the account type dropdown. Preventing the enter key event will stop
// those dropdown events from generating
generalSsbAppDirectives.directive('suppressEnterKey', [function () {
    return {
        restrict: 'A',
        scope: false,
        link: function (scope, elem) {
            elem.on('keydown', function(event){
                var code = event.keyCode || event.which;

                if(code == 13){
                    event.preventDefault();
                }
            });
        }
    };
}]);

generalSsbAppDirectives.directive('maskInput', ['$filter', function ($filter) {
    return {
        restrict: 'A',
        require: '?ngModel',
        link: function (scope, elem, attrs, ngModel) {
            
            // do nothing if ng-model is not present
            if (!ngModel) return;
            
            ngModel.$render = function(){
                if(!scope.creatingNewAccount){
                    elem.val($filter('accountNumMask')(ngModel.$modelValue));
                }
            };
        }
    };
}]);

generalSsbAppDirectives.directive('modalDisclaimer', ['$filter', function ($filter) {
    return {
        restrict: 'E',
        templateUrl:  $filter('webAppResourcePath')('directDepositApp/ddEditAccount/modalDisclaimer.html'),
        link: function (scope, elem, attrs){
            scope.disclaimerId = attrs.id;

            elem.removeAttr('id');
        }
    };
}]);
