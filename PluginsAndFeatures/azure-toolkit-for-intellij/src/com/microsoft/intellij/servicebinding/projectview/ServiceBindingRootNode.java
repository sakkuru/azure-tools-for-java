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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.servicebinding.ServiceBindingInfo;
import com.microsoft.intellij.servicebinding.ServiceBindingManager;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceBindingRootNode extends ProjectViewNode<String> {
    private static boolean isFirstExpandListenerAdded = false;

    public ServiceBindingRootNode(Project project, ViewSettings settings) {
        super(project, "Connected Services", settings);

        if (!isFirstExpandListenerAdded) {
            final TreeModelListener firstExpandListener = new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent e) {
                }

                @Override
                public void treeNodesInserted(TreeModelEvent e) {
                    // expand when this root node have one child for the first time
                    if (e != null && e.getTreePath() != null && e.getChildIndices().length == 1 &&
                            e.getTreePath().getLastPathComponent() instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getTreePath().getLastPathComponent();
                        if (node.getUserObject() instanceof ServiceBindingRootNode) {
                            // TODO: use new style of invokeLater
                            ApplicationManager.getApplication().invokeLater(() -> {
                                ProjectView.getInstance(getProject()).getCurrentProjectViewPane().getTree().expandPath(e.getTreePath());
                            });

                        }
                    }

                }

                @Override
                public void treeNodesRemoved(TreeModelEvent e) {
                }

                @Override
                public void treeStructureChanged(TreeModelEvent e) {
                }
            };

            ProjectView.getInstance(getProject()).getCurrentProjectViewPane().getTree().getModel().addTreeModelListener(firstExpandListener);
            isFirstExpandListenerAdded = true;
        }
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        List<ServiceBindingInfo> list = ServiceBindingManager.getInstance(this.getProject()).getServiceBindings();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(bindingInfo -> new ServiceBindingNode(getProject(), getSettings(), bindingInfo)).collect(Collectors.toList());
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setPresentableText(getValue());

        // TODO: add icon from ui designer
        presentation.setIcon(PluginUtil.getIcon("/icons/azure.png"));
    }

    @Override
    public boolean expandOnDoubleClick() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public boolean canNavigate() {
        return true;
    }

    @Override
    public boolean contains(@NotNull VirtualFile virtualFile) {
        return false;
    }
}
