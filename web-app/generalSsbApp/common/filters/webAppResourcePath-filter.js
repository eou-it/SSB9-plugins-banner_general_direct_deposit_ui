/*******************************************************************************
 Copyright 2016 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
directDepositApp.filter('webAppResourcePath', ['webAppResourcePathString', function (webAppResourcePathString) {
    return function(input){
        var separator = input.startsWith('/') ? '' : '/';
        return webAppResourcePathString + separator + input;
    };
}]);