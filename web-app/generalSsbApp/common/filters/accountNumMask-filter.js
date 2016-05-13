/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
directDepositApp.filter('accountNumMask', function () {
    return function(input){
        var out = '', i;
        
        if(input){
            for(i = 0; i < (input.length - 4); i++)
            {
                out += 'x';
            }
            
            out += input.substring(i);
        }
        
        return out;
    };
});