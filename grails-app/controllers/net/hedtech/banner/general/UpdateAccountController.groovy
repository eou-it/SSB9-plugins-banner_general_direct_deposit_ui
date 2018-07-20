/*******************************************************************************
 Copyright 2015-2018 Ellucian Company L.P. and its affiliates.
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

    def beforeInterceptor = [action:this.&readOnlyCheck, except:'getCurrency']

    private readOnlyCheck() {
        // Disallow updates if in read-only mode
        if (!directDepositAccountCompositeService.areAccountsUpdatable()) {
            log.error('Invalid attempt to update account when in READ-ONLY mode.')
            return false
        }
    }

    def createAccount() {
        def map = request?.JSON ?: params
        map.pidm = ControllerUtility.getPrincipalPidm()

        // Unmask account info, as needed, for create (needed, for example, if an accounts payable account is
        // being created based on a payroll account).
        def unmaskedAcctIds = unmaskAccountInfoFromSessionCache([map])

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

                    // Now that the operation has been completed successfully, clear old masking data in preparation for new
                    clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

                    r.list = DirectDepositUtility.maskAccounts(marshalledAccounts)
                    render r as JSON
                } else {
                    def newAccount = directDepositAccountCompositeService.addorUpdateAccount(map)
                    def marshalledAccount = directDepositAccountService.marshallAccountsToMinimalStateForUi(newAccount)

                    // Now that the operation has been completed successfully, clear old masking data in preparation for new
                    clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

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
            def unmaskedAcctIds = unmaskAccountInfoFromSessionCache([map])

            def prioritizedAccounts = directDepositAccountCompositeService.rePrioritizeAccounts(map, map.newPosition)
            def marshalledAccounts = directDepositAccountService.marshallAccountsToMinimalStateForUi(prioritizedAccounts)

            // Now that the operation has been completed successfully, clear old masking data in preparation for new
            clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

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
        def unmaskedAcctIds = unmaskAccountInfoFromSessionCache(map)
        def pidm = ControllerUtility.getPrincipalPidm()

        map.each { it.pidm = pidm }

        try {
            def reorderedResults = directDepositAccountCompositeService.reorderAccounts(map)
            def maskedResults = []

            // First extract the first element, which is a list of "delete operation" results
            maskedResults.add(reorderedResults[0])

            // Then mask and add in the accounts (elements 1 through n in the list)
            def marshalledAccounts = directDepositAccountService.marshallAccountsToMinimalStateForUi(reorderedResults.drop(1))

            // Now that the operation has been completed successfully, clear old masking data in preparation for new
            clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

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
        def unmaskedAcctIds = unmaskAccountInfoFromSessionCache(map)
        def pidm = ControllerUtility.getPrincipalPidm()

        map.each { it.pidm = pidm }

        try {
            def model = [:]
            def accounts = directDepositAccountService.setupAccountsForDelete(map)
            def result = directDepositAccountService.delete(accounts.toBeDeleted)

            // Now that the operation has been completed successfully, clear old masking data in preparation for new
            clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

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

    def validateAccountsAreUnique() {
        def map = request?.JSON ?: params
        def model = [failure: false]
        def accountSummaryList = []

        unmaskAccountInfoFromSessionCache(map)

        map.find { acct ->
            def summaryStr = "${acct.bankRoutingInfo.bankRoutingNum}|${acct.bankAccountNum}|${acct.accountType}|${acct.apIndicator}|${acct.hrIndicator}"

            if (summaryStr in accountSummaryList) {
                model = [failure: true,
                         message: message(code:'net.hedtech.banner.general.overall.DirectDepositAccount.recordAlreadyExists', default: 'Record already exists for this Bank Account.')
                ]

                return true
            } else {
                accountSummaryList << summaryStr
            }

            return false
        }

        render model as JSON
    }

    /**
     * Unmask account information in list of accounts.
     * Preserve unmasked account IDs in a list to be returned, so that they can be cleared out from the session cache at
     * an appropriate time.  As an example, we may need to wait until all operations using the cache have been
     * successfully completed before clearing masked accounts from the cache.  This is because if such an operation
     * fails *and* the cache has already been cleared, then the frontend can be left holding masked account info which
     * now has no map of masked data on the backend to unmask it.
     * @param accts List of accounts
     * @return List of unmasked account IDs
     */
    private unmaskAccountInfoFromSessionCache(accts) {
        def cachedAcctInfo
        def unmaskedAcctIds = []

        accts.each { acct ->
            // Unmask account info. Values needed for unmasking are stored in the session.
            cachedAcctInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(acct.id)

            if (cachedAcctInfo) {
                acct.bankAccountNum = cachedAcctInfo.acctNum
                acct.bankRoutingInfo = [
                        id            : cachedAcctInfo.routing.id,
                        bankRoutingNum: cachedAcctInfo.routing.bankRoutingNum,
                        bankName      : cachedAcctInfo.routing.bankName
                ]

                unmaskedAcctIds << acct.id
            }
        }

        unmaskedAcctIds
    }

    private clearAccountMaskingInfoFromSessionCache(acctIds) {
        acctIds.each {
            DirectDepositUtility.removeDirectDepositAccountInfoFromSessionCache(it)
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
