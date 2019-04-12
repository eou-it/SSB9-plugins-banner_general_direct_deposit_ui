/*******************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
*******************************************************************************/ 



 /**
  * Specifies all of the URL mappings supported by the application.
  */
class UrlMappings {

    static mappings = {

       if (System.properties['BANNERXE_APP_NAME'].equals('DirectDeposit')) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>URLMAPPING redirected to App >>>>>"+System.properties['BANNERXE_APP_NAME'])
            "ssb/directDeposit" (redirect:[controller: 'directDeposit', action:'landingPage'])
        }
    }
}