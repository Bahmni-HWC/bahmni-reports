package org.bahmni.reports.dao.impl;

import org.bahmni.reports.dao.GenericDao;
import org.bahmni.reports.model.GenericProgramReportConfig;
import org.bahmni.reports.model.Report;
import org.bahmni.reports.util.SqlUtil;
import org.stringtemplate.v4.ST;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.bahmni.reports.util.FileReaderUtil.getFileContent;
import static org.bahmni.reports.util.GenericProgramReportTemplateHelper.constructProgramAttributeNamesString;
import static org.bahmni.reports.util.GenericProgramReportTemplateHelper.constructProgramNamesString;
import static org.bahmni.reports.util.GenericProgramReportTemplateHelper.getDateRangeFor;
import static org.bahmni.reports.util.GenericReportsHelper.constructExtraPatientIdentifiersToFilter;
import static org.bahmni.reports.util.GenericReportsHelper.constructPatientAddressesToDisplay;
import static org.bahmni.reports.util.GenericReportsHelper.constructPatientAttributeNamesToDisplay;

public class GenericProgramDaoImpl implements GenericDao {

    private Report<GenericProgramReportConfig> report;

    public GenericProgramDaoImpl(Report<GenericProgramReportConfig> report) {
        this.report = report;
    }

    @Override
    public ResultSet getResultSet(Connection connection, String startDate, String endDate, List<String> conceptNamesToFilter) throws SQLException {
        String sql = getFileContent("sql/genericProgramReport.sql");
        ST sqlTemplate = new ST(sql, '#', '#');
        sqlTemplate.add("startDate", startDate);
        sqlTemplate.add("endDate", endDate);
        GenericProgramReportConfig config = report.getConfig();
        if (config != null) {
            sqlTemplate.add("patientAttributes", constructPatientAttributeNamesToDisplay(config));
            sqlTemplate.add("patientAddresses", constructPatientAddressesToDisplay(config));
            sqlTemplate.add("programAttributes", constructProgramAttributeNamesString(config.getProgramAttributes()));
            sqlTemplate.add("showAllStates", config.isShowAllStates());
            sqlTemplate.add("programNamesToFilterSql", constructProgramNamesString(config.getProgramNamesToFilter()));
            sqlTemplate.add("extraPatientIdentifierTypes", constructExtraPatientIdentifiersToFilter(config));
            sqlTemplate.add("ageGroupName", config.getAgeGroupName());

        }
        sqlTemplate.add("applyDateRangeFor", getDateRangeFor(config));
        return SqlUtil.executeSqlWithStoredProc(connection, sqlTemplate.render());
    }
}
