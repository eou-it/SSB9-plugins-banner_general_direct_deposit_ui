/*******************************************************************************
 Copyright 2015-2017 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import net.hedtech.banner.testing.BaseIntegrationTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test

class DirectDepositUtilityTests extends BaseIntegrationTestCase {

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
    void testSanitizeOnEmptyMap(){
        def map = [:]

        DirectDepositUtility.sanitizeMap(map)

        assertEquals(0, map.size())
    }

    @Test
    void testSanitizeOnCleanMap(){
        def map = [
                accountNumber: 123,
                bankName: 'My Bank'
        ]

        DirectDepositUtility.sanitizeMap(map)

        assertEquals(123, map.accountNumber)
        assertEquals('My Bank', map.bankName)
    }

    @Test
    void testSanitizeOnScriptTag(){
        def map = [
                accountNumber: 123,
                bankName: '<sCrIpT>alert(68541)<\\/sCrIpT>'
        ]

        DirectDepositUtility.sanitizeMap(map)

        assertEquals(123, map.accountNumber)
        assertEquals('', map.bankName)
    }

    @Test
    void testSanitizeWithNestedMap(){
        def map = [
                accountNumber: 123,
                bankName: '<sCrIpT>alert(68541)<\\/sCrIpT>',
                nested: [
                        accountType: '<sCrIpT>alert(999)<\\/sCrIpT>',
                        addressSequenceNum: 5555,
                        apIndicator: 'I'
                ]
        ]

        DirectDepositUtility.sanitizeMap(map)

        assertEquals(123, map.accountNumber)
        assertEquals('', map.bankName)
        assertEquals('', map.nested.accountType)
        assertEquals(5555, map.nested.addressSequenceNum)
        assertEquals('I', map.nested.apIndicator)
    }

    @Test
    void testSetAndGetAndRemoveDirectDepositAccountInfoInSessionCache() {
        DirectDepositUtility.setDirectDepositAccountInfoInSessionCache(1, [id: '333'])
        def cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedInfo
        assertEquals '333', cachedInfo.id

        DirectDepositUtility.removeDirectDepositAccountInfoFromSessionCache('1')
        cachedInfo = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNull cachedInfo
    }

    @Test
    void testMaskBankInfoLongNumber() {
        assertEquals 'xxxx5678', DirectDepositUtility.maskBankInfo('12345678')
    }

    @Test
    void testMaskBankInfoMediumNumber() {
        assertEquals '5678', DirectDepositUtility.maskBankInfo('5678')
    }

    @Test
    void testMaskBankInfoShortNumber() {
        assertEquals '78', DirectDepositUtility.maskBankInfo('78')
    }

    @Test
    void testMaskAccountsWithList() {
        def accounts = [
            [
                id: 1,
                bankAccountNum: '12345678',
                bankRoutingInfo: [
                    bankRoutingNum: '87654321'
                ]
            ]
        ]

        def maskedAccounts = DirectDepositUtility.maskAccounts(accounts)

        assertEquals 1, maskedAccounts.size()

        def account = maskedAccounts[0]

        assertEquals 'xxxx5678', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        def cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedAccount
        assertEquals '12345678', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum
    }

    @Test
    void testMaskAccountsWithArray() {
        def accounts = [
            [
                id: 1,
                bankAccountNum: '12345678',
                bankRoutingInfo: [
                    bankRoutingNum: '87654321'
                ]
            ]
        ] as Object[]

        def maskedAccounts = DirectDepositUtility.maskAccounts(accounts)

        assertEquals 1, maskedAccounts.size()

        def account = maskedAccounts[0]

        assertEquals 'xxxx5678', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        def cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedAccount
        assertEquals '12345678', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum
    }

    @Test
    void testMaskAccountsWithSingleObject() {
        def account = [
            id: 1,
            bankAccountNum: '12345678',
            bankRoutingInfo: [
                bankRoutingNum: '87654321'
            ]
        ]

        def maskedAccount = DirectDepositUtility.maskAccounts(account)

        assertNotNull maskedAccount

        // Since maskAccounts mutates the object(s) passed in, these should be equal:
        assertEquals account, maskedAccount

        assertEquals 'xxxx5678', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        def cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedAccount
        assertEquals '12345678', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum
    }

    @Test
    void testMaskAccountsWithDuplicateRoutingInfo() {
        def accounts = [
            [
                id: 1,
                bankAccountNum: '12345678',
                bankRoutingInfo: [
                    bankRoutingNum: '87654321'
                ]
            ],
            [
                id: 2,
                bankAccountNum: '44444444',
                bankRoutingInfo: [
                    bankRoutingNum: '87654321'
                ]
            ]
        ]

        def maskedAccounts = DirectDepositUtility.maskAccounts(accounts)

        assertEquals 2, maskedAccounts.size()

        // First account
        def account = maskedAccounts[0]

        assertEquals 'xxxx5678', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        def cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(1)

        assertNotNull cachedAccount
        assertEquals '12345678', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum

        // Second account
        account = maskedAccounts[1]

        assertEquals 'xxxx4444', account.bankAccountNum
        assertEquals 'xxxx4321', account.bankRoutingInfo.bankRoutingNum

        cachedAccount = DirectDepositUtility.getDirectDepositAccountInfoFromSessionCache(2)

        assertNotNull cachedAccount
        assertEquals '44444444', cachedAccount.acctNum
        assertEquals '87654321', cachedAccount.routing.bankRoutingNum
    }

    @Test
    void testIsCollectionOrArrayUsingList() {
        assertTrue DirectDepositUtility.isCollectionOrArray([1, 2, 3])
    }

    @Test
    void testIsCollectionOrArrayUsingArray() {
        assertTrue DirectDepositUtility.isCollectionOrArray([1, 2, 3] as Integer[])
    }

    @Test
    void testIsCollectionOrArrayUsingSingleObject() {
        assertFalse DirectDepositUtility.isCollectionOrArray(new Integer(1))
    }

}
