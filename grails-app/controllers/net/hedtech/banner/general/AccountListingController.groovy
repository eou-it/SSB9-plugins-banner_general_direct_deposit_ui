package net.hedtech.banner.general

import grails.converters.JSON
import net.hedtech.banner.general.overall.DirectDepositAccountService
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.general.person.PersonUtility

class AccountListingController  {

    def directDepositAccountService
    def directDepositAccountCompositeService
    def currencyFormatService



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
            render model as JSON
        }
    }

    def getUserPayrollAllocations() {
        def person = findPerson()

        if (person) {
            try {
                JSON.use('deep') {
                    render directDepositAccountCompositeService.getUserHrAllocations(person.pidm) as JSON
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

        render model as JSON
    }

}
