package org.bahmni.reports.report;

import org.bahmni.reports.BahmniReportsProperties;
import org.bahmni.reports.model.SnomedDiagnosisReportConfig;
import org.bahmni.reports.template.SnomedDiagnosisReportTemplate;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class SnomedDiagnosisReportUnitTest {

    @Test
    public void shouldPickupSnomedDiagnosisReportTemplateWhenSnomedDiagnosisReportTypeIsInvoked() {
        SnomedDiagnosisReportConfig snomedDiagnosisReportConfig = new SnomedDiagnosisReportConfig();
        SnomedDiagnosisReport snomedDiagnosisReport = new SnomedDiagnosisReport();
        snomedDiagnosisReport.setConfig(snomedDiagnosisReportConfig);
        assertTrue(snomedDiagnosisReport.getTemplate(new BahmniReportsProperties()).getClass().isAssignableFrom(SnomedDiagnosisReportTemplate.class));
    }
}