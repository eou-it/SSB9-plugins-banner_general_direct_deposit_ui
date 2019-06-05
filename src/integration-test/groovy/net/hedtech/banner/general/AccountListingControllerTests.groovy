/*******************************************************************************
 Copyright 2015-2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import grails.converters.JSON
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.GrailsWebMockUtil
import grails.util.Holders
import grails.web.servlet.context.GrailsWebApplicationContext
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

import net.hedtech.banner.testing.BaseIntegrationTestCase

import net.hedtech.banner.general.AccountListingController

@Integration
@Rollback
class AccountListingControllerTests extends BaseIntegrationTestCase {

    def controller

    /**
     * The setup method will run before all test case method executions start.
     */
    @Before
    public void setUp() {
        formContext = ['SELFSERVICE','GUAGMNU']
        super.setUp()
        webAppCtx = new GrailsWebApplicationContext()
        controller = Holders.grailsApplication.getMainContext().getBean("net.hedtech.banner.general.AccountListingController")
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
        mockRequest()
        SSBSetUp('HOSH00018', '111111')

        controller.request.contentType = "text/json"
        controller.getApAccountsForCurrentUser()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 'xxxx7543', data[0].bankAccountNum
    }

    @Test
    void testGetApAccountsForCurrentUserWithInactive(){
        mockRequest()
        SSBSetUp('GDP000001', '111111')

        controller.request.contentType = "text/json"
        controller.getApAccountsForCurrentUser()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 3, data.size()
        assertTrue data.bankAccountNum.contains('xxxx4850')
        assertTrue data.bankAccountNum.contains('xxxx4852')
        assertTrue data.bankAccountNum.contains('xxxxx7244')
    }

    @Test
    void testGetUserPayrollAllocations(){
        mockRequest()
        SSBSetUp('GDP000005', '111111')

        controller.getUserPayrollAllocations()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 'xxxxx0542', data.allocations[0].bankAccountNum
    }

    @Test
    void testGetLastPayDateInfo(){
        mockRequest()
        SSBSetUp('HOP510001', '111111')

        controller.getLastPayDateInfo()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )
        println data
        assertNotNull data
        assertEquals 7837.31, data.totalNet, 0.001
        assertEquals 1, data.docAccts.size()
    }

    @Test
    void testGetCurrency() {
        mockRequest()
        SSBSetUp('GDP000005', '111111')

        controller.getCurrency()
        def dataForNullCheck = controller.response.contentAsString
        def data = JSON.parse( dataForNullCheck )

        assertNotNull data
        assertEquals '$', data.currencySymbol
    }

    public GrailsWebRequest mockRequest() {
        GrailsMockHttpServletRequest mockRequest = new GrailsMockHttpServletRequest();
        GrailsMockHttpServletResponse mockResponse = new GrailsMockHttpServletResponse();
        GrailsWebMockUtil.bindMockWebRequest(webAppCtx, mockRequest, mockResponse)
    }

}
