/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
var generalSsbAppControllers = angular.module('generalSsbAppControllers', []);
var generalSsbAppDirectives = angular.module('generalSsbAppDirectives', []);


var directDepositApp = angular.module('directDepositApp', ['ngResource','ui.router','generalSsbAppControllers',
    'generalSsbAppDirectives','ui.bootstrap','I18n','numericApp'])
    .run(
    ['$rootScope', '$state', '$stateParams', '$filter', 'breadcrumbService', 'directDepositService', 'notificationCenterService',
        function ($rootScope, $state, $stateParams, $filter, breadcrumbService, directDepositService, notificationCenterService) {
            $rootScope.$state = $state;
            $rootScope.$stateParams = $stateParams;
            $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState, fromParams) {
                $state.previous = fromState;
                $state.previousParams = fromParams;
                breadcrumbService.setBreadcrumbs(toState.data.breadcrumbs);
                breadcrumbService.refreshBreadcrumbs();
            });
            $rootScope.$on('$viewContentLoaded', function () {
            });
            $(document.body).removeAttr("role");
            $("html").attr("dir", $filter('i18n')('default.language.direction') === 'ltr' ? "ltr" : "rtl");
            $rootScope.notificationErrorType = "error";
            $rootScope.notificationSuccessType = "success";
            $rootScope.notificationWarningType = "warning";
            $rootScope.flashNotification = true;
            //IE fix
            if (!window.location.origin) {
                window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
            }

            directDepositService.getRoles().$promise.then(function (response) {
                $rootScope.isStudent = response.isStudent;
                $rootScope.isEmployee = response.isEmployee;
            });

            directDepositService.getCurrencySymbol().$promise.then(function (response) {
                $rootScope.currencySymbol = response.currencySymbol.trim();
            });

            $rootScope.isDesktopView = isDesktop();

            // Above, we use the isDesktop function implemented in the banner_ui_ss plugin, which thus far has
            // proven to be satisfactory.  Below we modify the implementation of isTablet from banner_ui_ss to
            // be consistent with the definition of "is tablet" elsewhere in this app.
            var isTablet = window.matchMedia("only screen and (min-width: 768px) and (max-width:1024px)");
            $rootScope.isTabletView = isTablet.matches;

            $rootScope.apAccountExists = false;
            
            $rootScope.displayReprioritizeRemainingWarning = function(){
                notificationCenterService.displayNotification('directDeposit.invalid.reprioritze.remaining','error');
            };

            $rootScope.playAudibleMessage = null;
        }
    ]
);

directDepositApp.constant('webAppResourcePathString', '../plugins/banner-direct-deposit-ui-0.1');

directDepositApp.config(['$stateProvider', '$urlRouterProvider', 'webAppResourcePathString',
    function($stateProvider, $urlRouterProvider, webAppResourcePathString){
        // For any unmatched url, send to /directDepositListing
        var url = url ? url : 'directDepositListing';

        $urlRouterProvider.otherwise(url) ;

        /*********************************************************
         * Defining all the different states of the directDepositApp
         * Landing pages
         *********************************************************/
        $stateProvider
            .state('directDepositListing', {
                url: "/directDepositListing",
                templateUrl: webAppResourcePathString + '/directDepositApp/ddListing/directDepositListing.html',
                controller: 'ddListingController',
                onEnter: function(ddListingService){
                    ddListingService.doReload();
                },
                data: {
                    breadcrumbs: []
                },
                params: {
                    onLoadNotifications: []
                }
            });
    }
]);

directDepositApp.config(['$locationProvider',
    function ($locationProvider) {
        $locationProvider.html5Mode(false);
    }
]);

directDepositApp.config(['$httpProvider',
    function ($httpProvider) {
        if (!$httpProvider.defaults.headers.get) {
            $httpProvider.defaults.headers.get = {};
        }
        $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';
        $httpProvider.defaults.cache = false;
        $httpProvider.defaults.headers.get['If-Modified-Since'] = '0';
        $httpProvider.interceptors.push(function ($q, $window,$rootScope) {
            $rootScope.ActiveAjaxConectionsWithouthNotifications = 0;
            var checker = function(parameters,status){
                //YOU CAN USE parameters.url TO IGNORE SOME URL
                if(status === "request"){
                    $rootScope.ActiveAjaxConectionsWithouthNotifications+=1;
                    $('.body-overlay').addClass('loading');
                    $("#content").attr('aria-busy', true);
                }
                if(status === "response"){
                    $rootScope.ActiveAjaxConectionsWithouthNotifications-=1;

                }
                if($rootScope.ActiveAjaxConectionsWithouthNotifications<=0){
                    $rootScope.ActiveAjaxConectionsWithouthNotifications=0;
                    $('.body-overlay').removeClass('loading');
                    $("#content").attr('aria-busy', false);
                }
            };
            return {
                'request': function(config) {
                    checker(config,"request");
                    return config;
                },
                'requestError': function(rejection) {
                    checker(rejection.config,"request");
                    return $q.reject(rejection);
                },
                'response': function(response) {
                    checker(response.config,"response");
                    return response;
                },
                'responseError': function(rejection) {
                    checker(rejection.config,"response");
                    if (rejection.status === 403) {
                        window.location.href = '/login/denied';
                    }
                    return $q.reject(rejection);
                }
            };
        });
    }
]);
