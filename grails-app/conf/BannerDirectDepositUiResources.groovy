/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

modules = {
    /* Override UI Bootstrap 0.10.0 to use version 0.13.3
     * Override AngularUI Router 0.2.10 to use version 0.2.15 */
    overrides {
        'angularApp' {
            resource id:[plugin: 'banner-ui-ss', file: 'js/angular/ui-bootstrap-tpls-0.10.0.min.js'], url: [plugin: 'banner-direct-deposit-ui', file: 'js/angular/ui-bootstrap-tpls-0.13.3.min.js']
            resource id:[plugin: 'banner-ui-ss', file: 'js/angular/angular-ui-router.min.js'], url: [plugin: 'banner-direct-deposit-ui', file: 'js/angular/angular-ui-router.min.js']
        }
    }

    'angular' {
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'js/angular/angular-route.min.js']
    }

    'bootstrapLTR' {
        dependsOn "jquery"
        defaultBundle environment == "development" ? false : "bootstrap"

        resource url:[plugin: 'banner-ui-ss', file: 'bootstrap/css/bootstrap.css'], attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-ui-ss', file: 'css/bootstrap-fixes.css'], attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-ui-ss', file: 'bootstrap/js/bootstrap.js']
    }

    'bootstrapRTL' {
        dependsOn "jquery"
        defaultBundle environment == "development" ? false : "bootstrap"

        resource url:[plugin: 'banner-ui-ss', file: 'bootstrap/css/bootstrap-rtl.css'], attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-ui-ss', file: 'css/bootstrap-fixes-rtl.css'], attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-ui-ss', file: 'bootstrap/js/bootstrap.js']
    }

    'directDepositApp' {
        dependsOn "angular,glyphicons"

        defaultBundle environment == "development" ? false : "directDepositApp"

        //Main configuration file
        resource url: [plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/app.js']

        // Services
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddListing/ddListing-service.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/services/breadcrumb-service.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/services/notificationcenter-service.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/services/directDeposit-service.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddEditAccount/ddEditAccount-service.js']


        // Controllers
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddListing/ddListing-controller.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddEditAccount/ddEditAccount-controller.js']

        // Filters
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/filters/i18n-filter.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/filters/accountNumMask-filter.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/filters/webAppResourcePath-filter.js']

        // Directives
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddListing/ddListing-directive.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/ddEditAccount/ddEditAccount-directive.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/directives/enterKey-directive.js']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'directDepositApp/common/directives/ddPopover-directive.js']

    }

    'generalSsbAppLTR' {
        dependsOn "bannerWebLTR, directDepositApp, bootstrapLTR"

        // CSS
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/main.css'],   attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/responsive.css'], attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/banner-icon-font.css'],   attrs: [media: 'screen, projection']
    }

    'generalSsbAppRTL' {
        dependsOn "bannerWebRTL, directDepositApp, bootstrapRTL"

        // CSS
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/main-rtl.css'],   attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/responsive-rtl.css'],   attrs: [media: 'screen, projection']
        resource url:[plugin: 'banner-direct-deposit-ui', file: 'css/banner-icon-font-rtl.css'],   attrs: [media: 'screen, projection']
    }

}
