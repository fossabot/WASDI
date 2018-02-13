/**
 * Created by a.corrado on 21/04/2017.
 */


/*
    this method take in input a JSON with this format:
 [
 {
 "alias": "",
 "condition": "",
 "defaultValue": "Sentinel Precise (Auto Download)",
 "description": "",
 "field": "orbitType",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Orbit State Vectors",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": [
 "Sentinel Precise (Auto Download)",
 "Sentinel Restituted (Auto Download)",
 "DORIS Preliminary POR (ENVISAT)",
 "DORIS Precise VOR (ENVISAT) (Auto Download)",
 "DELFT Precise (ENVISAT, ERS1&2) (Auto Download)",
 "PRARE Precise (ERS1&2) (Auto Download)",
 "Kompsat5 Precise"
 ]
 },
 {
 "alias": "",
 "condition": "",
 "defaultValue": "3",
 "description": "",
 "field": "polyDegree",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Polynomial Degree",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": []
 },
 {
 "alias": "",
 "condition": "",
 "defaultValue": "false",
 "description": "",
 "field": "continueOnFail",
 "format": "",
 "interval": "",
 "itemAlias": "",
 "label": "Do not fail if new orbit file is not found",
 "notEmpty": false,
 "notNull": false,
 "pattern": "",
 "unit": "",
 "valueSet": []
 }
 ]

 i must convert it in this format
    {
    orbitType:"Sentinel Precise (Auto Download)"
    polyDegree:3,
    continueOnFail:false,
    }
 */
function utilsProjectConvertJSONFromServerInOptions(oJSONInput)
{
    if(utilsIsObjectNullOrUndefined(oJSONInput) == true)
        return null;
    var iNumberOfParameters = oJSONInput.length;
    if( iNumberOfParameters == 0)
        return null;

    var oNewObjectOutput = {};
    for(var iIndexParameter = 0; iIndexParameter < iNumberOfParameters; iIndexParameter++ )
    {
        if( ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter]) == true ) || ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].field) == true ) ||
            ( utilsIsStrNullOrEmpty(oJSONInput[iIndexParameter].field) == true ) || ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].defaultValue) == true ) )
        {
            next;//return null?
        }
        else
        {
            switch(oJSONInput[iIndexParameter].defaultValue)
            {
                case "true":oNewObjectOutput[oJSONInput[iIndexParameter].field] = true;//convert string in boolean
                            break;
                case "false":oNewObjectOutput[oJSONInput[iIndexParameter].field] = false;//convert string in boolean
                            break;
                default: oNewObjectOutput[oJSONInput[iIndexParameter].field] = oJSONInput[iIndexParameter].defaultValue;
            }


        }
    }
    return oNewObjectOutput;
}

function utilsProjectGetArrayOfValuesForParameterInOperation(oJSONInput,sProperty)
{
    if( utilsIsObjectNullOrUndefined(oJSONInput) === true ) {
        return null;
    }
    var iNumberOfParameters = oJSONInput.length;
    if( iNumberOfParameters === 0)
        return null;
    if(utilsIsObjectNullOrUndefined(sProperty) === true)
        return null;

    var oReturnArray = [];
    for(var iIndexParameter = 0; iIndexParameter < iNumberOfParameters; iIndexParameter++ )
    {
        // if field === sProperty
        if( ( utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter]) === false )&&(utilsIsObjectNullOrUndefined(oJSONInput[iIndexParameter].field) === false)
                                                                                 &&(oJSONInput[iIndexParameter].field === sProperty) )
        {

            if(oJSONInput[iIndexParameter].valueSet.length !== 0)
            {
                // if valueSet isn't empty
                return oJSONInput[iIndexParameter].valueSet;
            }
        }
    }
}
//test
String.prototype.distance = function (char) {
    var index = this.indexOf(char);

    if (index === -1) {
        alert(char + " does not appear in " + this);
    } else {
        alert(char + " is " + (this.length - index) + " characters from the end of the string!");
    }
}




function utilsProjectShowRabbitMessageUserFeedBack(oMessage) {

    var sMessageCode = oMessage.messageCode;
    var sUserMessage = "";
    // Get extra operations
    switch(sMessageCode)
    {
        case "DOWNLOAD":
            sUserMessage = "FILE NOW AVAILABLE ON WASDI SERVER<br>READY";
            break;
        case "PUBLISH":
            sUserMessage = "PUBLISH DONE<br>READY";
            break;
        case "PUBLISHBAND":
            sUserMessage = "BAND PUBLISHED: " + oMessage.payload.bandName + "<br>PRODUCT: <br> " + oMessage.payload.productName + "<br>READY";
            break;
        case "UPDATEPROCESSES":
            console.log("UPDATE PROCESSES"+" " +utilsGetTimeStamp());
            break;
        case "APPLYORBIT":
            sUserMessage = "APPLY ORBIT COMPLETED<br>READY";
            break;
        case "CALIBRATE":
            sUserMessage = "RADIOMETRIC CALIBRATE COMPLETE<br>READY";
            break;
        case "MULTILOOKING":
            sUserMessage = "Multilooking Completed<br>READY";
            break;
        case "NDVI":
            sUserMessage = "NDVI Completed<br>READY";
            break;
        case "TERRAIN":
            sUserMessage = "RANGE DOPPLER TERRAIN CORRECTION COMPLETED<br>READY";
            break;

        default:
            console.log("RABBIT ERROR: GOT EMPTY MESSAGE<br>READY");
    }

    // Is there a feedback for the user?
    if (!utilsIsStrNullOrEmpty(sUserMessage)) {
        // Give the short message
        var oDialog = utilsVexDialogAlertBottomRightCorner(sUserMessage);
        utilsVexCloseDialogAfterFewSeconds(4000,oDialog);
    }

}

function utilsJstreeUpdateLabelNode (sIdNodeInput, sNewLabelNodeInput)
{
    if(utilsIsObjectNullOrUndefined(sIdNodeInput) === true)return false;

    if(utilsIsObjectNullOrUndefined(sNewLabelNodeInput) === true)return false;

    var oNode = null;
    oNode = $('#jstree').jstree(true).get_node(sIdNodeInput);
    $('#jstree').jstree(true).rename_node(oNode,sNewLabelNodeInput);

    return true;
};

function utilsProjectConvertPositionsSatelliteFromServerInCesiumArray(aaArrayInput)
{
    if(utilsIsObjectNullOrUndefined(aaArrayInput) === true)
        return [];
    if(aaArrayInput.length === 0 )
        return [];

    var iLengthArray = aaArrayInput.length;
    var aReturnArray = [];
    var aTemp = [];
    for( var iIndexArray = 0; iIndexArray < iLengthArray; iIndexArray++)
    {
        aTemp = aaArrayInput[iIndexArray].split(";");

        // skip if aTemp is wrong
        if(utilsIsObjectNullOrUndefined(aTemp)=== true ||  aTemp.length !== 4)
            continue;

        aReturnArray.push(aTemp[1]);//push log
        aReturnArray.push(aTemp[0]);//push lat
        aReturnArray.push(aTemp[2]);//push alt
    }

    return aReturnArray;
};

function utilsProjectConvertCurrentPositionFromServerInCesiumDegrees(sInput)
{
    if(utilsIsStrNullOrEmpty(sInput) === true)
        return [];

    var aSplitedInput = sInput.split(";")
    var aReturnValue = [];
    aReturnValue.push(aSplitedInput[1]);
    aReturnValue.push(aSplitedInput[0]);
    aReturnValue.push(aSplitedInput[2]);
    return aReturnValue;
}

function utilsProjectCheckInDialogIfProductNameIsInUsed(sProductName, aoListOfProducts)
{
    if(aoListOfProducts === null || sProductName === null )
        return false;
    var iNumberOfProducts = aoListOfProducts.length;
    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
    {
        if(aoListOfProducts[iIndexProduct].name === sProductName)
            return true;
    }
    return false;
}

function utilsProjectGetSelectedBandsByProductName(sProductName, asSelectedBands)
{
    if(utilsIsObjectNullOrUndefined(asSelectedBands) === true)
        return null;

    var iNumberOfSelectedBands = asSelectedBands.length;
    var asReturnBandsName = [];
    for( var iIndexSelectedBand = 0 ; iIndexSelectedBand < iNumberOfSelectedBands; iIndexSelectedBand++ )
    {
        //check if the asSelectedBands[iIndexSelectedBand] is a sProductName band
        if(utilsIsSubstring(asSelectedBands[iIndexSelectedBand] , sProductName))
        {
            var sBandName=  asSelectedBands[iIndexSelectedBand].replace(sProductName + "_","");
            asReturnBandsName.push(sBandName);
        }
    }

    return asReturnBandsName;
}

function utilsProjectGetProductsName (aoProducts){
    if(utilsIsObjectNullOrUndefined(aoProducts) === true)
        return null;
    var iNumberOfProducts = aoProducts.length;
    var asProductsName = [];
    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
    {
        asProductsName.push(aoProducts[iIndexProduct].name);
    }
    return asProductsName;
}

function utilsProjectGetProductByName(sName,aoProducts){
    if(utilsIsStrNullOrEmpty(sName) === true)
        return null;
    var iNumberOfProducts = aoProducts.length;

    for(var iIndexProduct = 0; iIndexProduct < iNumberOfProducts ; iIndexProduct++)
    {
        if( aoProducts[iIndexProduct].name === sName)
        {
            return aoProducts[iIndexProduct];
        }
    }
    return null;
};

function utilsProjectGetBandsFromSelectedProducts(asSelectedProducts,aoProducts)
{
    if( utilsIsObjectNullOrUndefined(asSelectedProducts) === true)
        return null;
    var iNumberOfSelectedProducts = asSelectedProducts.length;
    var asProductsBands=[];
    for(var iIndexSelectedProduct = 0; iIndexSelectedProduct < iNumberOfSelectedProducts; iIndexSelectedProduct++)
    {
        var oProduct = utilsProjectGetProductByName(asSelectedProducts[iIndexSelectedProduct],aoProducts);
        var iNumberOfBands;

        if(utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands) === true)
            iNumberOfBands = 0;
        else
            iNumberOfBands = oProduct.bandsGroups.bands.length;

        for(var iIndexBand = 0; iIndexBand < iNumberOfBands; iIndexBand++)
        {
            if( utilsIsObjectNullOrUndefined(oProduct.bandsGroups.bands[iIndexBand]) === false )
            {
                asProductsBands.push(oProduct.name + "_" + oProduct.bandsGroups.bands[iIndexBand].name);
            }
        }
    }
    return asProductsBands;
    // return ["test","secondo"];
};
