package org.bahmni.reports.web;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.exception.DRException;
import org.apache.log4j.Logger;
import org.bahmni.reports.BahmniReportsProperties;
import org.bahmni.reports.filter.JasperResponseConverter;
import org.bahmni.reports.model.ReportConfig;
import org.bahmni.reports.template.BaseReportTemplate;
import org.bahmni.reports.template.ReportTemplates;
import org.bahmni.reports.util.ConfigReaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;


@Controller
public class MainReportController {

    private static final Logger logger = Logger.getLogger(MainReportController.class);
    private ReportTemplates reportTemplates;
    private JasperResponseConverter converter;
    private BahmniReportsProperties bahmniReportsProperties;

    @Autowired
    public MainReportController(ReportTemplates reportTemplates, JasperResponseConverter converter, BahmniReportsProperties bahmniReportsProperties) {
        this.reportTemplates = reportTemplates;
        this.converter = converter;
        this.bahmniReportsProperties = bahmniReportsProperties;
    }

    //TODO: Better way to handle the response.
    @RequestMapping(value = "/report", method = RequestMethod.GET)
    public void getReport(HttpServletRequest request, HttpServletResponse response) {
        Connection connection = null;
        try {
            String startDate = request.getParameter("startDate");
            String endDate = request.getParameter("endDate");
            String reportName = request.getParameter("name");
            String responseType = request.getParameter("responseType");

            ReportConfig reportConfig = new ConfigReaderUtil().findConfig(reportName, bahmniReportsProperties.getConfigFilePath());

            BaseReportTemplate baseReportTemplate = reportTemplates.get(reportConfig.getType());
            connection = baseReportTemplate.getDataSource().getConnection();
            JasperReportBuilder reportBuilder = baseReportTemplate.build(connection, reportConfig, startDate, endDate);

            convertToResponse(responseType, reportBuilder, response, reportConfig.getName());
        } catch (SQLException | IOException | DRException e) {
            logger.error("Error running report", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                response.flushBuffer();
                response.getOutputStream().close();
            } catch (IOException e) {
                logger.error(e);
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Could not close connection.", e);
                }
            }
        }
    }


    private void convertToResponse(String responseType, JasperReportBuilder reportBuilder, HttpServletResponse response, String fileName) throws SQLException {
        try {
            converter.convert(responseType, reportBuilder, response, fileName);
        } catch (DRException | IOException e) {
            logger.error("Could not convert response", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}