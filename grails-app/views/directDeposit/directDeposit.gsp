%{--*******************************************************************************
Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
*******************************************************************************--}%
<!DOCTYPE html>
<!--[if IE 9 ]>    <html xmlns:ng="http://angularjs.org" ng-app="directDepositApp" id="ng-app" class="ie9"> <![endif]-->
<html xmlns:ng="http://angularjs.org" ng-app="directDepositApp" id="ng-app">
<head>
    <script type="text/javascript">
        var superUser=${session['SUPER_USER_INDICATOR'] ?: 'undefined'};
        var proxyUser=${session['PROXY_USER_INDICATOR'] ?: 'undefined'};
        var proxyUserName = '${session.PROXY_USER_NAME ?: 'undefined'}';
        var adminFlag=${session['adminFlag'] ?: 'undefined'};
        var employeeFlag=${session['EmployeeFlag'] ?: 'undefined'};
        var proxyFlag=${session['proxyFlag'] ?: 'undefined'};
        var originatorFlag =${session['originatorFlag'] ?: 'undefined'};
        var approverFlag =${session['approverFlag'] ?: 'undefined'};
        var url = '${url}'

    </script>
    <g:applyLayout name="bannerWebPage">
        <meta name="locale" content="${request.locale.toLanguageTag()}" >
        <meta name="menuEndPoint" content="${request.contextPath}/ssb/menu"/>
        <meta name="menuBaseURL" content="${request.contextPath}/ssb"/>
        <meta name="menuBase" content="${request.contextPath}"/>
        <meta charset="${message(code: 'default.character.encoding')}">
        <g:set var="applicationName" value= "${grails.util.Metadata.current.getApplicationName()}"/>
        <meta name="applicationName" content="${applicationName}">

        <g:if test="${message(code: 'default.language.direction')  == 'rtl'}">
            <r:require modules="directDepositAppRTL"/>
        </g:if>
        <g:else>
            <r:require modules="directDepositAppLTR"/>
        </g:else>

    </g:applyLayout>

    <meta name="viewport" content="width=device-width, height=device-height,  initial-scale=1.0, user-scalable=no, user-scalable=0"/>
    <meta http-equiv="X-UA-Compatible" content="IE=10" />

    <script type="text/javascript">
        <g:i18n_setup/>
    </script>
    <script type="text/javascript">
        // Track calling page for breadcrumbs
        (function () {
            // URL to exclude from updating genAppCallingPage, because it's actually just the authentication
            // page and not a "calling page."
            var referrerUrl = document.referrer,
                    excludedRegex = /\/${applicationName}\/login\/auth?/,
                    isExcluded;

            if (referrerUrl) {
                isExcluded = excludedRegex.test(referrerUrl);

                if (!isExcluded) {
                    // Track this page
                    sessionStorage.setItem('genAppCallingPage', referrerUrl);
                }
            }
        })();
    </script>
</head>

<body ng-class="{'direct-deposit': true, employee: isEmployee, student: isStudent, desktop: isDesktopView, 'no-ap': !apAccountExists}">

<div class="body-overlay"></div>
<div id="content" class="container-fluid" aria-relevant="additions" role="main">
    <div ui-view></div>
</div>
</body>
</html>
