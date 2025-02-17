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

package org.apache.shardingsphere.test.e2e.engine.ral;

import org.apache.shardingsphere.test.e2e.cases.SQLCommandType;
import org.apache.shardingsphere.test.e2e.framework.param.array.E2ETestParameterFactory;
import org.apache.shardingsphere.test.e2e.framework.param.model.AssertionTestParameter;
import org.apache.shardingsphere.test.e2e.framework.runner.ParallelRunningStrategy;
import org.apache.shardingsphere.test.e2e.framework.runner.ParallelRunningStrategy.ParallelLevel;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.Collection;

@ParallelRunningStrategy(ParallelLevel.SCENARIO)
public final class GeneralRALE2EIT extends BaseRALE2EIT {
    
    public GeneralRALE2EIT(final AssertionTestParameter testParam) {
        super(testParam);
    }
    
    @Parameters(name = "{0}")
    public static Collection<AssertionTestParameter> getTestParameters() {
        return E2ETestParameterFactory.getAssertionTestParameters(SQLCommandType.RAL);
    }
    
    @Test
    public void assertExecute() throws SQLException, ParseException {
        try (Connection connection = getContainerComposer().getTargetDataSource().getConnection()) {
            try (
                    Statement statement = connection.createStatement()) {
                assertResultSet(statement);
            }
        }
    }
    
    private void assertResultSet(final Statement statement) throws SQLException, ParseException {
        if (null == getAssertion().getAssertionSQL()) {
            assertResultSet(statement, getSQL());
        } else {
            statement.execute(getSQL());
            sleep(2000);
            assertResultSet(statement, getAssertion().getAssertionSQL().getSql());
        }
    }
    
    private void assertResultSet(final Statement statement, final String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertResultSet(resultSet);
        }
    }
}
