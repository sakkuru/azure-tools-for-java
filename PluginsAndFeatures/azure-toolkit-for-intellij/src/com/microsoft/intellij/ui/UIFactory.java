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

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;
import com.microsoft.intellij.ProgressTaskModal;
import com.microsoft.azuretools.utils.IProgressTaskImpl;

/**
 * Created by shch on 10/4/2016.
 */
public class UIFactory implements IUIFactory{

    @Override
    public INotification getNotificationWindow() {
        return new NotificationWindow();
    }

    @Override
    public IWebUi getWebUi() { return new WebUi(); }

    @Override
    public IDeviceLoginUI getDeviceLoginUI() { return new DeviceLoginUI(); }

    @Override
    public IProgressTaskImpl getProgressTaskImpl() {
        return new ProgressTaskModal(getProject());
    }

    private Project getProject() {
        return null;
    }
}
