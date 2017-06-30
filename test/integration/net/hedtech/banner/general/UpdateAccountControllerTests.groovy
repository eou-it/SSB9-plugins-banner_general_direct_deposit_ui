/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import grails.converters.JSON
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
    void testCreateAccount() {
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
            version:0,
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
            pidm:${pidm},
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
        def existingAccts = controller.directDepositAccountService.getActiveApAccounts(pidm) //36732
        
        controller.request.contentType = "text/json"
        controller.request.json = """[{
            id: ${existingAccts[0].id},
            class:"net.hedtech.banner.general.overall.DirectDepositAccount",
            accountType:"C",
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
            pidm:${pidm},
            priority:3,
            status:"P",
            apDelete:true
        }]""".toString()

        controller.deleteAccounts()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals 1, data[0].size()
        assertEquals true, data[0][0]
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
            "pidm": ${pidm},
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
    void testReorderAllAccounts() {
        loginSSB 'GDP000005', '111111'

        def pidm = ControllerUtility.getPrincipalPidm()
        def existingAccts = controller.directDepositAccountCompositeService.getUserHrAllocations(pidm).allocations //36743

        controller.request.contentType = "text/json"
        controller.request.json = """[{
                "id": ${existingAccts[1].id},
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
                "pidm": ${pidm},
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
        assertEquals data[0][0], true
        assertEquals 'xxxx3546', data[1].bankAccountNum
        assertEquals 'xxxxx0542', data[2].bankAccountNum
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
    void testUnmaskAccountInfoFromSessionCache() {
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
        controller.unmaskAccountInfoFromSessionCache(acctSentFromUi)

        assertEquals unmaskedAcct, acctSentFromUi

        def cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        // The value previously set in cache has been cleared
        assertNull cachedInfo
    }

}
