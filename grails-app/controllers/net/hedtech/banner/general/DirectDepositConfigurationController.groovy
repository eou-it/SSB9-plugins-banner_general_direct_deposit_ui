package net.hedtech.banner.general

import grails.converters.JSON
import net.hedtech.banner.exceptions.ApplicationException

class DirectDepositConfigurationController {

    def directDepositConfigurationService

    def getConfig() {
        try {
            render directDepositConfigurationService.getDirectDepositParams() as JSON
        } catch (ApplicationException e) {
            render ControllerUtility.returnFailureMessage(e) as JSON
        }
    }

}
