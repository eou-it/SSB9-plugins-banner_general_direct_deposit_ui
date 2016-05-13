/*******************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
*******************************************************************************/


import net.hedtech.banner.configuration.ApplicationConfigurationUtils as ConfigFinder
import grails.plugin.springsecurity.SecurityConfigType

// ******************************************************************************
//
//                       +++ EXTERNALIZED CONFIGURATION +++
//
// ******************************************************************************
//
// Config locations should be added to the map used below. They will be loaded based upon this search order:
// 1. Load the configuration file if its location was specified on the command line using -DmyEnvName=myConfigLocation
// 2. Load the configuration file if it exists within the user's .grails directory (i.e., convenient for developers)
// 3. Load the configuration file if its location was specified as a system environment variable
//
// Map [ environment variable or -D command line argument name : file path ]

grails.config.locations = [] // leave this initialized to an empty list, and add your locations in the map below.

def locationAdder = ConfigFinder.&addLocation.curry(grails.config.locations)

[ BANNER_APP_CONFIG:            "banner_configuration.groovy",
  BANNER_GENERAL_SSB_CONFIG:    "${appName}_configuration.groovy",
  WEB_APP_EXTENSIBILITY_CONFIG: "WebAppExtensibilityConfig.class"
].each { envName, defaultFileName -> locationAdder( envName, defaultFileName ) }

grails.config.locations.each {
    println "configuration: " + it
}

// ******************************************************************************
//
//                       +++ BUILD NUMBER SEQUENCE UUID +++
//
// ******************************************************************************
//
// A UUID corresponding to this project, which is used by the build number generator.
// Since the build number generator web service provides build number sequences to
// multiple projects, and each project uses a unique UUID to identify which number
// sequence it is using.
//
// This number should NOT be changed.
// FYI: When a new UUID is needed (e.g., for a new project), use this URI:
//      http://maldevl2.sungardhe.com:8080/BuildNumberServer/newUUID
//
// DO NOT EDIT THIS UUID UNLESS YOU ARE AUTHORIZED TO DO SO AND KNOW WHAT YOU ARE DOING
//
build.number.uuid = "5b2d0947-541b-4d00-b709-ba31a464feb0" // specific UUID for FGE solution
build.number.base.url="http://m039200.sungardhe.com:8080/BuildNumberServer/buildNumber?method=getNextBuildNumber&uuid="

grails.project.groupId = "net.hedtech" // used when deploying to a maven repo
grails.databinding.useSpringBinder=true
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
        html: ['text/html', 'application/xhtml+xml'],
        xml: ['text/xml', 'application/xml', 'application/vnd.sungardhe.student.v0.01+xml'],
        text: 'text/plain',
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        css: 'text/css',
        csv: 'text/csv',
        all: '*/*',
        json: ['application/json', 'text/json'],
        form: 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data',
        jpg: 'image/jpeg',
        png: 'image/png',
        gif: 'image/gif',
        bmp: 'image/bmp',
]

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64  **** note: Setting this to html will ensure html is escaped, to prevent XSS attack ****
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
grails.plugin.springsecurity.logout.afterLogoutUrl = "/"
grails.converters.domain.include.version = true
//grails.converters.json.date = "default"

grails.converters.json.pretty.print = true
grails.converters.json.default.deep = true

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = false

// enable GSP preprocessing: replace head -> g:captureHead, title -> g:captureTitle, meta -> g:captureMeta, body -> g:captureBody
grails.views.gsp.sitemesh.preprocess = true

grails.resources.mappers.yuicssminify.includes = ['**/*.css']
grails.resources.mappers.yuijsminify.includes = ['**/*.js']
grails.resources.mappers.yuicssminify.excludes = ['**/*.min.css']
grails.resources.mappers.yuijsminify.excludes = ['**/*.min.js']

// set per-environment serverURL stem for creating absolute links
environments {
    development {
        grails.resources.debug = true;
    }
}

environments {
    test {
        ssbEnabled = true
        ssbOracleUsersProxied = true
        grails.plugins.springsecurity.interceptUrlMap = [
                '/': ['IS_AUTHENTICATED_ANONYMOUSLY'] ]
    }
    development {
        ssbEnabled = true
        ssbOracleUsersProxied = true
    }
    production {

    }

}

// ******************************************************************************
//
//                       +++ DATA ORIGIN CONFIGURATION +++
//
// ******************************************************************************
// This field is a Banner standard, along with 'lastModifiedBy' and lastModified.
// These properties are populated automatically before an entity is inserted or updated
// within the database. The lastModifiedBy uses the username of the logged in user,
// the lastModified uses the current timestamp, and the dataOrigin uses the value
// specified here:
dataOrigin = "Banner"

// ******************************************************************************
//
//                       +++ FORM-CONTROLLER MAP +++
//
// ******************************************************************************
// This map relates controllers to the Banner forms that it replaces.  This map
// supports 1:1 and 1:M (where a controller supports the functionality of more than
// one Banner form.  This map is critical, as it is used by the security framework to
// set appropriate Banner security role(s) on a database connection. For example, if a
// logged in user navigates to the 'medicalInformation' controller, when a database
// connection is attained and the user has the necessary role, the role is enabled
// for that user and Banner object.
formControllerMap = [
        'selfservicemenu' : ['SELFSERVICE-EMPLOYEE'],
        'survey': ['SELFSERVICE'],
        'useragreement': ['SELFSERVICE'],
        'securityqa': ['SELFSERVICE'],
        'general': ['SELFSERVICE'],
        'updateaccount': ['SELFSERVICE'],
        'accountlisting': ['SELFSERVICE'],
        'directdepositconfiguration': ['SELFSERVICE']
]


grails.plugin.springsecurity.useRequestMapDomainClass = false
//grails.plugin.springsecurity.rejectIfNoRule = true

grails.plugin.springsecurity.securityConfigType = SecurityConfigType.InterceptUrlMap


// ******************************************************************************
//
//                       +++ INTERCEPT-URL MAP +++
//
// ******************************************************************************

grails.plugin.springsecurity.interceptUrlMap = [
        '/': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/resetPassword/**': [ 'IS_AUTHENTICATED_ANONYMOUSLY' ] ,
        '/login/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/index**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/logout/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/ssb/logout/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],                
        '/ssb/menu':['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/js/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/css/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/images/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/fonts/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/plugins/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/errors/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/help/**':  ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/i18n/**':  ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/ssb/selfServiceMenu/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/ssb/menu**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/directDepositApp/**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        '/ssb/keepAlive/data**': ['IS_AUTHENTICATED_ANONYMOUSLY'],
        // ALL URIs specified with the BannerAccessDecisionVoter.ROLE_DETERMINED_DYNAMICALLY
        // 'role' (it's not a real role) will result in authorization being determined based
        // upon a user's role assignments to the corresponding form (see 'formControllerMap' above).
        // Note: This 'dynamic form-based authorization' is performed by the BannerAccessDecisionVoter
        // registered as the 'roleVoter' within Spring Security.
        //
        // Only '/name_used_in_formControllerMap/' and '/api/name_used_in_formControllerMap/'
        // URL formats are supported.  That is, the name_used_in_formControllerMap must be first, or
        // immediately after 'api' -- but it cannot be otherwise nested. URIs may be protected
        // by explicitly specifying true roles instead -- as long as ROLE_DETERMINED_DYNAMICALLY
        // is NOT specified.
        //
        // '/**': [ 'ROLE_DETERMINED_DYNAMICALLY' ]
        //'/**': [ 'ROLE_SELFSERVICE-FACULTY_BAN_DEFAULT_M' ]
        '/ssb/securityQA/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/survey/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/userAgreement/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/general/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/UpdateAccount/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/accountListing/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M'],
        '/ssb/DirectDepositConfiguration/**': ['ROLE_SELFSERVICE-EMPLOYEE_BAN_DEFAULT_M','ROLE_SELFSERVICE-STUDENT_BAN_DEFAULT_M','ROLE_SELFSERVICE_BAN_DEFAULT_M']
]

// CodeNarc rulesets
codenarc.ruleSetFiles="rulesets/banner.groovy"
codenarc.reportName="target/CodeNarcReport.html"
codenarc.propertiesFile="grails-app/conf/codenarc.properties"
codenarc.extraIncludeDirs=["grails-app/composers"]

grails.validateable.packages=['net.hedtech.banner.student.registration']

// placeholder for real configuration
// base.dir is probably not defined for .war file deployments
//banner.picturesPath=System.getProperty('base.dir') + '/test/images'

markdown = [
        removeHtml: true
]

grails.resources.adhoc.excludes = ['/**/*-custom.css']

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

/* remove this line
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */

// ******************************************************************************
//                       RESTful API Endpoint Configuration
// ******************************************************************************
restfulApiConfig = {
    // Resources for web_app_extensibility plugin
    resource 'extensions' config {
        serviceName = 'webAppExtensibilityExtensionService'   // In some cases the service name has to be prepended with the plugin name
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app: grailsApplication)
                    priority = 100
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.json.DefaultJSONExtractor()
        }
    }
    resource 'resources' config {
        serviceName = 'webAppExtensibilityResourceService'   // In some cases the service name has to be prepended with the plugin name
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
                    priority = 100
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.json.DefaultJSONExtractor()
        }
    }
    // End Resources for web_app_extensibility plugin
}

grails.plugin.springsecurity.cas.active=false
grails.plugin.springsecurity.saml.active=false
