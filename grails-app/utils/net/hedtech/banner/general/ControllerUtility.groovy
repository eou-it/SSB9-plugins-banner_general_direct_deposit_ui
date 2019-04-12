/*******************************************************************************
 Copyright 2015-2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/
package net.hedtech.banner.general

import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.general.system.InstitutionalDescription
import org.apache.log4j.Logger
import org.grails.plugins.web.taglib.ValidationTagLib
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder

class ControllerUtility {

    static def log = Logger.getLogger('net.hedtech.banner.general.ControllerUtility')

    def static getPrincipalPidm() {
        try {
            return SecurityContextHolder?.context?.authentication?.principal?.pidm
        } catch (MissingPropertyException it) {
            log.error("principal lacks a pidm - may be unauthenticated or session expired. Principal: ${SecurityContextHolder?.context?.authentication?.principal}")
            log.error(it)
            throw it
        }
    }


    public static getCurrencyCode() {
        def currencyCode
        def session = RequestContextHolder?.currentRequestAttributes()?.request?.session

        if (session?.getAttribute("baseCurrencyCode")) {
            currencyCode = session.getAttribute("baseCurrencyCode")
        } else {
            currencyCode = InstitutionalDescription.fetchByKey()?.baseCurrCode
            session.setAttribute("baseCurrencyCode", currencyCode)
        }

        return currencyCode
    }

    public static  returnFailureMessage(ApplicationException  e) {
        def model = [:]

        model.failure = true
        log.error(e)

        try {
            def extractError = e.returnMap({ mapToLocalize -> new ValidationTagLib().message(mapToLocalize) })
            model.message = extractError.message + (extractError.errors ? " " + extractError.errors : "")

            if(e.type == 'SQLException'){
                // don't expose the oracle error numbers in SQL exceptions
                model.message = model.message.replaceAll("(ORA)-[0-9]+: ","")
            }

            return model
        }
        catch (ApplicationException ex) {
            log.error(ex)
            model.message = e.message
            return model
        }
    }

}
