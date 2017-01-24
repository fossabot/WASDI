/**
 * Created by a.corrado on 23/01/2017.
 */
'use strict';
angular.module('wasdi.RabbitStompService', ['wasdi.RabbitStompService']).
service('RabbitStompService', ['$http',  'ConstantsService','$interval', function ($http, oConstantsService,$interval,$scope) {

    // Reconnection promise to stop the timer if the reconnection succeed or if the user change page
    this.m_oInterval = $interval;
    this.m_oConstantsService = oConstantsService;
    this.m_oScope = $scope;
    this.m_oActiveWorkspace=null;

    this.m_oReconnectTimerPromise = null;
    this.m_oRabbitReconnect = null;
    this.m_oClient = null;
    this.m_oOn_Connect = null;
    this.m_oOn_Error = null;
    this.m_oRabbitReconnect = null;

    this.m_oSubscription = null;


    /*@Params: WorkspaceID, Name of controller, Controller
    * it need the Controller for call the methods (the methods are inside the active controllers)
    * the methods are call in oRabbitCallback
    * */
    this.initWebStomp = function(oActiveWorkspace,sControllerName,oControllerActive)
    {
        if(utilsIsObjectNullOrUndefined(oActiveWorkspace) || utilsIsObjectNullOrUndefined(oControllerActive) || utilsIsStrNullOrEmpty(sControllerName))
            return false;

        this.m_oActiveWorkspace=oActiveWorkspace;

        // Web Socket to receive workspace messages
        var oWebSocket = new WebSocket(this.m_oConstantsService.getStompUrl());
        var oController = this;
        this.m_oClient = Stomp.over(oWebSocket);

        /**
         * Rabbit Callback: receives the Messages
         * @param message
         */
        var oRabbitCallback = function (message) {
            // called when the client receives a STOMP message from the server
            if (message.body) {
                console.log("got message with body " + message.body)

                // Get The Message View Model
                var oMessageResult = JSON.parse(message.body);

                if (oMessageResult == null) return;
                if (oMessageResult.messageResult == "KO") {
                    //TODO REMOVE ELEMENT IN PROCESS QUEUE
                    alert('There was an error in the RabbitCallback');
                    return;
                }

                // Route the message
                if (oMessageResult.messageCode == "DOWNLOAD") {

                    if(sControllerName == "EditorController" || sControllerName == "ImportController")
                        oControllerActive.receivedDownloadMessage(oMessageResult);
                    //TODO ERRROR CASE
                }
                else if (oMessageResult.messageCode == "PUBLISH") {
                    if(sControllerName == "EditorController" )
                        oControllerActive.receivedPublishMessage(oMessageResult);
                    //TODO ERRROR CASE

                }
                else if (oMessageResult.messageCode == "PUBLISHBAND") {

                    if(sControllerName == "EditorController" || sControllerName == "ImportController")
                        oControllerActive.receivedPublishBandMessage(oMessageResult.payload.layerId);
                    //TODO ERRROR CASE

                }

            } else {
                console.log("got empty message");
            }
        }
            //oController.addTestLayer(message.body);
            /**
             * Callback of the Rabbit On Connect
             */

            var on_connect = function () {
                console.log('Web Stomp connected');

                //CHECK IF sWorkSpaceId is null
                var sWorkSpaceId = null;
                    sWorkSpaceId = oController.m_oActiveWorkspace.workspaceId;

                oController.m_oSubscription = oController.m_oClient.subscribe(sWorkSpaceId, oRabbitCallback);

                // Is this a re-connection?
                if (oController.m_oReconnectTimerPromise != null) {
                    // Yes it is: clear the timer
                    oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
                    oController.m_oReconnectTimerPromise = null;
                }
            };


            /**
             * Callback for the Rabbit On Error
             */
            var on_error = function (sMessage) {
                console.log('Web Stomp Error');
                if (sMessage == "LOST_CONNECTION") {
                    console.log('LOST Connection');

                    if (oController.m_oReconnectTimerPromise == null) {
                        // Try to Reconnect
                        oController.m_oReconnectTimerPromise = oController.m_oInterval(oController.m_oRabbitReconnect, 5000);
                    }
                }
            };

            // Keep local reference to the callbacks to use it in the reconnection callback
            this.m_oOn_Connect = on_connect;
            this.m_oOn_Error = on_error;

            // Call back for rabbit reconnection
            var rabbit_reconnect = function () {

                console.log('Web Stomp Reconnection Attempt');

                // Connect again
                oController.oWebSocket = new WebSocket(oController.m_oConstantsService.getStompUrl());
                oController.m_oClient = Stomp.over(oController.oWebSocket);
                oController.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), oController.m_oOn_Connect, oController.m_oOn_Error, '/');
            };
            this.m_oRabbitReconnect = rabbit_reconnect;
            //connect to the queue
            this.m_oClient.connect(oController.m_oConstantsService.getRabbitUser(), oController.m_oConstantsService.getRabbitPassword(), on_connect, on_error, '/');


            //// Clean Up when exit!!
            oControllerActive.m_oScope.$on('$destroy', function () {
                // Is this a re-connection?
                if (oController.m_oReconnectTimerPromise != null) {
                    // Yes it is: clear the timer
                    oController.m_oInterval.cancel(oController.m_oReconnectTimerPromise);
                    oController.m_oReconnectTimerPromise = null;
                }
                else {
                    if (oController.m_oClient != null) {
                        oController.m_oClient.disconnect();
                    }
                }
            });

        return true;
    }

    //This method remove a message in all queues
    //this.removeMessageInQueues = function(oMessage)
    //{
    //    if(utilsIsObjectNullOrUndefined(oMessage))
    //        return false;
    //    var iIndexMessageInEditoControllerQueue = utilsFindObjectInArray(this.m_aoEditorControllerQueueMessages,oMessage) ;
    //    var iIndexMessageInImportControllerQueue = utilsFindObjectInArray(this.m_aoImportControllerQueueMessages,oMessage) ;
    //    // TODO REMOVE TO CACHE
    //    /*Remove in editor controller*/
    //    if (iIndexMessageInEditoControllerQueue > -1) {
    //        this.m_aoEditorControllerQueueMessages.splice(iIndexMessageInEditoControllerQueue, 1);
    //    }
    //    /*remove in Import Controller*/
    //    if ( iIndexMessageInImportControllerQueue > -1) {
    //        this.m_aoImportControllerQueueMessages.splice( iIndexMessageInImportControllerQueue, 1);
    //    }
    //
    //}



}]);

