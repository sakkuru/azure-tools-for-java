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
package com.microsoft.intellij.servicebinding.projectview;

import com.intellij.ide.DeleteProvider;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.servicebinding.ServiceBindingInfo;
import com.microsoft.intellij.servicebinding.ServiceBindingManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServiceBindingTreeStructureProvider implements TreeStructureProvider {
    private Project project;

    public ServiceBindingTreeStructureProvider(Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                  @NotNull Collection<AbstractTreeNode<?>> children,
                                                  ViewSettings settings) {
        Project myProject = parent instanceof ProjectViewProjectNode ? parent.getProject() : null;

        if (myProject == null || ApplicationManager.getApplication().isUnitTestMode() || children.isEmpty()) {
            return children;
        }
        List<AbstractTreeNode<?>> list = new ArrayList<>(children.size() + 1);
        list.addAll(children);
        list.add(new ServiceBindingRootNode(myProject, settings));
        return list;
    }

    @Nullable
    @Override
    public Object getData(@NotNull Collection<AbstractTreeNode<?>> selected, @NotNull String dataId) {
        if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
            return getDeleteModelProvider(selected);
        }
        return null;
    }

    private DeleteProvider getDeleteModelProvider(Collection<AbstractTreeNode<?>> selectedToDelete) {
        final List<ServiceBindingInfo> bindings = new ArrayList<>();
        for (AbstractTreeNode treeNode : selectedToDelete) {
            if (!(treeNode instanceof ServiceBindingNode)) {
                return null;
            }
            ServiceBindingInfo psiModel = ((ServiceBindingNode) treeNode).getValue();
            bindings.add(psiModel);
        }

        return new ServerBindingDeleteProvider(bindings);
    }

    class ServerBindingDeleteProvider implements DeleteProvider {
        private static final String DELETE_CONFIRM = "Delete service binding?";
        private List<ServiceBindingInfo> toDelete;

        ServerBindingDeleteProvider(List<ServiceBindingInfo> toDelete) {
            this.toDelete = toDelete;
        }

        @Override
        public void deleteElement(@NotNull DataContext dataContext) {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            dialogButton = JOptionPane.showConfirmDialog(ProjectView.getInstance(project).getCurrentProjectViewPane().getTree(),
                                                          DELETE_CONFIRM, "Confirm", dialogButton);
            if (dialogButton == JOptionPane.YES_OPTION) {
                ServiceBindingManager.getInstance(project).deleteServiceBindings(toDelete);
                ProjectView.getInstance(project).refresh();
            }
        }

        @Override
        public boolean canDeleteElement(@NotNull DataContext dataContext) {
            return true;
        }
    }
}
