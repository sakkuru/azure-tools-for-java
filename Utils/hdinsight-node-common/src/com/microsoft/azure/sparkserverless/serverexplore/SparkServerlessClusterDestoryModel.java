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

package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class SparkServerlessClusterDestoryModel implements Cloneable {
    @NotNull
    private String clusterName;

    @Nullable
    private String errorMessage;

    @NotNull
    public String getClusterName() {
        return clusterName;
    }

    public SparkServerlessClusterDestoryModel setClusterName(@NotNull String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public SparkServerlessClusterDestoryModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}