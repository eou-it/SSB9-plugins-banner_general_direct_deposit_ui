/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

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
                model = directDepositAccountService.fetchApAccountsByPidmAsListOfMaps(person.pidm)
            } catch (ApplicationException e) {
                render ControllerUtility.returnFailureMessage(e) as JSON
            }
        }

        JSON.use("deep") {
            render DirectDepositUtility.maskAccounts(model) as JSON
        }
    }

    def getUserPayrollAllocations() {
        def person = findPerson()

        if (person) {
            try {
                def hrAllocs = directDepositAccountCompositeService.getUserHrAllocationsAsListOfMaps(person.pidm)
                DirectDepositUtility.maskAccounts(hrAllocs.allocations)

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
            it.bankAccountNumber = DirectDepositUtility.maskBankInfo(it.bankAccountNumber)
            it.bankRoutingNumber = DirectDepositUtility.maskBankInfo(it.bankRoutingNumber)
        }

        render model as JSON
    }

}
