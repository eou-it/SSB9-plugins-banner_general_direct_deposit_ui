/*******************************************************************************
 Copyright 2015 Ellucian Company L.P. and its affiliates.
 *******************************************************************************/

generalSsbAppDirectives.directive('sampleCheckPopOver', ['$filter', 'directDepositService', function($filter, directDepositService) {

    var template = '<button class="icon-info-CO" aria-label="'+$filter("i18n")("directDeposit.label.show.check")+'"></button>';

    var link = function (scope, element, attrs) {
        element.on('click', function(event) {
            var src = attrs.popoverImg || '',
                alt = attrs.popoverAlt || 'Sample check';

            // Prevent the hidePopover directive from handling the event, immediately closing the popover
            event.stopImmediatePropagation();

            // Toggle popover open/closed
            if (element.next('.popover.in').length !== 0) {
                // Popover is already open, toggle it closed
                element.popover('destroy');
            } else {
                // Destroy any existing popovers
                directDepositService.destroyAllPopovers();

                // Open popover
                element.popover({
                    content: '<img class="sample-check" src="' + src + '" alt="' + alt + '">',
                    trigger: 'manual',
                    placement: 'bottom',
                    html: true
                });

                // Adjust positioning, etc. based on screen dimensions
                element.on('shown.bs.popover', function(event) {
                    var flipsLeft = isElementRightOfCenter(event.target),
                        flipClass = flipsLeft ? 'flipleft' : 'flipright',
                        popoverElement = $(event.target).next(),
                        leftOffset = flipsLeft ? -142 : 162;

                    popoverElement.css('left', parseInt(popoverElement.css('left')) + leftOffset + 'px');
                    popoverElement.css('top', parseInt(popoverElement.css('top')) - 1 + 'px');
                    popoverElement.find('.popover-content').css('padding', '5px');
                    popoverElement.addClass(flipClass);
                });

                element.popover('show');

                // Make message available to screen reader.  This function call results in message being
                // set in audible message div as well as that div being made visibile, which causes readers
                // to pick up on the role=alert and aria-live=assertive attributes, reading the message.
                directDepositService.setPlayAudibleMessage(alt, element);
            }
        });
    };

    function isElementRightOfCenter(element) {
        var clientElement = $('.dd-popover-client'),
            clientElementLeftEdge,
            clientCenter,
            iconLeftEdge = $(element).offset().left,
            iconCenter = iconLeftEdge + ($(element).width() / 2);

        if (clientElement.length === 0) {
            // Default to right-of-center
            return true;
        }

        clientElementLeftEdge = clientElement.offset().left;
        clientCenter = clientElementLeftEdge + clientElement.width() / 2;

        return iconCenter > clientCenter;
    }

    return {
        restrict: 'E',
        template: template,
        link : link
    };
}]);

generalSsbAppDirectives.directive('ddPopOver', ['$filter', 'directDepositService', 'ddListingService',
    function($filter, directDepositService, ddListingService) {

    var link = function (scope, element, attrs) {
        var width = attrs.popoverWidth || '300px',
            position = attrs.popoverPosition || 'top',
            msg = $filter('i18n')(attrs.popoverMsg),
            template = '<div class="popover dd-tooltip" style="width: ' + width + ';"><div class="popover-content"></div><div class="arrow"></div></div>';

        scope.togglePopover = function(event) {
            // Prevent the hidePopover directive from handling the event, immediately closing the popover
            if (event) {
                event.stopImmediatePropagation();
            }

            // Toggle popover open/closed
            if (element.next('.popover.in').length !== 0) {
                // Popover is already open, toggle it closed
                element.popover('destroy');
            } else {
                // Destroy any existing popovers
                directDepositService.destroyAllPopovers();

                // Open popover
                element.popover({
                    template: template,
                    content: msg,
                    trigger: 'manual',
                    placement: position,
                    html: true
                });

                element.popover('show');

                // Make message available to screen reader.  This function call results in message being
                // set in audible message div as well as that div being made visibile, which causes readers
                // to pick up on the role=alert and aria-live=assertive attributes, reading the message.
                directDepositService.setPlayAudibleMessage(msg, element);
            }
        };

        element.on('click', function(event) {
            scope.togglePopover(event);
        });
    };

    return {
        restrict: 'A',
        link : link
    };
}]);

generalSsbAppDirectives.directive('hidePopover', ['directDepositService', function(directDepositService) {

    var link = function(scope, element) {
        element.on('click', function(event) {
            // If not clicking on the modal (the check for ".parents('.popover.in')" checks for that),
            // destroy all popovers.
            if ($(event.target).parents('.popover.in').length === 0) {
                directDepositService.destroyAllPopovers();
            }
        });
    };

    return {
        restrict: 'A',
        link: link
    };
}]);
