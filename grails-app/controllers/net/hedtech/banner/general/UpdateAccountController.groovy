/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import grails.converters.JSON
import net.hedtech.banner.exceptions.ApplicationException
import org.codehaus.groovy.grails.web.json.JSONObject

class UpdateAccountController {

    def directDepositAccountService
    def bankRoutingInfoService
    def directDepositAccountCompositeService
    def directDepositConfigurationService

    def createAccount() {
        def map = request?.JSON ?: params
        map.pidm = ControllerUtility.getPrincipalPidm()

        // Unmask account info, as needed, for create (needed, for example, if an accounts payable account is
        // being created based on a payroll account).
        unmaskAccountInfoFromSessionCache(map)

        // default values for a new Direct Deposit account
        map.id = null
        map.documentType = 'D'
        map.intlAchTransactionIndicator = 'N'

        def configStatus = directDepositConfigurationService.getParam(directDepositConfigurationService.SHOW_USER_PRENOTE_STATUS, 'Y')
        map.status = (configStatus == 'Y') ? 'P' : 'A'

        log.debug("trying to create acct: "+ map.bankAccountNum)

        try {
            JSON.use( 'deep' ) {
                
                // response object to keep UI happy
                def r = [:]

                removeKeyValuePairsNotWantedForUpdate(map)
                fixJSONObjectForCast(map)

                directDepositAccountService.validateAccountAmounts(map)

                //newPosition is set so we need to do some reodering as we insert
                if(map.newPosition) {
                    def reprioritizedAccounts = directDepositAccountCompositeService.rePrioritizeAccounts(map, map.newPosition)
                    def marshalledAccounts = directDepositAccountService.marshallAccountsToMinimalStateForUi(reprioritizedAccounts)

                    r.list = DirectDepositUtility.maskAccounts(marshalledAccounts)
                    render r as JSON
                } else {
                    def newAccount = directDepositAccountCompositeService.addorUpdateAccount(map)
                    def marshalledAccount = directDepositAccountService.marshallAccountsToMinimalStateForUi(newAccount)

                    render DirectDepositUtility.maskAccounts([marshalledAccount]).first() as JSON
                }
            }
        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }

    def updateAccount() {
        def map = request?.JSON ?: params

        try {
            // Do some cleanup to prepare for update
            removeKeyValuePairsNotWantedForUpdate(map)
            fixJSONObjectForCast(map)

            // Account and routing numbers will be masked, and are not updatable anyway,
            // so exclude them from the update.
            map.remove('bankAccountNum')
            map.remove('bankRoutingInfo')

            directDepositAccountService.validateAccountAmounts(map)

            def updatedAccount = directDepositAccountService.update(map)
            def marshalledAccount = directDepositAccountService.marshallAccountsToMinimalStateForUi(updatedAccount)

            render DirectDepositUtility.maskAccounts([marshalledAccount]).first() as JSON

        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }
    
    def reorderAccounts() {
        def map = request?.JSON ?: params

        map.pidm = ControllerUtility.getPrincipalPidm()

        try {
            // Do some cleanup to prepare for update
            removeKeyValuePairsNotWantedForUpdate(map)
            fixJSONObjectForCast(map)
            unmaskAccountInfoFromSessionCache(map)

            def prioritizedAccounts = directDepositAccountCompositeService.rePrioritizeAccounts(map, map.newPosition)
            def marshalledAccounts = directDepositAccountService.marshallAccountsToMinimalStateForUi(prioritizedAccounts)
            def maskedAccounts = DirectDepositUtility.maskAccounts(marshalledAccounts)

            render maskedAccounts as JSON

        } catch (ApplicationException e) {
            def arrayResult = [];
            arrayResult[0] = ControllerUtility.returnFailureMessage(e)
            
            render arrayResult as JSON
        }
    }

    def getCurrency() {
        try {
            def symbol = [:]

            symbol.currencySymbol = directDepositAccountCompositeService.getCurrencySymbol()

            render symbol as JSON

        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }
    
    def reorderAllAccounts() {
        def map = request?.JSON ?: params

        map.each {
            unmaskAccountInfoFromSessionCache(it)
            it.pidm = ControllerUtility.getPrincipalPidm()
        }

        try {
            def reorderedResults = directDepositAccountCompositeService.reorderAccounts(map)
            def maskedResults = []

            // First extract the first element, which is a list of "delete operation" results
            maskedResults.add(reorderedResults[0])

            // Then mask and add in the accounts (elements 1 through n in the list)
            def marshalledAccounts = directDepositAccountService.marshallAccountsToMinimalStateForUi(reorderedResults.drop(1))
            def maskedAccounts = DirectDepositUtility.maskAccounts(marshalledAccounts)
            maskedResults.addAll(directDepositAccountService.marshallAccountsToMinimalStateForUi(maskedAccounts))

            render maskedResults as JSON

        } catch (ApplicationException e) {
            def arrayResult = [];
            arrayResult[0] = ControllerUtility.returnFailureMessage(e)
            
            render arrayResult as JSON
        }
    }

    def deleteAccounts() {
        def map = request?.JSON ?: params

        map.each {
            unmaskAccountInfoFromSessionCache(it)
            it.pidm = ControllerUtility.getPrincipalPidm()
        }

        try {
            def model = [:]
            def accounts = directDepositAccountService.setupAccountsForDelete(map)
            def result = directDepositAccountService.delete(accounts.toBeDeleted)

            accounts.messages.each {
                it.acct = DirectDepositUtility.maskBankInfo(it.acct)
            }
            model.messages = accounts.messages
            model.messages.add(result)
            
            render model.messages as JSON

        } catch (ApplicationException e) {
            def arrayResult = [];
            arrayResult[0] = ControllerUtility.returnFailureMessage(e)
            
            render arrayResult as JSON
        }
    }

    def getBankInfo() {
        def map = request?.JSON ?: params
        
        log.debug("trying to fetch bank: "+ map.bankRoutingNum)
        
        try {
            render bankRoutingInfoService.validateRoutingNumber(map.bankRoutingNum)[0] as JSON

        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }
    
    def validateAccountNum() {
        def model = [:]
        def map = request?.JSON ?: params
        
        log.debug("validating acct num: "+ map.bankAccountNum)
        
        try {
            model.failure = false
            directDepositAccountService.validateAccountNumFormat(map.bankAccountNum)
            render model as JSON

        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }

    def unmaskAccountInfoFromSessionCache(acct) {
        // Unmask account info. Values needed for unmasking are stored in the session.
        def cachedAcctInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(acct.id)

        if (cachedAcctInfo) {
            acct.bankAccountNum = cachedAcctInfo.acctNum
            acct.bankRoutingInfo = [
                id:             cachedAcctInfo.routing.id,
                bankRoutingNum: cachedAcctInfo.routing.bankRoutingNum,
                bankName:       cachedAcctInfo.routing.bankName
            ]

            DirectDepositUtility.removeDirectDepositAccountInfoFromSessionCache(acct.id)
        }
    }

    /**
     * Certain key/value pairs, most notably those including dates, can get flagged as dirty, and an object that
     * is actually clean from a functional point of view is unnecessarily updated.  Removing such fields prevents
     * this from occurring.
     * @param json JSONObject to fix
     */
    private def removeKeyValuePairsNotWantedForUpdate(JSONObject json) {
        json.remove("lastModified")

        def bankRoutingInfo = json.get("bankRoutingInfo")

        if (bankRoutingInfo) {
            bankRoutingInfo.remove("lastModified")
        }
    }

    /**
     * Prepare the values in a JSONObject map to be properly cast to values that can be set on the
     * domain object (i.e. class DirectDepositAccount).
     *
     * To be specific, here's the problem:  ServiceBase.update uses InvokerHelper to set properties from
     * the JSON object onto the domain object.  In doing this, InvokerHelper.setProperties eventually
     * results in the DefaultTypeTransformation.castToType (the class is a Java one not Groovy).  This
     * method does not know how to handle JSONObject.NULL as a true Java null nor a date string as a
     * Date object, which results in exceptions being thrown for these.  In this method we fix the null
     * issue.  We have no date objects that we want to explicitly update, so as of this writing we don't
     * try to fix those.
     * @param json JSONObject to fix
     */
    private def fixJSONObjectForCast(JSONObject json) {
        json.each {entry ->
            // Make JSONObject.NULL a real Java null
            if (entry.value == JSONObject.NULL) {
                entry.value = null

//            If we ever want to fix dates, this is one possible solution
//            } else if (entry.key == "lastModified") {
//                // Make this date string a real Date object
//                entry.value = DateUtility.parseDateString(entry.value, "yyyy-MM-dd'T'HH:mm:ss'Z'")
            }
        }
    }

}
