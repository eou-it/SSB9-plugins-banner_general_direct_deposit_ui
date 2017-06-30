/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import net.hedtech.banner.security.XssSanitizer
import org.springframework.web.context.request.RequestContextHolder

class DirectDepositUtility {

    private static final DD_ACCOUNT_CACHE = "DD_ACCOUNT_CACHE"

    /**
     * Recursively sanitize all values in map to eliminate cross-site scripting (XSS) vulnerabilities.
     * @param map
     */
    def static sanitizeMap(Map map) {
        map.each { element ->
            def v = element.value

            if (v in Map) {
                sanitizeMap(v)
            } else if (v in String) {
                element.value = XssSanitizer.sanitize(v)
            }
        }
    }

    static setDirectDepositAccountInfoInSessionCache(acctId, acctInfo) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)

        if (!cache) {
            cache = [:]
            session.setAttribute(DD_ACCOUNT_CACHE, cache)
        }

        cache[""+acctId] = acctInfo // For consistency in type, convert ID to string for use as key
    }

    static getDirectDepositAccountInfoFromSessionCache(acctId) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)
        def acctIdStr = ""+acctId // Type of key in cache is String

        cache?."$acctIdStr"
    }

    static removeDirectDepositAccountInfoFromSessionCache(acctId) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)

        cache?.remove(""+acctId)

        cache
    }

    /**
     * Mask all but the last four characters of val with 'x'.
     * Examples:
     *   "12345678" becomes "xxxx5678"
     *   "5678" remains "5678"
     *   "78" remains "78"
     *
     * @param val
     * @return Masked value
     */
    static String maskBankInfo(val) {
        return val.replaceAll("\\w(?=\\w{4})", "x")
    }

    /**
     * Mask account and routing numbers in place (i.e. the object passed in is mutated),
     * returning the object passed in.  Handles collections, arrays, and single objects.
     * @param accts
     * @return Masked accts object
     */
    static maskAccounts(accts) {
        if (!accts) return accts

        def accounts = isCollectionOrArray(accts) ? accts : [accts]
        def processedRoutingInfo = [:]

        accounts.each {
            def routingInfo = processedRoutingInfo[it.bankRoutingInfo.id]

            // Save and mask routing info only once for each bankRoutingInfo object (i.e. if the
            // same routing info is included in more than one account, only process it once.
            if (!routingInfo) {
                routingInfo = processedRoutingInfo[it.bankRoutingInfo.id] = [
                        beforeMasking: [
                            id:             it.bankRoutingInfo.id,
                            bankRoutingNum: it.bankRoutingInfo.bankRoutingNum,
                            bankName:       it.bankRoutingInfo.bankName
                        ],
                        masked: maskBankInfo(it.bankRoutingInfo.bankRoutingNum)
                ]
            }

            def acctInfo = [acctNum: it.bankAccountNum, routing: routingInfo.beforeMasking]
            setDirectDepositAccountInfoInSessionCache(it.id, acctInfo)
            it.bankAccountNum = maskBankInfo(it.bankAccountNum)
            it.bankRoutingInfo.bankRoutingNum = routingInfo.masked
        }

        return accts
    }

    static boolean isCollectionOrArray(object) {
        [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
    }

}
