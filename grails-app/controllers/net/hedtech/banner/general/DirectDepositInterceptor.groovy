/*******************************************************************************
 Copyright 2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import net.hedtech.banner.SanitizeUtility

class DirectDepositInterceptor {

    DirectDepositInterceptor() {
        match controller: 'updateAccount', action:'*', actionExclude:'getCurrency'
    }

    boolean before() {
        def requestParams = request?.JSON ?: params

        if (requestParams in List) {
            requestParams.each {item -> SanitizeUtility.sanitizeMap(item)}
        } else if (requestParams in Map) {
            SanitizeUtility.sanitizeMap(requestParams)
        } else {
            logger.error(new Exception('Unknown request parameter type. Expected Map or Array.'))
            return false
        }

    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
