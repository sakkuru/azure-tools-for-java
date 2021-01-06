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

package com.microsoft.tooling.msservices.serviceexplorer;

import lombok.Getter;

public enum AzureActionEnum {

    REFRESH("Refresh", "Refreshing", AzureIconSymbol.Common.REFRESH, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY),
    CREATE("Create", "Creating", AzureIconSymbol.Common.CREATE, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 2),

    OPEN_IN_PORTAL("Open In Portal", "Opening", AzureIconSymbol.Common.OPEN_IN_PORTAL, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 10),
    OPEN_IN_BROWSER("Open In Browser", "Opening", AzureIconSymbol.Common.OPEN_IN_PORTAL, Groupable.DEFAULT_GROUP, Sortable.HIGH_PRIORITY + 11),
    SHOW_PROPERTIES("Show Properties", "Loading", AzureIconSymbol.Common.SHOW_PROPERTIES),

    START("Start", "Starting", AzureIconSymbol.Common.START, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 1),
    STOP("Stop", "Stopping", AzureIconSymbol.Common.STOP, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 2),
    RESTART("Restart", "Restarting", AzureIconSymbol.Common.RESTART, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 3),
    DELETE("Delete", "Deleting", AzureIconSymbol.Common.DELETE, Groupable.MAINTENANCE_GROUP, Sortable.DEFAULT_PRIORITY + 4);

    @Getter
    private final String name;
    @Getter
    private final String doingName;
    @Getter
    private final AzureIconSymbol iconSymbol;
    @Getter
    private final Integer group;
    @Getter
    private final Integer priority;

    AzureActionEnum(String name, String doingName, AzureIconSymbol iconSymbol) {
        this(name, doingName, iconSymbol, Groupable.DEFAULT_GROUP, Sortable.DEFAULT_PRIORITY);
    }

    AzureActionEnum(String name, String doingName, AzureIconSymbol iconSymbol, Integer group, Integer priority) {
        this.name = name;
        this.doingName = doingName;
        this.iconSymbol = iconSymbol;
        this.group = group;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return name();
    }

}
