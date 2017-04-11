/*******************************************************************************
 Copyright 2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

directDepositApp.service( 'breadcrumbService', ['$filter', '$rootScope', function ($filter, $rootScope) {
    var constantBreadCrumb = [],
        callingUrl,
        CALLING_URL = 1,
        GEN_LANDING_PAGE_SIGNATURE;

    $rootScope.applicationName = $('meta[name=applicationName]').attr("content");
    GEN_LANDING_PAGE_SIGNATURE = new RegExp('/'+ $rootScope.applicationName +'/ssb/general$');

    this.reset = function() {
        var label;

        constantBreadCrumb = [
            {
                label: 'general.breadcrumb.directDeposit',
                url: ''
            }
        ];

        callingUrl = sessionStorage.getItem('genAppCallingPage');

        if (callingUrl) {
            label = GEN_LANDING_PAGE_SIGNATURE.test(callingUrl) ? 'banner.generalssb.landingpage.title' : 'default.paginate.prev';

            constantBreadCrumb.splice(0, 0, {
                label: label,
                url: CALLING_URL
            });
        }
    };

    this.setBreadcrumbs = function (bc) {
        this.reset();
        constantBreadCrumb.push.apply(constantBreadCrumb, bc);
    };

    this.refreshBreadcrumbs = function() {
        var breadCrumbInputData = {},
            updatedHeaderAttributes,
            backButtonUrl = '',
            registerBackButtonClickListenerOverride = function(location) {
                $('#breadcrumbBackButton').on('click',function(){
                    window.location = location;
                })
            };

        _.each (constantBreadCrumb, function(item) {
            var label = ($filter('i18n')(item.label));

            if (item.url) {
                if (item.url === CALLING_URL) {
                    breadCrumbInputData[label] = callingUrl;
                    backButtonUrl = callingUrl;
                } else {
                    breadCrumbInputData[label] = "/" + document.location.pathname.slice(Application.getApplicationPath().length + 1) + "#" + item.url;
                }
            } else {
                breadCrumbInputData[label] = "";
            }
        });

        updatedHeaderAttributes = {
            "breadcrumb":breadCrumbInputData
        };

        BreadCrumbAndPageTitle.draw(updatedHeaderAttributes);

        // As this is in a consolidated app, the default "previous breadcrumb" logic needs to be overridden to
        // point the back button to the calling page URL.  (Note that the back button is only used for mobile,
        // not desktop.)
        registerBackButtonClickListenerOverride(backButtonUrl);
    };
}]);
