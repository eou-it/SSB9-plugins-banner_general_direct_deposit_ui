<%@ page import="org.springframework.context.i18n.LocaleContextHolder" %>
%{--*******************************************************************************
Copyright 2015-2020 Ellucian Company L.P. and its affiliates.
*******************************************************************************--}%
<!DOCTYPE html>
<!--[if IE 9 ]>    <html xmlns:ng="http://angularjs.org" ng-app="directDepositApp" id="ng-app" class="ie9"> <![endif]-->
<html xmlns:ng="http://angularjs.org"  id="ng-app">
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
        var url = '${url}';

    </script>
    <g:applyLayout name="bannerWebPage">
        <meta name="locale" content="${LocaleContextHolder.getLocale()}" >
        <meta name="menuEndPoint" content="${request.contextPath}/ssb/menu"/>
        <meta name="menuBaseURL" content="${request.contextPath}/ssb"/>
        <meta name="menuBase" content="${request.contextPath}"/>
        <meta charset="${message(code: 'default.character.encoding')}">
        <g:set var="applicationContextRoot" value= "${application.contextPath}"/>
        <meta name="applicationContextRoot" content="${applicationContextRoot}">

        <g:if test="${message(code: 'default.language.direction')  == 'rtl'}">
            <asset:stylesheet src="modules/dd-applicationRTL-mf.css"/>
            <asset:stylesheet src="modules/directDepositAppRTL-mf.css"/>
        </g:if>
        <g:else>
            <asset:stylesheet src="modules/dd-applicationLTR-mf.css"/>
            <asset:stylesheet src="modules/directDepositAppLTR-mf.css"/>
        </g:else>

        <asset:javascript src="modules/dd-application-mf.js"/>
        <g:theme/>
    </g:applyLayout>

    <meta name="viewport" content="width=device-width, height=device-height,  initial-scale=1.0, user-scalable=no, user-scalable=0"/>
    <meta http-equiv="X-UA-Compatible" content="IE=10" />

    <g:bannerMessages/>

    <script type="text/javascript">
        // Track calling page for breadcrumbs
        (function () {
            // URLs to exclude from updating genAppCallingPage, because they're actually either the authentication
            // page or App Nav, and are not "calling pages."
            var referrerUrl = document.referrer,
                excludedRegex = [
                    /\${applicationContextRoot}\/login\/auth?/,
                    /\/seamless/
                ],
                isExcluded;

            if (referrerUrl) {
                isExcluded = _.find(excludedRegex, function (regex) {
                    return regex.test(referrerUrl);
                });

                if (!isExcluded && referrerUrl.indexOf("/ssb/") !== -1) {
                    // Track this page
                    sessionStorage.setItem('genAppCallingPage', referrerUrl);
                }
            }
        })();
    </script>
</head>

<body ng-class="{'direct-deposit': true, employee: isEmployee, student: isStudent, desktop: isDesktopView, 'no-ap': !apAccountExists}">

<div class="body-overlay"></div>
<div id="content" ng-app="directDepositApp" class="container-fluid" aria-relevant="additions" role="main">
    <div ui-view></div>
</div>
</body>
</html>
