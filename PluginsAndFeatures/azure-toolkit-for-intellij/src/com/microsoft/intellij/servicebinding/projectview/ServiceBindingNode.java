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

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.servicebinding.ServiceBindingInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class ServiceBindingNode extends ProjectViewNode<ServiceBindingInfo> {
    ServiceBindingNode(Project project, @NotNull ViewSettings settings, ServiceBindingInfo bindingInfo) {
        super(project, bindingInfo, settings);
    }

    @Override
    public boolean contains(@NotNull VirtualFile virtualFile) {
        return false;
    }

    @NotNull
    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        presentation.setIcon(AllIcons.Nodes.AbstractException);
        // TODO: show the reasonable text at TreeNode
        presentation.setPresentableText(this.getValue().getName());
    }

    @Override
    public boolean expandOnDoubleClick() {
        return false;
    }

    public ServiceBindingInfo getModel() {
        return this.getValue();
    }
}
