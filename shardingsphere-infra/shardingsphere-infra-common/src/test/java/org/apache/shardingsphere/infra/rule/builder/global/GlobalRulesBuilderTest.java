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

package org.apache.shardingsphere.infra.rule.builder.global;

import lombok.SneakyThrows;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.database.type.dialect.MySQLDatabaseType;
import org.apache.shardingsphere.infra.instance.ComputeNodeInstance;
import org.apache.shardingsphere.infra.instance.InstanceContext;
import org.apache.shardingsphere.infra.instance.metadata.jdbc.JDBCInstanceMetaData;
import org.apache.shardingsphere.infra.lock.LockContext;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.infra.rule.builder.fixture.FixtureGlobalRule;
import org.apache.shardingsphere.infra.rule.builder.fixture.FixtureGlobalRuleConfiguration;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public final class GlobalRulesBuilderTest {
    
    @Test
    public void assertBuildRules() {
        Collection<ShardingSphereRule> shardingSphereRules = GlobalRulesBuilder
                .buildRules(Collections.singletonList(new FixtureGlobalRuleConfiguration()), Collections.singletonMap("logic_db", buildShardingSphereDatabase()), buildInstanceContext());
        assertThat(shardingSphereRules.size(), is(1));
    }
    
    @Test
    public void assertBuildRulesClassType() {
        Collection<ShardingSphereRule> shardingSphereRules = GlobalRulesBuilder
                .buildRules(Collections.singletonList(new FixtureGlobalRuleConfiguration()), Collections.singletonMap("logic_db", buildShardingSphereDatabase()), buildInstanceContext());
        assertTrue(shardingSphereRules.toArray()[0] instanceof FixtureGlobalRule);
    }
    
    private InstanceContext buildInstanceContext() {
        ComputeNodeInstance computeNodeInstance = new ComputeNodeInstance(new JDBCInstanceMetaData(UUID.randomUUID().toString()));
        ModeConfiguration modeConfiguration = new ModeConfiguration("Standalone", null, false);
        InstanceContext instanceContext = new InstanceContext(computeNodeInstance, () -> 0, modeConfiguration, mock(LockContext.class));
        return instanceContext;
    }
    
    @SneakyThrows
    private ShardingSphereDatabase buildShardingSphereDatabase() {
        ShardingSphereDatabase shardingSphereDatabase = ShardingSphereDatabase.create("logic_db", new MySQLDatabaseType());
        return shardingSphereDatabase;
    }
}
