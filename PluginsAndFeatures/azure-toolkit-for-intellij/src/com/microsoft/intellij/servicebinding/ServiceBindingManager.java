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

package com.microsoft.intellij.servicebinding;


import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServiceBindingManager {
    private Project project;

    private static Map<Project, ServiceBindingManager> map = new ConcurrentHashMap<>();

    public static ServiceBindingManager getInstance(@NotNull Project project) {
        return map.computeIfAbsent(project, (key) -> new ServiceBindingManager(project));
    }

    public List<ServiceBindingInfo> getServiceBindings() {
        ServiceBindingState state = ServiceManager.getService(project, ServiceBindingState.class).getState();
        return state.getServiceBindingInfos();
    }

    public void addServiceBinding(ServiceBindingInfo binding) {
        ServiceBindingState state = ServiceManager.getService(project, ServiceBindingState.class).getState();
        state.addBindingInfo(binding);

        ProjectView.getInstance(ProjectManager.getInstance().getDefaultProject()).refresh();
    }

    public void deleteServiceBindings(Collection<ServiceBindingInfo> toDeleteList) {
        if (CollectionUtils.isEmpty(toDeleteList)) {
            return;
        }
        ServiceBindingState state = ServiceManager.getService(project, ServiceBindingState.class).getState();
        List<ServiceBindingInfo> existingBindings = state.getServiceBindingInfos();
        if (CollectionUtils.isEmpty(existingBindings)) {
            return;
        }
        List<String> idsToDelete = toDeleteList.stream().map(ServiceBindingInfo::getId).collect(Collectors.toList());
        Collection<String> idsToRemove = new ArrayList<>();
        for (ServiceBindingInfo bindingInfo : existingBindings) {
            if (idsToDelete.contains(bindingInfo.getId())) {
                idsToRemove.add(bindingInfo.getId());
            }
        }
        if (CollectionUtils.isEmpty(idsToRemove)) {
            return;
        }
        state.removeByIds(idsToRemove);
        ProjectView.getInstance(ProjectManager.getInstance().getDefaultProject()).refresh();
    }

    private ServiceBindingManager(Project project) {
        this.project = project;
    }
}
