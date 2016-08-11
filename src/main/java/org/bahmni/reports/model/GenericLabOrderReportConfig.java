package org.bahmni.reports.model;

import java.util.List;

import static org.bahmni.reports.util.BahmniReportUtil.emptyListIfNull;

public class GenericLabOrderReportConfig extends GenericReportsConfig implements Config {
    private Boolean showProvider = false;
    private boolean showVisitInfo = false;
    private List<String> programsToFilter;
    private List<String> conceptNamesToFilter;
    private List<String> conceptValuesToFilter;
    private boolean showOrderDateTime;

    public Boolean showProvider() {
        return showProvider;
    }

    public void setShowProvider(Boolean showProvider) {
        this.showProvider = showProvider;
    }

    public boolean showVisitInfo() {
        return showVisitInfo;
    }

    public void setShowVisitInfo(boolean showVisitInfo) {
        this.showVisitInfo = showVisitInfo;
    }

    public List<String> getProgramsToFilter() {
        return emptyListIfNull(programsToFilter);
    }

    public void setProgramsToFilter(List<String> programsToFilter) {
        this.programsToFilter = programsToFilter;
    }

    public List<String> getConceptNamesToFilter() {
        return conceptNamesToFilter;
    }

    public void setConceptNamesToFilter(List<String> conceptNamesToFilter) {
        this.conceptNamesToFilter = conceptNamesToFilter;
    }

    public List<String> getConceptValuesToFilter() {
        return conceptValuesToFilter;
    }

    public void setConceptValuesToFilter(List<String> conceptValuesToFilter) {
        this.conceptValuesToFilter = conceptValuesToFilter;
    }

    public boolean isShowOrderDateTime() {
        return showOrderDateTime;
    }

    public void setShowOrderDateTime(boolean showOrderDateTime) {
        this.showOrderDateTime = showOrderDateTime;
    }
}