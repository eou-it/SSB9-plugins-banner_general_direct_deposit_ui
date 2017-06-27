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

    public static setDirectDepositAccountInfoInSessionCache(acctId, acctInfo) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)

        if (!cache) {
            cache = [:]
            session.setAttribute(DD_ACCOUNT_CACHE, cache)
        }

        cache[""+acctId] = acctInfo // For consistency in type, convert ID to string for use as key
    }

    public static getDirectDepositAccountInfoFromSessionCache(acctId) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)
        def acctIdStr = ""+acctId // Type of key in cache is String

        cache?."$acctIdStr"
    }

    public static removeDirectDepositAccountInfoFromSessionCache(acctId) {
        def session = RequestContextHolder.currentRequestAttributes().request.session
        def cache = session.getAttribute(DD_ACCOUNT_CACHE)

        cache?.remove(""+acctId)

        cache
    }

}
