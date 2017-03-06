/*******************************************************************************
 Copyright 2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

directDepositApp.service( 'breadcrumbService', ['$filter',function ($filter) {
    var constantBreadCrumb = [],
        GENERAL_LANDING_PAGE = 1;

    this.reset = function() {
        constantBreadCrumb = [
            {
                label: 'banner.generalssb.landingpage.title',
                url: GENERAL_LANDING_PAGE
            },
            {
                label: 'general.breadcrumb.directDeposit',
                url: '/directDepositListing'
            }
        ];
    };

    this.setBreadcrumbs = function (bc) {
        this.reset();
        constantBreadCrumb.push.apply(constantBreadCrumb, bc);
    };

    this.refreshBreadcrumbs = function() {
        var baseurl = $('meta[name=menuBase]').attr("content"),
            breadCrumbInputData = {},
            updatedHeaderAttributes;

        _.each (constantBreadCrumb, function(item) {
            var label = ($filter('i18n')(item.label));

            if (item.url) {
                breadCrumbInputData[label] = (item.url === GENERAL_LANDING_PAGE) ?
                    document.location.origin + baseurl :
                    "/" + document.location.pathname.slice(Application.getApplicationPath().length + 1) + "#" + item.url;
            } else {
                breadCrumbInputData[label] = "";
            }
        });

        updatedHeaderAttributes = {
            "breadcrumb":breadCrumbInputData
        };

        BreadCrumbAndPageTitle.draw(updatedHeaderAttributes);
    };
}]);