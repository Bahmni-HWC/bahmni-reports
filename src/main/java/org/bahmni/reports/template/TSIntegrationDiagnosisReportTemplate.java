package org.bahmni.reports.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.constant.HorizontalAlignment;
import net.sf.dynamicreports.report.constant.PageType;
import net.sf.dynamicreports.report.constant.WhenNoDataType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.reports.model.Report;
import org.bahmni.reports.model.TSIntegrationDiagnosisReportConfig;
import org.bahmni.reports.model.TSPageObject;
import org.bahmni.reports.model.UsingDatasource;
import org.bahmni.reports.report.BahmniReportBuilder;
import org.bahmni.reports.util.CommonComponents;
import org.bahmni.webclients.HttpClient;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;

import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;
import static org.bahmni.reports.template.Templates.minimalColumnStyle;
import static org.bahmni.reports.util.FileReaderUtil.getFileContent;


@UsingDatasource("openmrs")
public class TSIntegrationDiagnosisReportTemplate extends BaseReportTemplate<TSIntegrationDiagnosisReportConfig> {
    public static final String CREATE_SQL_TEMPLATE = "create temporary table {0}(code varchar(100) not null)";
    public static final String INSERT_SQL_TEMPLATE = "insert into {0} values (?)";
    public static final String DIAGNOSIS_COLUMN_NAME = "Diagnosis";
    public static final String TERMINOLOGY_COLUMN_NAME = "Terminology Code";
    public static final String FEMALE_COLUMN_NAME = "Female";
    public static final String MALE_COLUMN_NAME = "Male";
    public static final String OTHER_COLUMN_NAME = "Other";
    public static final String NOT_DISCLOSED_COLUMN_NAME = "Not disclosed";
    public static final String TOTAL_COLUMN_NAME = "Total";
    public static final int TS_DIAGNOSIS_LOOKUP_DEFAULT_PAGE_SIZE = 20;
    private static final Logger logger = LogManager.getLogger(TSIntegrationDiagnosisReportTemplate.class);
    private HttpClient httpClient;
    private Properties tsProperties;
    private String descendantsUrlTemplate;

    public TSIntegrationDiagnosisReportTemplate(HttpClient httpClient, Properties tsProperties, String descendantsUrlTemplate) {
        super();
        this.httpClient = httpClient;
        this.tsProperties = tsProperties;
        this.descendantsUrlTemplate = descendantsUrlTemplate;
    }

    public void setDescendantsUrlTemplate(String descendantsUrlTemplate) {
        this.descendantsUrlTemplate = descendantsUrlTemplate;
    }

    @Override
    public BahmniReportBuilder build(Connection connection, JasperReportBuilder jasperReport, Report<TSIntegrationDiagnosisReportConfig> report, String startDate, String endDate, List<AutoCloseable> resources, PageType pageType) {
        String tempTableName = "tmpCodes_" + System.nanoTime();
        loadTempTable(connection, tempTableName, report.getConfig().getTerminologyParentCode());
        String sql = getFileContent("sql/tsIntegrationDiagnosisCount.sql");

        CommonComponents.addTo(jasperReport, report, pageType);
        jasperReport.addColumn(col.column(DIAGNOSIS_COLUMN_NAME, DIAGNOSIS_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
        if (report.getConfig().isDisplayTerminologyCode()) {
            String terminologyConfigColumnName = report.getConfig().getTerminologyColumnName();
            String terminologyColumnName = StringUtils.isNotBlank(terminologyConfigColumnName) ? terminologyConfigColumnName : TERMINOLOGY_COLUMN_NAME;
            jasperReport.addColumn(col.column(terminologyColumnName, TERMINOLOGY_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
        }
        if (report.getConfig().isDisplayGenderGroup()) {
            jasperReport.addColumn(col.column(FEMALE_COLUMN_NAME, FEMALE_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
            jasperReport.addColumn(col.column(MALE_COLUMN_NAME, MALE_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
            jasperReport.addColumn(col.column(OTHER_COLUMN_NAME, OTHER_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
            jasperReport.addColumn(col.column(NOT_DISCLOSED_COLUMN_NAME, NOT_DISCLOSED_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
        }
        jasperReport.addColumn(col.column(TOTAL_COLUMN_NAME, TOTAL_COLUMN_NAME, type.stringType()).setStyle(minimalColumnStyle).setHorizontalAlignment(HorizontalAlignment.CENTER));
        String formattedSql = getFormattedSql(sql, report.getConfig().getTsConceptSource(), startDate, endDate, tempTableName);
        jasperReport.setShowColumnTitle(true).setWhenNoDataType(WhenNoDataType.ALL_SECTIONS_NO_DETAIL).setDataSource(formattedSql, connection);

        return new BahmniReportBuilder(jasperReport);
    }

    private void loadTempTable(Connection connection, String tempTableName, String parentCode) {
        int offset = 0;
        int pageSize = getDefaultPageSize();
        try {
            String createSqlStmt = MessageFormat.format(CREATE_SQL_TEMPLATE, tempTableName);
            String insertSqlStmt = MessageFormat.format(INSERT_SQL_TEMPLATE, tempTableName);
            Statement statement = connection.createStatement();
            statement.execute(createSqlStmt);
            PreparedStatement pstmtInsert = connection.prepareStatement(insertSqlStmt);

            TSPageObject pageObject = null;
            do {
                try {
                    pageObject = fetchDescendantsByPagination(parentCode, pageSize, offset, "en");
                } catch (IOException e) {
                    throw new RuntimeException();
                }
                List<String> codes = pageObject.getCodes();
                for (int batchcount = 0; batchcount < codes.size(); batchcount++) {
                    pstmtInsert.setString(1, codes.get(batchcount));
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
                offset += pageSize;
            } while (offset < pageObject.getTotal());
        } catch (SQLException e) {
            logger.error("Error occured while making database call to " + tempTableName + " table");
            throw new RuntimeException();
        }
    }

    private TSPageObject fetchDescendantsByPagination(String terminologyCode, int pageSize, int offset, String localeLanguage) throws IOException {
        String url = MessageFormat.format(descendantsUrlTemplate, terminologyCode, pageSize, offset, localeLanguage);
        String responseStr = httpClient.get(URI.create(url));
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(responseStr, TSPageObject.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }


    private String getFormattedSql(String templateSql, String conceptSourceCode, String startDate, String endDate, String tempTableName) {
        ST sqlTemplate = new ST(templateSql, '#', '#');
        sqlTemplate.add("conceptSourceCode", conceptSourceCode);
        sqlTemplate.add("startDate", startDate);
        sqlTemplate.add("endDate", endDate);
        sqlTemplate.add("tempTable", tempTableName);
        return sqlTemplate.render();
    }


    public int getDefaultPageSize() {
        String pageSize = System.getenv("REPORTS_TS_PAGE_SIZE");
        if (pageSize == null) pageSize = tsProperties.getProperty("ts.defaultPageSize");
        if (pageSize != null) {
            return Integer.parseInt(pageSize);
        }
        return TS_DIAGNOSIS_LOOKUP_DEFAULT_PAGE_SIZE;
    }

}