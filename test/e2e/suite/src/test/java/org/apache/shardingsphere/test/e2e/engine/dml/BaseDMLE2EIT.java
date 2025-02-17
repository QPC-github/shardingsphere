/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.test.e2e.engine.dml;

import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.util.expr.InlineExpressionParser;
import org.apache.shardingsphere.test.e2e.cases.dataset.metadata.DataSetColumn;
import org.apache.shardingsphere.test.e2e.cases.dataset.metadata.DataSetMetaData;
import org.apache.shardingsphere.test.e2e.cases.dataset.row.DataSetRow;
import org.apache.shardingsphere.test.e2e.engine.E2EContainerComposer;
import org.apache.shardingsphere.test.e2e.engine.SingleE2EIT;
import org.apache.shardingsphere.test.e2e.env.DataSetEnvironmentManager;
import org.apache.shardingsphere.test.e2e.env.runtime.scenario.path.ScenarioDataPath;
import org.apache.shardingsphere.test.e2e.env.runtime.scenario.path.ScenarioDataPath.Type;
import org.apache.shardingsphere.test.e2e.framework.database.DatabaseAssertionMetaData;
import org.apache.shardingsphere.test.e2e.framework.database.DatabaseAssertionMetaDataFactory;
import org.apache.shardingsphere.test.e2e.framework.param.model.AssertionTestParameter;
import org.junit.After;
import org.junit.Before;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class BaseDMLE2EIT extends SingleE2EIT {
    
    private static final String DATA_COLUMN_DELIMITER = ", ";
    
    private DataSetEnvironmentManager dataSetEnvironmentManager;
    
    public BaseDMLE2EIT(final AssertionTestParameter testParam) {
        super(testParam);
    }
    
    @Before
    public final void init() throws Exception {
        dataSetEnvironmentManager = new DataSetEnvironmentManager(new ScenarioDataPath(getTestParam().getScenario()).getDataSetFile(Type.ACTUAL), getContainerComposer().getActualDataSourceMap());
        dataSetEnvironmentManager.fillData();
    }
    
    @After
    public final void tearDown() {
        dataSetEnvironmentManager.cleanData();
    }
    
    protected final void assertDataSet(final int actualUpdateCount) throws SQLException {
        assertThat("Only support single table for DML.", getDataSet().getMetaDataList().size(), is(1));
        assertThat(actualUpdateCount, is(getDataSet().getUpdateCount()));
        DataSetMetaData expectedDataSetMetaData = getDataSet().getMetaDataList().get(0);
        for (String each : new InlineExpressionParser(expectedDataSetMetaData.getDataNodes()).splitAndEvaluate()) {
            DataNode dataNode = new DataNode(each);
            DataSource dataSource = getContainerComposer().getActualDataSourceMap().get(dataNode.getDataSourceName());
            try (
                    Connection connection = dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(generateFetchActualDataSQL(dataNode))) {
                assertDataSet(preparedStatement, expectedDataSetMetaData, getDataSet().findRows(dataNode));
            }
        }
    }
    
    private void assertDataSet(final PreparedStatement actualPreparedStatement, final DataSetMetaData expectedDataSetMetaData, final List<DataSetRow> expectedDataSetRows) throws SQLException {
        try (ResultSet actualResultSet = actualPreparedStatement.executeQuery()) {
            assertMetaData(actualResultSet.getMetaData(), expectedDataSetMetaData.getColumns());
            assertRows(actualResultSet, expectedDataSetRows);
        }
    }
    
    private String generateFetchActualDataSQL(final DataNode dataNode) throws SQLException {
        Optional<DatabaseAssertionMetaData> databaseAssertionMetaData = DatabaseAssertionMetaDataFactory.newInstance(getTestParam().getDatabaseType());
        if (databaseAssertionMetaData.isPresent()) {
            String primaryKeyColumnName = databaseAssertionMetaData.get().getPrimaryKeyColumnName(
                    getContainerComposer().getActualDataSourceMap().get(dataNode.getDataSourceName()), dataNode.getTableName());
            return String.format("SELECT * FROM %s ORDER BY %s ASC", dataNode.getTableName(), primaryKeyColumnName);
        }
        return String.format("SELECT * FROM %s", dataNode.getTableName());
    }
    
    private void assertMetaData(final ResultSetMetaData actual, final Collection<DataSetColumn> expected) throws SQLException {
        assertThat(actual.getColumnCount(), is(expected.size()));
        int index = 1;
        for (DataSetColumn each : expected) {
            assertThat(actual.getColumnLabel(index++), is(each.getName()));
        }
    }
    
    private void assertRows(final ResultSet actual, final List<DataSetRow> expected) throws SQLException {
        int rowCount = 0;
        while (actual.next()) {
            int columnIndex = 1;
            for (String each : expected.get(rowCount).splitValues(DATA_COLUMN_DELIMITER)) {
                assertValue(actual, columnIndex, each);
                columnIndex++;
            }
            rowCount++;
        }
        assertThat("Size of actual result set is different with size of expected dat set rows.", rowCount, is(expected.size()));
    }
    
    private void assertValue(final ResultSet actual, final int columnIndex, final String expected) throws SQLException {
        if (Types.DATE == actual.getMetaData().getColumnType(columnIndex)) {
            if (!E2EContainerComposer.NOT_VERIFY_FLAG.equals(expected)) {
                assertThat(new SimpleDateFormat("yyyy-MM-dd").format(actual.getDate(columnIndex)), is(expected));
            }
        } else if (Types.CHAR == actual.getMetaData().getColumnType(columnIndex)
                && ("PostgreSQL".equals(getTestParam().getDatabaseType().getType()) || "openGauss".equals(getTestParam().getDatabaseType().getType()))) {
            assertThat(String.valueOf(actual.getObject(columnIndex)).trim(), is(expected));
        } else if (isPostgreSQLOrOpenGaussMoney(actual.getMetaData().getColumnTypeName(columnIndex))) {
            assertThat(actual.getString(columnIndex), is(expected));
        } else if (Types.BINARY == actual.getMetaData().getColumnType(columnIndex)) {
            assertThat(actual.getObject(columnIndex), is(expected.getBytes(StandardCharsets.UTF_8)));
        } else {
            assertThat(String.valueOf(actual.getObject(columnIndex)), is(expected));
        }
    }
    
    private boolean isPostgreSQLOrOpenGaussMoney(final String columnTypeName) {
        return "money".equalsIgnoreCase(columnTypeName) && ("PostgreSQL".equals(getTestParam().getDatabaseType().getType()) || "openGauss".equals(getTestParam().getDatabaseType().getType()));
    }
    
    protected void assertGeneratedKeys(final ResultSet generatedKeys) throws SQLException {
        if (null == getGeneratedKeyDataSet()) {
            return;
        }
        assertThat("Only support single table for DML.", getGeneratedKeyDataSet().getMetaDataList().size(), is(1));
        assertMetaData(generatedKeys.getMetaData(), getGeneratedKeyDataSet().getMetaDataList().get(0).getColumns());
        assertRows(generatedKeys, getGeneratedKeyDataSet().getRows());
    }
}
