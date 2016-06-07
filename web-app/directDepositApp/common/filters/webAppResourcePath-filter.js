/*******************************************************************************
 Copyright 2016 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
directDepositApp.filter('webAppResourcePath', ['webAppResourcePathString', function (webAppResourcePathString) {
    return function(input){
        var separator = input[0] === '/' ? '' : '/';
        return webAppResourcePathString + separator + input;
    };
}]);
