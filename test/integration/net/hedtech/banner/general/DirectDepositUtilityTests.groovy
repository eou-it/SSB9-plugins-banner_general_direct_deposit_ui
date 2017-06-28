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

}
