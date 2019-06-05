/*******************************************************************************
 Copyright 2019 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

package net.hedtech.banner.general

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class DirectDepositInterceptorSpec extends Specification implements InterceptorUnitTest<DirectDepositInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test directDeposit interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"directDeposit")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
