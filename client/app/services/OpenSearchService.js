/**
 * Created by s.adamo on 15/12/2016.
 */


'use strict';
angular.module('wasdi.OpenSearchService', ['wasdi.OpenSearchService']).
service('OpenSearchService', ['$http',  'ConstantsService', function ($http, oConstantsService) {
    this.APIURL = oConstantsService.getAPIURL();
    this.m_oHttp = $http;
    //----------API------------------------
    this.API_GET_PRODUCTS_COUNT = "/search/sentinel/count?sQuery=";
    this.API_GET_PRODUCTS = "/search/sentinel/result?sQuery=";
    this.API_GET_PROVIDERS = "/search/providers";
    this.API_GET_SEARCH = "/search/query?sQuery=";
    this.API_GET_SEARCHLIST = "/search/querylist?sQuery=";

    //-------------------------------------

    this.getApiProductsCount = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS_COUNT + query;
    };

    this.getApiProducts = function (query) {
        return this.APIURL + this.API_GET_PRODUCTS + query;
    };

    this.getApiProductsWithProviders = function (query) {
        return this.APIURL + this.API_GET_SEARCH + query;
    };

    this.getApiProductsListWithProviders = function (query) {
        return this.APIURL + this.API_GET_SEARCHLIST + query;
    };

    this.getApiProductCountWithProviders = function(sQueryInput,sProvidersInput)
    {
        return this.APIURL + '/search/query/count?sQuery=' + sQueryInput +"&providers="+sProvidersInput;
    };

    this.getApiProductListCountWithProviders = function(sQueryInput,sProvidersInput)
    {
        return this.APIURL + '/search/query/countlist?sQuery=' + sQueryInput +"&providers="+sProvidersInput;
    };

    this.getListOfProvider = function()
    {
        return this.m_oHttp.get(this.APIURL + this.API_GET_PROVIDERS);
    }


}]);

