angular.module('wasdi.wapSelectArea', [])
    .directive('wapselectarea', ['MapService',function ($MapService) {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                heightMap: '=',
                widthMap: '=',
                boundingBox: '='
            },
            template: `<div ng-attr-id="{{$ctrl.mapId}}" ng-style="$ctrl.oMapStyle"></div>`,

            controller: function() {

                // generated a new id map number and converted as string
                this.mapId = "" + Date.now() + Math.random();

                this.oMap = null;
                this.m_oDrawnItems = {};

                //CHECK IF Height or width are null or undefined
                if(utilsIsANumber(this.heightMap ) === false ){
                    this.heightMap = 0;
                    console.error('height-map parameter is not a number');
                }
                if(utilsIsANumber(this.widthMap ) === false ){
                    this.widthMap = 0;
                    console.error('width-map parameter is not a number');
                }

                this.oMapStyle = { height: this.heightMap + 'px',
                                   width: this.widthMap + 'px'  };

                this.addBoundingBoxDrawerOnMap = function (oMap) {

                    if(oMap === null || oMap === undefined ){
                        return null;
                    }

                    this.m_oDrawnItems = new L.FeatureGroup();

                    oMap.addLayer(this.m_oDrawnItems);

                    var oOptions={
                        position:'topright',//position of menu
                        draw:{// what kind of shapes are disable/enable
                            marker:false,
                            polyline:false,
                            circle:false,
                            circlemarker:false,
                            polygon:false
                        },

                        edit: {
                            featureGroup: this.m_oDrawnItems,//draw items are the "voice" of menu
                            edit: false,// hide edit button
                            remove: false// hide remove button
                        }
                    };

                    var oDrawControl = new L.Control.Draw(oOptions);

                    oMap.addControl(oDrawControl);

                    //Without this.m_oWasdiMap.on() the shape isn't saved on map
                    var oController = this;
                    oMap.on(L.Draw.Event.CREATED, function (event)
                    {
                        var layer = event.layer;
                        oController.boundingBox.northEast = layer._bounds._northEast;
                        oController.boundingBox.southWest = layer._bounds._southWest;

                        //remove old shape
                        if(oController.m_oDrawnItems && oController.m_oDrawnItems.getLayers().length!==0){
                            oController.m_oDrawnItems.clearLayers();
                        }
                        //save new shape in map
                        oController.m_oDrawnItems.addLayer(layer);
                    });

                    oMap.on(L.Draw.Event.DELETESTOP, function (event) {
                        var layer = event.layers;
                    });
                    return oMap;
                }

                let oController = this;
                //init the map after the directive is loaded
                setTimeout(function() {
                    oController.oMap = $MapService.initMapSingleton(oController.mapId);

                    //is it an option?
                    oController.oMap = oController.addBoundingBoxDrawerOnMap(oController.oMap);
                }, 500);

            },
            controllerAs: '$ctrl'
        };
    }]);

