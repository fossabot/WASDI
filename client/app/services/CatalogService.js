

'use strict';
angular.module('wasdi.CatalogService', ['wasdi.CatalogService']).
service('CatalogService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;

    this.getCategories = function()
    {
        return this.m_oHttp.get(this.APIURL + "/catalog/categories");
    };
    this.getEntries = function(sFrom,sTo,sFreeText,sCategory)
    {
          return this.m_oHttp.get(this.APIURL + "/catalog/entries?from=" + sFrom + "&to=" + sTo + "&freetext=" + sFreeText + "&category=" + sCategory);
         // return this.m_oHttp.get(this.APIURL + "/catalog/entries?from=" + sFrom + "&to=" + sTo + "&category=" + sCategory);
         // return this.m_oHttp.get(this.APIURL + "/catalog/entries");
    };

    this.downloadEntry = function(oEntry)
    {
          return this.m_oHttp.post(this.APIURL + "/catalog/downloadentry",oEntry,{responseType: 'arraybuffer'});
    };
    this.downloadByName = function(sFileName)
    {
          return this.m_oHttp.get(this.APIURL + "/catalog/downloadbyname?filename=" + sFileName);
    };

    this.ingestFile = function(sSelectedFile,sWorkspace){
        return this.m_oHttp.put(this.APIURL + '/catalog/upload/ingest?file=' + sSelectedFile + '&workspace=' + sWorkspace);
    };
    this.uploadFTPFile = function(oFtpTransferFile,sWorkspaceId){

        return this.m_oHttp.put(this.APIURL + '/catalog/upload/ftp?workspace='+sWorkspaceId,oFtpTransferFile);
    };
}]);

