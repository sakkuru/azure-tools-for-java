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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file;

import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;

import javax.swing.*;

@Log
public class AppServiceFileNode extends AzureRefreshableNode {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final long SIZE_20MB = 20 * 1024 * 1024;
    private final AppServiceFileService fileService;
    private final AppServiceFile file;

    public AppServiceFileNode(final AppServiceFile file, final Node parent, AppServiceFileService service) {
        super(file.getName(), file.getName(), parent, null);
        this.file = file;
        this.fileService = service;
        if (this.file.getType() != AppServiceFile.Type.DIRECTORY) {
            this.addDownloadAction();
        }
    }

    private void addDownloadAction() {
        this.addAction("Download", new NodeActionListener() {
            @Override
            protected void actionPerformed(final NodeActionEvent e) {
                download();
            }
        });
    }

    @AzureOperation(name = "download file", type = AzureOperation.Type.ACTION)
    private void download() {
        DefaultLoader.getIdeHelper().saveAppServiceFile(file, getProject(), null);
    }

    @Override
    @AzureOperation(name = "refresh file", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        if (this.file.getType() != AppServiceFile.Type.DIRECTORY) {
            return;
        }
        this.fileService.getFilesInDirectory(this.file.getPath()).stream()
            .map(file -> new AppServiceFileNode(file, this, fileService))
            .forEach(this::addChildNode);
    }

    @Override
    public void onNodeDblClicked(Object context) {
        this.openFileInEditor(context, this.file);
    }

    @AzureOperation(name = "open file in editor", type = AzureOperation.Type.ACTION)
    public void openFileInEditor(Object context, AppServiceFile file) {
        if (file.getType() == AppServiceFile.Type.DIRECTORY) {
            return;
        } else if (file.getSize() > SIZE_20MB) {
            DefaultLoader.getUIHelper().showError("File is too large, please download it first", "File is Too Large");
            return;
        }
        final Runnable open = () -> DefaultLoader.getIdeHelper().openAppServiceFile(file, context);
        AzureTaskManager.getInstance().runLater(open);
    }

    @Override
    public int getPriority() {
        return this.file.getType() == AppServiceFile.Type.DIRECTORY ? Sortable.HIGH_PRIORITY : Sortable.DEFAULT_PRIORITY;
    }

    @Override
    public Icon getIcon() {
        return DefaultLoader.getIdeHelper().getFileTypeIcon(this.file.getName(), this.file.getType() == AppServiceFile.Type.DIRECTORY);
    }

    @Override
    public String getToolTip() {
        return file.getType() == AppServiceFile.Type.DIRECTORY ?
            String.format("Type: %s Date modified: %s", file.getMime(), file.getMtime()) :
            String.format("Type: %s Size: %s Date modified: %s", file.getMime(), FileUtils.byteCountToDisplaySize(file.getSize()), file.getMtime());
    }
}
