/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.util;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTasksProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MavenRunTaskUtil {

    private static final String MAVEN_TASK_PACKAGE = "package";

    /**
     * Add Maven package goal into the run configuration's before run task.
     */
    public static void addMavenPackageBeforeRunTask(RunConfiguration runConfiguration) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(runConfiguration.getProject());
        if (MavenUtils.isMavenProject(runConfiguration.getProject())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(runConfiguration));
            if (MavenRunTaskUtil.shouldAddMavenPackageTask(tasks, runConfiguration.getProject())) {
                MavenBeforeRunTask task = new MavenBeforeRunTask();
                task.setEnabled(true);
                task.setProjectPath(runConfiguration.getProject().getBasePath() + File.separator
                        + MavenConstants.POM_XML);
                task.setGoal(MAVEN_TASK_PACKAGE);
                tasks.add(task);
                manager.setBeforeRunTasks(runConfiguration, tasks);
            }
        }
    }

    @NotNull
    public static List<Artifact> collectProjectArtifact(@NotNull Project project) {
        return Arrays.asList(MavenConstants.TYPE_WAR, "ear", MavenConstants.TYPE_JAR).stream()
              .map(ArtifactType::findById)
              .filter(Objects::nonNull)
              .flatMap(type -> ArtifactManager.getInstance(project).getArtifactsByType(type).stream()).collect(
                Collectors.toList());

    }

    public static String getTargetPath(MavenProject mavenProject) {
        return (mavenProject == null) ? null : new File(mavenProject.getBuildDirectory()).getPath() + File.separator
                + mavenProject.getFinalName() + "." + mavenProject.getPackaging();
    }

    public static String getTargetName(MavenProject mavenProject) {
        return (mavenProject == null) ? null : mavenProject.getFinalName() + "." + mavenProject.getPackaging();

    }

    /**
     * Legacy code, will be replaced by BeforeRunTaskUtils
     * @deprecated
     */
    private static boolean shouldAddMavenPackageTask(List<BeforeRunTask> tasks, Project project) {
        boolean shouldAdd = true;
        for (BeforeRunTask task : tasks) {
            if (task.getProviderId().equals(MavenBeforeRunTasksProvider.ID)) {
                MavenBeforeRunTask mavenTask = (MavenBeforeRunTask) task;
                if (mavenTask.getGoal().contains(MAVEN_TASK_PACKAGE) && Comparing.equal(mavenTask.getProjectPath(),
                        project.getBasePath() + File.separator + MavenConstants.POM_XML)) {
                    mavenTask.setEnabled(true);
                    shouldAdd = false;
                    break;
                }
            }
        }
        return shouldAdd;
    }

    public static String getFileType(@NotNull final String fileName) {
        String fileType = "";
        int index = fileName.lastIndexOf(".");
        if (index >= 0 && (index + 1) < fileName.length()) {
            fileType = fileName.substring(index + 1);
        }
        return fileType;
    }
}
