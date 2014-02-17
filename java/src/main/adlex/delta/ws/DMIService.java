/**
 * DMIService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package adlex.delta.ws;

public interface DMIService extends java.rmi.Remote {
    public boolean setProperty(java.lang.String propertyName, java.lang.String propertyValue) throws java.rmi.RemoteException;
    public java.lang.String getProperty(java.lang.String propertyName, java.lang.String defValue) throws java.rmi.RemoteException;
    public java.lang.String[][] getProperties(java.lang.String appId, java.lang.String viewId, java.lang.String resolution, java.lang.String idFilter, java.lang.String propFilter) throws java.rmi.RemoteException;
    public java.lang.String getServerUUID() throws java.rmi.RemoteException;
    public java.lang.String[][] getApplications() throws java.rmi.RemoteException;
    public java.lang.String[][] getMetrics(java.lang.String appId, java.lang.String viewId, java.lang.String resolution) throws java.rmi.RemoteException;
    public java.lang.String[][] getDimensions(java.lang.String appId, java.lang.String viewId, java.lang.String resolution) throws java.rmi.RemoteException;
    public void registerPropertyProvider(java.lang.Object ifc) throws java.rmi.RemoteException;
    public java.lang.String[][] getDataViews(java.lang.String appId) throws java.rmi.RemoteException;
    public java.lang.String[][] getDataSources(java.lang.String appId, java.lang.String viewId) throws java.rmi.RemoteException;
    public java.lang.String[] getDimensionValues(java.lang.String appId, java.lang.String viewId, java.lang.String dimId, java.lang.String dataSourceId, java.lang.Integer quant, java.lang.String filter) throws java.rmi.RemoteException;
    public java.lang.String[] getResolutions(java.lang.String appId, java.lang.String viewId) throws java.rmi.RemoteException;
    public long getLastSampleTime(java.lang.String appId, java.lang.String viewId, java.lang.String resolution) throws java.rmi.RemoteException;
    public long getCurrentTime(java.lang.String appId, java.lang.String viewId, java.lang.String resolution) throws java.rmi.RemoteException;
    public adlex.delta.ws.DMIData getDMIData(java.lang.String appId, java.lang.String viewId, java.lang.String dataSourceId, java.lang.String[] dimensionIds, java.lang.String[] metricIds, java.lang.String[][] dimFilters, java.lang.String[][] metricFilters, java.lang.String[][] sort, java.lang.Integer top, java.lang.String resolution, java.lang.String timePeriod, java.lang.Integer numberOfPeriods, java.lang.Long timeBegin, java.lang.Long timeEnd, java.lang.Long timeout) throws java.rmi.RemoteException;
    public adlex.delta.ws.DMIData2 getDMIData2(java.lang.String appId, java.lang.String viewId, java.lang.String dataSourceId, java.lang.String[] dimensionIds, java.lang.String[] metricIds, java.lang.String[][] dimFilters, java.lang.String[][] metricFilters, java.lang.String[][] sort, java.lang.Integer top, java.lang.String resolution, java.lang.String timePeriod, java.lang.Integer numberOfPeriods, java.lang.Long timeBegin, java.lang.Long timeEnd, java.lang.Long timeout) throws java.rmi.RemoteException;
    public adlex.delta.ws.DMIData3 getDMIData3(java.lang.String appId, java.lang.String viewId, java.lang.String dataSourceId, java.lang.String[] dimensionIds, java.lang.String[] metricIds, java.lang.String[][] dimFilters, java.lang.String[][] metricFilters, java.lang.String[][] sort, java.lang.Integer top, java.lang.String resolution, java.lang.String timePeriod, java.lang.Integer numberOfPeriods, java.lang.Long timeBegin, java.lang.Long timeEnd, java.lang.Long timeout) throws java.rmi.RemoteException;
    public java.lang.String saveReportDefinition(java.lang.String appId, java.lang.String viewId, java.lang.String dataSourceId, java.lang.String[] dimensionIds, java.lang.String[] metricIds, java.lang.String[][] dimFilters, java.lang.String[][] metricFilters, java.lang.String[][] sort, java.lang.Integer top, java.lang.String resolution, java.lang.String timePeriod, java.lang.Integer numberOfPeriods, java.lang.Long timeBegin, java.lang.Long timeEnd) throws java.rmi.RemoteException;
    public void setSaveReportLimit(int number) throws java.rmi.RemoteException;
    public int getSaveReportLimit() throws java.rmi.RemoteException;
    public int getSaveReportNumber() throws java.rmi.RemoteException;
    public java.lang.String[] getI18N(java.lang.String lang, java.lang.String[] totranslate) throws java.rmi.RemoteException;
}
