/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.migration.wfly10.config.task.update;

import org.jboss.migration.core.task.AbstractServerMigrationTask;
import org.jboss.migration.core.task.ParentServerMigrationTask;
import org.jboss.migration.core.task.ServerMigrationTask;
import org.jboss.migration.core.task.TaskContext;
import org.jboss.migration.core.task.ServerMigrationTaskName;
import org.jboss.migration.core.task.ServerMigrationTaskResult;
import org.jboss.migration.wfly10.config.management.DeploymentResources;
import org.jboss.migration.wfly10.config.management.HostControllerConfiguration;
import org.jboss.migration.wfly10.config.management.StandaloneServerConfiguration;
import org.jboss.migration.wfly10.config.task.executor.DeploymentsManagementSubtaskExecutor;
import org.jboss.migration.wfly10.config.task.executor.SubtaskExecutorAdapters;
import org.jboss.migration.wfly10.config.task.factory.DomainConfigurationTaskFactory;
import org.jboss.migration.wfly10.config.task.factory.StandaloneServerConfigurationTaskFactory;

/**
 * Removes deployments from configs.
 * @author emmartins
 */
public class RemoveDeployments<S> implements StandaloneServerConfigurationTaskFactory<S>, DomainConfigurationTaskFactory<S> {

    public static final RemoveDeployments INSTANCE  = new RemoveDeployments();

    private static final String TASK_NAME_NAME = "remove-deployments";
    private static final ServerMigrationTaskName TASK_NAME = new ServerMigrationTaskName.Builder(TASK_NAME_NAME).build();

    private RemoveDeployments() {
    }

    @Override
    public ServerMigrationTask getTask(S source, StandaloneServerConfiguration configuration) throws Exception {
        return getTask(source, configuration.getDeploymentResources());
    }

    @Override
    public ServerMigrationTask getTask(S source, HostControllerConfiguration configuration) throws Exception {
        return getTask(source, configuration.getDeploymentResources());
    }

    protected ServerMigrationTask getTask(S source, DeploymentResources deploymentResources) throws Exception {
        return new ParentServerMigrationTask.Builder(TASK_NAME)
                .subtask(SubtaskExecutorAdapters.of(source, deploymentResources, new SubtaskExecutor()))
                .listener(new AbstractServerMigrationTask.Listener() {
                    @Override
                    public void started(TaskContext context) {
                        context.getLogger().infof("Deployments removal starting...");
                    }
                    @Override
                    public void done(TaskContext context) {
                        context.getLogger().infof("Deployments removal done.");
                    }
                })
                .build();
    }

    public static class SubtaskExecutor<S> implements DeploymentsManagementSubtaskExecutor<S> {
        private static final String SUBTASK_NAME_NAME = "remove-deployment";
        @Override
        public void executeSubtasks(S source, final DeploymentResources deploymentResources, TaskContext context) throws Exception {
            for (final String resourceName : deploymentResources.getResourceNames()) {
                final ServerMigrationTaskName taskName = new ServerMigrationTaskName.Builder(SUBTASK_NAME_NAME)
                        .addAttribute("name", resourceName)
                        .build();
                final ServerMigrationTask subtask = new ServerMigrationTask() {
                    @Override
                    public ServerMigrationTaskName getName() {
                        return taskName;
                    }
                    @Override
                    public ServerMigrationTaskResult run(TaskContext context) throws Exception {
                        deploymentResources.removeResource(resourceName);
                        context.getLogger().infof("Removed deployment %s", resourceName);
                        return ServerMigrationTaskResult.SUCCESS;
                    }
                };
                context.execute(subtask);
            }
        }
    }
}