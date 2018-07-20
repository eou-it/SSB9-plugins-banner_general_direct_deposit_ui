/*******************************************************************************
 Copyright 2015-2018 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import grails.converters.JSON
import groovy.sql.Sql
import net.hedtech.banner.exceptions.ApplicationException
import net.hedtech.banner.general.overall.DirectDepositAccount
import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class UpdateAccountControllerTests extends BaseIntegrationTestCase {

    /**
     * The setup method will run before all test case method executions start.
     */
    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        controller = new UpdateAccountController()
        super.setUp()
    }

    /**
     * The tear down method will run after all test case method execution.
     */
    @After
    public void tearDown() {
        super.tearDown()
        super.logout()
    }


    @Test
    void testReadOnlyCheckWithUpdatableOn() {
        loginSSB 'GDP000005', '111111'

        def isReadOnly = controller.readOnlyCheck()

        assertNull isReadOnly
    }

    @Test
    void testReadOnlyCheckWithUpdatableOff() {
        loginSSB 'GDP000005', '111111'

        def sql
        try {
            sql = new Sql(sessionFactory.getCurrentSession().connection())
            sql.executeUpdate("update PTRINST set PTRINST_DD_WEB_UPDATE_IND = \'N\'")
        } finally {
            sql?.close() // note that the test will close the connection, since it's our current session's connection
        }

        def isReadOnly = controller.readOnlyCheck()

        assertNotNull isReadOnly
        assertFalse isReadOnly
    }

    @Test
    void testCreateAccount() {
        // Implicitly tests DirectDepositAccountCompositeService.rePrioritizeAccounts
        loginSSB 'GDP000005', '111111'

        controller.request.contentType = "text/json"
        controller.request.json = '''{
            pidm:null,
            status:null,
            apIndicator:"I",
            hrIndicator:"A",
            bankAccountNum:"0822051515",
            amount:null,
            percent:10,
            accountType:"C",
            bankRoutingInfo:{
                bankName:"First Fidelity",
                bankRoutingNum:"123478902",
            },
            amountType:"amount",
            priority:2,
            newPosition:2
        }'''

        controller.createAccount()
        def dataForNullCheck = controller.response.contentAsString
        def dataList = JSON.parse( dataForNullCheck ).list.sort {it.priority}

        assertNotNull dataList
        assertEquals 4, dataList.size()
        assertEquals 'xxxxxx1515', dataList[1].bankAccountNum
    }

    @Test
    void testCreateAccountAtLastPriorityWithExistingRemainingAccountAndCheckLastModified() {
        // Implicitly tests DirectDepositAccountCompositeService.rePrioritizeAccounts
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()

        controller.request.contentType = "text/json"
        controller.request.json = '''{
            pidm:null,
            status:null,
            apIndicator:"I",
            hrIndicator:"A",
            bankAccountNum:"0822051515",
            amount:null,
            percent:10,
            accountType:"C",
            bankRoutingInfo:{
                bankName:"First Fidelity",
                bankRoutingNum:"123478902",
            },
            amountType:"amount",
            priority:4,
            newPosition:4
        }'''

        controller.createAccount()
        def dataForNullCheck = controller.response.contentAsString
        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,            updatedAccts[0].priority
        assertEquals '736900542',  updatedAccts[0].bankAccountNum
        assertEquals GRAILS,       updatedAccts[0].lastModifiedBy

        assertEquals 2,            updatedAccts[1].priority
        assertEquals '95003546',   updatedAccts[1].bankAccountNum
        assertEquals GRAILS,       updatedAccts[1].lastModifiedBy

        // Although designated for priority 4, this account is put at 3 due to existing "Remaining" account
        assertEquals 3,            updatedAccts[2].priority
        assertEquals '0822051515', updatedAccts[2].bankAccountNum
        assertEquals USER,         updatedAccts[2].lastModifiedBy

        assertEquals 4,            updatedAccts[3].priority
        assertEquals '67674852',   updatedAccts[3].bankAccountNum
        assertEquals USER,         updatedAccts[3].lastModifiedBy

        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) > 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[2].lastModified.getTime() - updatedAccts[3].lastModified.getTime()) < 2000)
    }

    @Test
    void testCreateAccountAtPriorityTwoAndCheckLastModified() {
        // Implicitly tests DirectDepositAccountCompositeService.rePrioritizeAccounts
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()

        controller.request.contentType = "text/json"
        controller.request.json = '''{
            pidm:null,
            status:null,
            apIndicator:"I",
            hrIndicator:"A",
            bankAccountNum:"0822051515",
            amount:null,
            percent:10,
            accountType:"C",
            bankRoutingInfo:{
                bankName:"First Fidelity",
                bankRoutingNum:"123478902",
            },
            amountType:"amount",
            priority:2,
            newPosition:2
        }'''

        controller.createAccount()
        def dataForNullCheck = controller.response.contentAsString
        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,            updatedAccts[0].priority
        assertEquals '736900542',  updatedAccts[0].bankAccountNum
        assertEquals GRAILS,     updatedAccts[0].lastModifiedBy

        assertEquals 2,            updatedAccts[1].priority
        assertEquals '0822051515', updatedAccts[1].bankAccountNum
        assertEquals USER,         updatedAccts[1].lastModifiedBy

        assertEquals 3,            updatedAccts[2].priority
        assertEquals '95003546',   updatedAccts[2].bankAccountNum
        assertEquals USER,         updatedAccts[2].lastModifiedBy

        assertEquals 4,            updatedAccts[3].priority
        assertEquals '67674852',   updatedAccts[3].bankAccountNum
        assertEquals USER,         updatedAccts[3].lastModifiedBy

        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) > 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[2].lastModified.getTime() - updatedAccts[3].lastModified.getTime()) < 2000)
    }

    @Test
    void testCreateAccountAp() {
        loginSSB 'GDP000005', '111111'
        
        controller.request.contentType = "text/json"
        controller.request.json = '''{
            pidm:null,
            status:null,
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"0822051515",
            amount:null,
            percent:100,
            accountType:"C",
            bankRoutingInfo:{
                bankName:"First Fidelity",
                bankRoutingNum:"123478902",
            },
            amountType:"remaining"
        }'''

        controller.createAccount()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        assertNotNull data
        assertEquals 'xxxxxx1515', data.bankAccountNum
    }

    @Test
    void testUpdateAccount() {
        loginSSB 'HOSH00018', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountService.getActiveApAccounts(pidm) //36732

        controller.request.contentType = "text/json"
        controller.request.json = """{
            id: ${existingAccts[1].id},
            version:${existingAccts[1].version},
            class:"net.hedtech.banner.general.overall.DirectDepositAccount",
            accountType:"S",
            addressSequenceNum:1,
            addressTypeCode:"PR",
            amount:null,
            apAchTransactionTypeCode:null,
            apIndicator:"A",
            bankAccountNum:"9876543",
            bankRoutingInfo:{
                class:"net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                bankName:"First National Bank",
                bankRoutingNum:"234798944",
                dataOrigin:null,
                lastModified:"1999-08-17T03:34:22Z",
                lastModifiedBy:"PAYROLL"
            },
            dataOrigin:"Banner",
            documentType:"D",
            hrIndicator:"I",
            iatAddessSequenceNum:null,
            iatAddressTypeCode:null,
            intlAchTransactionIndicator:"N",
            isoCode:null,
            lastModified:"2016-02-26T19:48:41Z",
            lastModifiedBy:"mye000001",
            percent:100,
            priority:2,
            status:"A"
        }""".toString()

        controller.updateAccount()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals "S", data.accountType
    }

    @Test
    void testDeleteAccount() {
        loginSSB 'HOSH00018', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountService.getActiveApAccounts(pidm)
        existingAccts.each {
            it.discard()
            it.bankRoutingInfo.discard()
        }
        DirectDepositUtility.maskAccounts(existingAccts)

        controller.request.contentType = "text/json"
        controller.request.json = """[{
            id: ${existingAccts[0].id},
            accountType:"C",
            addressSequenceNum:1,
            addressTypeCode:"PR",
            amount:null,
            apAchTransactionTypeCode:null,
            apIndicator:"A",
            bankAccountNum:"xxx6543",
            bankRoutingInfo:{
                id: ${existingAccts[0].bankRoutingInfo.id},
                version: ${existingAccts[0].bankRoutingInfo.version},
                bankName:"Chase Manhattan Bank",
                bankRoutingNum:"xxxx8944"
            },
            dataOrigin:"Banner",
            documentType:"D",
            hrIndicator:"I",
            iatAddessSequenceNum:null,
            iatAddressTypeCode:null,
            intlAchTransactionIndicator:"N",
            isoCode:null,
            lastModified:"2016-02-26T19:48:41Z",
            lastModifiedBy:"mye000001",
            percent:100,
            priority:3,
            status:"P",
            apDelete:true,
            deleteMe:true
        }]""".toString()

        controller.deleteAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals 1, data[0].size()
        assertEquals true, data[0][0]
    }

    @Test
    void testDeleteDualAccount() {
        loginSSB 'GDP000002', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountService.getActiveApAccounts(pidm)
        existingAccts.each {
            it.discard()
            it.bankRoutingInfo.discard()
        }
        DirectDepositUtility.maskAccounts(existingAccts)

        controller.request.contentType = "text/json"
        controller.request.json = """[{
            id: ${existingAccts[0].id},
            accountType:"C",
            amount:null,
            apIndicator:"A",
            bankAccountNum:"xxx6543",
            bankRoutingInfo:{
                id: ${existingAccts[0].bankRoutingInfo.id},
                version: ${existingAccts[0].bankRoutingInfo.version},
                bankName:"Chase Manhattan Bank",
                bankRoutingNum:"xxxx8944"
            },
            documentType:"D",
            hrIndicator:"I",
            intlAchTransactionIndicator:"N",
            isoCode:null,
            percent:100,
            priority:3,
            status:"A",
            apDelete:true,
            deleteMe:true
        }]""".toString()

        controller.deleteAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals 'PR', data[0].activeType
        assertEquals 'xxx6543', data[0].acct
        assertTrue data[1][0]
    }

    @Test
    void testReorderAccounts() {
        loginSSB 'GDP000005', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """{
            "id": ${existingAccts[0].id},
            "version": 0,
            "dataOrigin": "Banner",
            "status": "A",
            "documentType": "D",
            "priority": 1,
            "apIndicator": "I",
            "hrIndicator": "A",
            "lastModified": "2016-04-08T16:14:01Z",
            "lastModifiedBy": "mye000005",
            "bankAccountNum": "736900542",
            "bankRoutingInfo": {
                "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                "id": 2,
                "version": 0,
                "bankName": "Chase Manhattan Bank",
                "bankRoutingNum": "748972234",
                "dataOrigin": null,
                "lastModified": "1999-08-17T03:34:22Z",
                "lastModifiedBy": "PAYROLL"
            },
            "amount": 77,
            "percent": null,
            "accountType": "C",
            "addressTypeCode": null,
            "addressSequenceNum": null,
            "intlAchTransactionIndicator": "N",
            "isoCode": null,
            "apAchTransactionTypeCode": null,
            "iatAddressTypeCode": null,
            "iatAddessSequenceNum": null,
            "amountType": "amount",
            "calculatedAmount": "\$77.00",
            "allocation": "\$77.00",
            "newPosition": 2
        }""".toString()

        controller.reorderAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck ).sort {it.priority}

        assertNotNull data
        assertEquals 'xxxx3546', data[0].bankAccountNum
        assertEquals 'xxxxx0542', data[1].bankAccountNum
    }

    @Test
    void testReorderAccountsAndCheckLastModified() {
        // Implicitly tests DirectDepositAccountCompositeService.rePrioritizeAccounts
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // Changing from priority 1 to 2 (see newPosition)
        controller.request.contentType = "text/json"
        controller.request.json = """{
            "id": ${existingAccts[0].id},
            "version": 0,
            "dataOrigin": "Banner",
            "status": "A",
            "documentType": "D",
            "priority": 1,
            "apIndicator": "I",
            "hrIndicator": "A",
            "lastModified": "2016-04-08T16:14:01Z",
            "lastModifiedBy": "mye000005",
            "bankAccountNum": "736900542",
            "bankRoutingInfo": {
                "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                "id": 2,
                "version": 0,
                "bankName": "Chase Manhattan Bank",
                "bankRoutingNum": "748972234",
                "dataOrigin": null,
                "lastModified": "1999-08-17T03:34:22Z",
                "lastModifiedBy": "PAYROLL"
            },
            "amount": 77,
            "percent": null,
            "accountType": "C",
            "addressTypeCode": null,
            "addressSequenceNum": null,
            "intlAchTransactionIndicator": "N",
            "isoCode": null,
            "apAchTransactionTypeCode": null,
            "iatAddressTypeCode": null,
            "iatAddessSequenceNum": null,
            "amountType": "amount",
            "calculatedAmount": "\$77.00",
            "allocation": "\$77.00",
            "newPosition": 2
        }""".toString()

        controller.reorderAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,            updatedAccts[0].priority
        assertEquals '95003546',  updatedAccts[0].bankAccountNum
        assertEquals USER,     updatedAccts[0].lastModifiedBy

        assertEquals 2,            updatedAccts[1].priority
        assertEquals '736900542', updatedAccts[1].bankAccountNum
        assertEquals USER,         updatedAccts[1].lastModifiedBy

        assertEquals 3,            updatedAccts[2].priority
        assertEquals '67674852',   updatedAccts[2].bankAccountNum
        assertEquals GRAILS,         updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) > 2000)
    }

    @Test
    void testReorderAllAccounts() {
        loginSSB 'GDP000005', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\$1,397.74",
                "allocation": "50%"
            }, {
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\$77.00",
                "allocation": "\$77.00"
            }, {
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\$1,320.73",
                "allocation": "Remaining"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertTrue data[0][0]
        assertEquals 'xxxx3546', data[1].bankAccountNum
        assertEquals 'xxxxx0542', data[2].bankAccountNum
    }

    @Test
    void testReorderAllAccountsBySwappingFirstTwoAndCheckingLastModified() {
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\$1,397.74",
                "allocation": "50%"
            }, {
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\$77.00",
                "allocation": "\$77.00"
            }, {
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\$1,320.73",
                "allocation": "Remaining"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data

        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,           updatedAccts[0].priority
        assertEquals '95003546',  updatedAccts[0].bankAccountNum
        assertEquals USER,        updatedAccts[0].lastModifiedBy

        assertEquals 2,           updatedAccts[1].priority
        assertEquals '736900542', updatedAccts[1].bankAccountNum
        assertEquals USER,        updatedAccts[1].lastModifiedBy

        assertEquals 3,           updatedAccts[2].priority
        assertEquals '67674852',  updatedAccts[2].bankAccountNum
        assertEquals GRAILS,    updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) > 2000)
    }

    @Test
    void testReorderAllAccountsBySwappingLastTwoAndCheckingLastModified() {
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\$77.00",
                "allocation": "\$77.00"
            }, {
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\$1,320.73",
                "allocation": "Remaining"
            }, {
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\\\$1,397.74",
                "allocation": "50%"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data

        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,           updatedAccts[0].priority
        assertEquals '736900542', updatedAccts[0].bankAccountNum
        assertEquals GRAILS,    updatedAccts[0].lastModifiedBy

        assertEquals 2,           updatedAccts[1].priority
        assertEquals '67674852',  updatedAccts[1].bankAccountNum
        assertEquals USER,        updatedAccts[1].lastModifiedBy

        assertEquals 3,           updatedAccts[2].priority
        assertEquals '95003546',  updatedAccts[2].bankAccountNum
        assertEquals USER,        updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) > 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) < 2000)
    }

    @Test
    void testReorderAllAccountsBySwappingFirstWithLastAndCheckingLastModified() {
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\$1,320.73",
                "allocation": "Remaining"
            }, {
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\\\$1,397.74",
                "allocation": "50%"
            }, {
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\\\$77.00",
                "allocation": "\\\$77.00"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data

        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,           updatedAccts[0].priority
        assertEquals '67674852',  updatedAccts[0].bankAccountNum
        assertEquals USER,        updatedAccts[0].lastModifiedBy

        assertEquals 2,           updatedAccts[1].priority
        assertEquals '95003546',  updatedAccts[1].bankAccountNum
        assertEquals GRAILS,    updatedAccts[1].lastModifiedBy

        assertEquals 3,           updatedAccts[2].priority
        assertEquals '736900542', updatedAccts[2].bankAccountNum
        assertEquals USER,        updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) > 2000)
        assertTrue('Allocation "modified by" times are similar.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) > 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) < 2000)
    }

    @Test
    void testReorderAllAccountsByMovingFirstToLastAndCheckingLastModified() {
        def USER = 'GDP000005'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\\\$1,397.74",
                "allocation": "50%"
            }, {
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\\\$1,320.73",
                "allocation": "Remaining"
            }, {
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\\\$77.00",
                "allocation": "\\\$77.00"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data

        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,           updatedAccts[0].priority
        assertEquals '95003546',  updatedAccts[0].bankAccountNum
        assertEquals USER,        updatedAccts[0].lastModifiedBy

        assertEquals 2,           updatedAccts[1].priority
        assertEquals '67674852',  updatedAccts[1].bankAccountNum
        assertEquals USER,        updatedAccts[1].lastModifiedBy

        assertEquals 3,           updatedAccts[2].priority
        assertEquals '736900542', updatedAccts[2].bankAccountNum
        assertEquals USER,        updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) < 2000)
    }

    @Test
    void testReorderAllAccountsByMovingLastToFirstAndCheckingLastModified() {
        def USER = 'GDP000005'
        def GRAILS = 'GRAILS'
        loginSSB USER, '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[2].id},
                "version": 0,
                "dataOrigin": "Banner",
                "pidm": ${pidm},
                "status": "A",
                "documentType": "D",
                "priority": 1,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "67674852",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 100,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "remaining",
                "calculatedAmount": "\\\\\$1,320.73",
                "allocation": "Remaining"
            }, {
                "id": ${existingAccts[0].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 2,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "736900542",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 2,
                    "version": 0,
                    "bankName": "Chase Manhattan Bank",
                    "bankRoutingNum": "748972234",
                    "dataOrigin": null,
                    "lastModified": "1999-08-17T03:34:22Z",
                    "lastModifiedBy": "PAYROLL"
                },
                "amount": 77,
                "percent": null,
                "accountType": "C",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "amount",
                "calculatedAmount": "\\\$77.00",
                "allocation": "\\\$77.00"
            }, {
                "id": ${existingAccts[1].id},
                "version": 0,
                "dataOrigin": "Banner",
                "status": "A",
                "documentType": "D",
                "priority": 3,
                "apIndicator": "I",
                "hrIndicator": "A",
                "lastModified": "2016-04-07T19:54:44Z",
                "lastModifiedBy": "mye000005",
                "bankAccountNum": "95003546",
                "bankRoutingInfo": {
                    "class": "net.hedtech.banner.general.crossproduct.BankRoutingInfo",
                    "id": 6,
                    "version": 5,
                    "bankName": "First National Bank",
                    "bankRoutingNum": "234798944",
                    "dataOrigin": "GRAILS",
                    "lastModified": "2010-01-01T05:00:00Z",
                    "lastModifiedBy": "GRAILS"
                },
                "amount": null,
                "percent": 50,
                "accountType": "S",
                "addressTypeCode": null,
                "addressSequenceNum": null,
                "intlAchTransactionIndicator": "N",
                "isoCode": null,
                "apAchTransactionTypeCode": null,
                "iatAddressTypeCode": null,
                "iatAddessSequenceNum": null,
                "amountType": "percentage",
                "calculatedAmount": "\\\\\$1,397.74",
                "allocation": "50%"
        }]""".toString()

        controller.reorderAllAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data

        def updatedAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        // "lastModified" user ID and timestamp should change only on explicitly modified records.  Reprioritization
        // should not change those values in records whose position did not change.
        assertEquals 1,           updatedAccts[0].priority
        assertEquals '67674852',  updatedAccts[0].bankAccountNum
        assertEquals USER,        updatedAccts[0].lastModifiedBy

        assertEquals 2,           updatedAccts[1].priority
        assertEquals '736900542', updatedAccts[1].bankAccountNum
        assertEquals USER,        updatedAccts[1].lastModifiedBy

        assertEquals 3,           updatedAccts[2].priority
        assertEquals '95003546',  updatedAccts[2].bankAccountNum
        assertEquals USER,        updatedAccts[2].lastModifiedBy

        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[0].lastModified.getTime() - updatedAccts[1].lastModified.getTime()) < 2000)
        assertTrue('Allocation "modified by" times differ.', Math.abs(updatedAccts[1].lastModified.getTime() - updatedAccts[2].lastModified.getTime()) < 2000)
    }

    @Test
    void testGetCurrency() {
        loginSSB 'GDP000005', '111111'

        controller.getCurrency()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals '$', data.currencySymbol
    }

    @Test
    void testGetBankInfo() {
        loginSSB 'GDP000005', '111111'

        controller.request.json = '{bankRoutingNum: "123478902"}'

        controller.getBankInfo()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals '123478902', data.bankRoutingNum
        assertEquals 'First Fidelity', data.bankName
    }

    @Test
    void testReturnFailureMessage() {
        loginSSB 'GDP000005', '111111'

        controller.request.json = '{bankAccountNum: "123456789x123456789x123456789x12345"}'
        controller.validateAccountNum()

        def failureMessageModel = controller.response.json
        assert(failureMessageModel.failure)
        assertEquals("Invalid bank account number format.", failureMessageModel.message)
    }

    @Test
    void testReturnFailureMessageBadData() {
        loginSSB 'GDP000005', '111111'

        controller.request.contentType = "text/json"
        controller.request.json = '''{
            pidm:null,
            status:null,
            apIndicator:"I",
            hrIndicator:"blahblahblah",
            bankAccountNum:"0822051515",
            amount:null,
            percent:10,
            accountType:"C",
            bankRoutingInfo:{
                bankName:"First Fidelity",
                bankRoutingNum:"123478902",
            },
            amountType:"amount",
            priority:2,
            newPosition:2
        }'''

        controller.createAccount()

        def failureMessageModel = controller.response.json
        assert(failureMessageModel.failure)
        assertEquals(failureMessageModel.message.contains('ORA'), false)
    }

    @Test
    void testUnmaskAccountInfoFromSessionCacheWithSingleAccount() {
        def acctInfoToCache = [
            acctNum: '12345678',
            routing: [
                id: 10,
                bankRoutingNum: '87654321',
                bankName: 'River Bank'
            ]
        ]

        def acctSentFromUi = [
            id: 1,
            bankAccountNum: 'xxxx5678',
            bankRoutingInfo: [
                id: 10,
                bankRoutingNum: 'xxxx4321',
                bankName: 'River Bank'
            ]
        ]

        def unmaskedAcct = [
            id: 1,
            bankAccountNum: '12345678',
            bankRoutingInfo: [
                id: 10,
                bankRoutingNum: '87654321',
                bankName: 'River Bank'
            ]
        ]

        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(1, acctInfoToCache)
        def unmaskedAcctIds = controller.unmaskAccountInfoFromSessionCache([acctSentFromUi])

        // Account has now been unmasked
        assertEquals unmaskedAcct, acctSentFromUi

        def cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        // The value previously set in cache has not yet been cleared
        assertNotNull cachedInfo

        // One account was unmasked
        assertEquals 1, unmaskedAcctIds.size()

        // Clear the now unmasked items from the cache...
        controller.clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

        // ...and the item is no longer in the cache
        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)
        assertNull cachedInfo

    }

    @Test
    void testUnmaskAccountInfoFromSessionCacheWithMultipleAccounts() {
        def acctInfoToCache = [
                [
                    acctNum: '12345678',
                    routing: [
                        id: 10,
                        bankRoutingNum: '87654321',
                        bankName: 'River Bank'
                    ]
                ],
                [
                    acctNum: '23456789',
                    routing: [
                        id: 20,
                        bankRoutingNum: '98765432',
                        bankName: 'Outer Bank'
                    ]
                ],
                [
                    acctNum: '34567890',
                    routing: [
                        id: 30,
                        bankRoutingNum: '09876543',
                        bankName: 'Bank Shot'
                    ]
                ]
        ]

        def acctsSentFromUi = [
                [
                        id: 1,
                        bankAccountNum: 'xxxx5678',
                        bankRoutingInfo: [
                                id: 10,
                                bankRoutingNum: 'xxxx4321',
                                bankName: 'River Bank'
                        ]
                ],
                [
                        id: 2,
                        bankAccountNum: 'xxxx6789',
                        bankRoutingInfo: [
                                id: 20,
                                bankRoutingNum: 'xxxx5432',
                                bankName: 'Outer Bank'
                        ]
                ],
                [
                        id: 3,
                        bankAccountNum: 'xxxx7890',
                        bankRoutingInfo: [
                                id: 30,
                                bankRoutingNum: 'xxxx6543',
                                bankName: 'Bank Shot'
                        ]
                ]
        ]

        def unmaskedAccts = [
                [
                        id: 1,
                        bankAccountNum: '12345678',
                        bankRoutingInfo: [
                                id: 10,
                                bankRoutingNum: '87654321',
                                bankName: 'River Bank'
                        ]
                ],
                [
                        id: 2,
                        bankAccountNum: '23456789',
                        bankRoutingInfo: [
                                id: 20,
                                bankRoutingNum: '98765432',
                                bankName: 'Outer Bank'
                        ]
                ],
                [
                        id: 3,
                        bankAccountNum: '34567890',
                        bankRoutingInfo: [
                                id: 30,
                                bankRoutingNum: '09876543',
                                bankName: 'Bank Shot'
                        ]
                ]
        ]

        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(1, acctInfoToCache[0])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(2, acctInfoToCache[1])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(3, acctInfoToCache[2])
        def unmaskedAcctIds = controller.unmaskAccountInfoFromSessionCache(acctsSentFromUi)

        // Accounts have now been unmasked
        assertEquals unmaskedAccts, acctsSentFromUi

        def cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        // The value previously set in cache has not yet been cleared
        assertNotNull cachedInfo

        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(2)
        assertNotNull cachedInfo

        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(3)
        assertNotNull cachedInfo

        // Three accounts were unmasked
        assertEquals 3, unmaskedAcctIds.size()

        // Clear the now unmasked items from the cache...
        controller.clearAccountMaskingInfoFromSessionCache(unmaskedAcctIds)

        // ...and the items are no longer in the cache
        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)
        assertNull cachedInfo
        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(2)
        assertNull cachedInfo
        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(3)
        assertNull cachedInfo

    }

    @Test
    void testValidateAccountsAreUniqueWithAllUniqueAccounts() {
        def acctInfoToCache = [
                [
                    acctNum: '12345678',
                    routing: [
                        id: 10,
                        bankRoutingNum: '87654321',
                        bankName: 'River Bank'
                    ]
                ],
                [
                    acctNum: '12345678',
                    routing: [
                        id: 20,
                        bankRoutingNum: '87654321',
                        bankName: 'River Bank'
                    ]
                ],
                [
                    acctNum: '34567890',
                    routing: [
                        id: 30,
                        bankRoutingNum: '09876543',
                        bankName: 'Bank Shot'
                    ]
                ]
        ]

        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(1, acctInfoToCache[0])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(2, acctInfoToCache[1])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(3, acctInfoToCache[2])

        controller.request.contentType = "text/json"
        controller.request.json = """[{
            id: "1",
            accountType:"C",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxxx5678",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx4321"
            }
        },
        {
            id: "2",
            accountType:"S",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxxx5678",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx4321"
            }
        },
        {
            id: "3",
            accountType:"C",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxx7890",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx6543"
            }
        }]""".toString()

        controller.validateAccountsAreUnique()

        def failureMessageModel = controller.response.json

        assertNotNull failureMessageModel
        assertEquals false, failureMessageModel.failure
    }

    @Test
    void testValidateAccountsAreUniqueWithDuplicateAccounts() {
        def acctInfoToCache = [
                [
                    acctNum: '12345678',
                    routing: [
                        id: 10,
                        bankRoutingNum: '87654321',
                        bankName: 'River Bank'
                    ]
                ],
                [
                    acctNum: '12345678',
                    routing: [
                        id: 20,
                        bankRoutingNum: '87654321',
                        bankName: 'River Bank'
                    ]
                ],
                [
                    acctNum: '34567890',
                    routing: [
                        id: 30,
                        bankRoutingNum: '09876543',
                        bankName: 'Bank Shot'
                    ]
                ]
        ]

        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(1, acctInfoToCache[0])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(2, acctInfoToCache[1])
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(3, acctInfoToCache[2])

        controller.request.contentType = "text/json"
        controller.request.json = """[{
            id: "1",
            accountType:"S",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxxx5678",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx4321"
            }
        },
        {
            id: "2",
            accountType:"S",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxxx5678",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx4321"
            }
        },
        {
            id: "3",
            accountType:"C",
            apIndicator:"A",
            hrIndicator:"I",
            bankAccountNum:"xxx7890",
            bankRoutingInfo:{
                bankRoutingNum:"xxxx6543"
            }
        }]""".toString()

        controller.validateAccountsAreUnique()

        def failureMessageModel = controller.response.json

        assertNotNull failureMessageModel
        assertEquals true, failureMessageModel.failure
        assertEquals("Record already exists for this Bank Account.", failureMessageModel.message)
    }

}
