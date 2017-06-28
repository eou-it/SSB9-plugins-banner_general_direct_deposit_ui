package net.hedtech.banner.general

import grails.converters.JSON
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.general.person.PersonUtility

class AccountListingController  {

    def directDepositAccountService
    def directDepositAccountCompositeService



    private def findPerson() {
        return PersonUtility.getPerson(ControllerUtility.getPrincipalPidm())
    }

    def getApAccountsForCurrentUser() {
        def person = findPerson()
        def model = [:]

        if (person) {
            try {
                model = directDepositAccountService.fetchApAccountsByPidm(person.pidm)
            } catch (ApplicationException e) {
                render ControllerUtility.returnFailureMessage(e) as JSON
            }
        }

        JSON.use("deep") {
            render maskAccounts(model) as JSON
        }
    }

    def getUserPayrollAllocations() {
        def person = findPerson()

        if (person) {
            try {
                def hrAllocs = directDepositAccountCompositeService.getUserHrAllocationsAsListOfMaps(person.pidm)
                maskAccounts(hrAllocs.allocations)

                JSON.use('deep') {
                    render hrAllocs as JSON
                }
            } catch (ApplicationException e) {
                render ControllerUtility.returnFailureMessage(e) as JSON
            }
        }
    }

    def getLastPayDateInfo() {
        def model = [:]
        def pidm = ControllerUtility.getPrincipalPidm()

        model = directDepositAccountCompositeService.getLastPayDistribution(pidm)

        model?.docAccts.each {
            it.bankAccountNumber = maskBankInfo(it.bankAccountNumber)
            it.bankRoutingNumber = maskBankInfo(it.bankRoutingNumber)
        }

        render model as JSON
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

    static maskAccounts(accts) {
        def routingInfoBeforeMasking = [:]

        accts.each {
            def rawRoutingInfo = routingInfoBeforeMasking[it.bankRoutingInfo.id]

            // Save and mask routing info only once for each bankRoutingInfo object.
            if (!rawRoutingInfo) {
                rawRoutingInfo = routingInfoBeforeMasking[it.bankRoutingInfo.id] = [
                    bankRoutingNum: it.bankRoutingInfo.bankRoutingNum,
                    bankName: it.bankRoutingInfo.bankName
                ]

                it.bankRoutingInfo.bankRoutingNum = maskBankInfo(it.bankRoutingInfo.bankRoutingNum)
            }

            def acctInfo = [acctNum: it.bankAccountNum, routing: rawRoutingInfo]
            DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(it.id, acctInfo)
            it.bankAccountNum = maskBankInfo(it.bankAccountNum)
        }

        return accts
    }

}
