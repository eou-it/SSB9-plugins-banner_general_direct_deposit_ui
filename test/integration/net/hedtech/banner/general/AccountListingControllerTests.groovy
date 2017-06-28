package net.hedtech.banner.general

import grails.converters.JSON
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

import net.hedtech.banner.testing.BaseIntegrationTestCase

import net.hedtech.banner.general.AccountListingController

class AccountListingControllerTests extends BaseIntegrationTestCase {

    /**
     * The setup method will run before all test case method executions start.
     */
    @Before
    public void setUp() {
        formContext = ['GUAGMNU']
        controller = new AccountListingController()
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
    void testGetApAccountsForCurrentUser(){
        loginSSB 'HOSH00018', '111111'
        
        controller.request.contentType = "text/json"
        controller.getApAccountsForCurrentUser()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 'xxxx7543', data[0].bankAccountNum
    }

    @Test
    void testGetUserPayrollAllocations(){
        loginSSB 'GDP000005', '111111'

        controller.getUserPayrollAllocations()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 'xxxxx0542', data.allocations[0].bankAccountNum
    }

    @Test
    void testGetLastPayDateInfo(){
        loginSSB 'HOP510001', '111111'

        controller.getLastPayDateInfo()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 7837.31, data.totalNet, 0.001
        assertEquals 1, data.docAccts.size()
    }

    @Test
    void testMaskBankInfoLongNumber() {
        assertEquals 'xxxx5678', controller.maskBankInfo('12345678')
    }

    @Test
    void testMaskBankInfoMediumNumber() {
        assertEquals '5678', controller.maskBankInfo('5678')
    }

    @Test
    void testMaskBankInfoShortNumber() {
        assertEquals '78', controller.maskBankInfo('78')
    }

    @Test
    void testMaskAccounts() {
        def accounts = [
            [
                id: 1,
                bankAccountNum: '12345678',
                bankRoutingInfo: [
                    bankRoutingNum: '87654321'
                ]
            ]
        ]

        def maskedAccounts = controller.maskAccounts(accounts)

        assertEquals 1, maskedAccounts.size()

        def account = maskedAccounts[0]

        assertEquals 'xxxx5678', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        def cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedAccount
        assertEquals '12345678', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum
    }

}
