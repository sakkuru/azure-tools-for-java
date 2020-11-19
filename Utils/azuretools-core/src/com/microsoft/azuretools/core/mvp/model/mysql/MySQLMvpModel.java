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

package com.microsoft.azuretools.core.mvp.model.mysql;

import com.microsoft.azure.management.mysql.v2017_12_01.Server;
import com.microsoft.azure.management.mysql.v2017_12_01.ServerUpdateParameters;
import com.microsoft.azure.management.mysql.v2017_12_01.SslEnforcementEnum;
import com.microsoft.azure.management.mysql.v2017_12_01.implementation.DatabaseInner;
import com.microsoft.azure.management.mysql.v2017_12_01.implementation.FirewallRuleInner;
import com.microsoft.azure.management.mysql.v2017_12_01.implementation.MySQLManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import lombok.Lombok;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MySQLMvpModel {

    private static final String NAME_ALLOW_ACCESS_TO_AZURE_SERVICES = "AllowAllWindowsAzureIps";
    private static final String IP_ALLOW_ACCESS_TO_AZURE_SERVICES = "0.0.0.0";
    private static final String NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL = "ClientIPAddress_";

    public static List<Server> listAllMySQLServers() {
        final List<Server> clusters = new ArrayList<>();
        List<Subscription> subs = AzureMvpModel.getInstance().getSelectedSubscriptions();
        if (subs.size() == 0) {
            return clusters;
        }
        Observable.from(subs).flatMap((sd) -> Observable.create((subscriber) -> {
            try {
                List<Server> clustersInSubs = listMySQLServersBySubscription(
                        sd.subscriptionId());
                synchronized (clusters) {
                    clusters.addAll(clustersInSubs);
                }
            } catch (IOException e) {
                // swallow exception and skip error subscription
            }
            subscriber.onCompleted();
        }).subscribeOn(Schedulers.io()), subs.size()).subscribeOn(Schedulers.io()).toBlocking().subscribe();
        return clusters;

    }

    public static List<DatabaseInner> listDatabases(final String subscriptionId, final Server server) {
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLClient(subscriptionId);
        final List<DatabaseInner> databases = mySQLManager.databases().inner().listByServer(server.resourceGroupName(), server.name());
        return databases;
    }

    public static List<FirewallRuleInner> listFirewallRules(final String subscriptionId, final Server server) {
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLClient(subscriptionId);
        final List<FirewallRuleInner> firewallRules = mySQLManager.firewallRules().inner().listByServer(server.resourceGroupName(), server.name());
        return firewallRules;

    }

    public static boolean isAllowAccessToAzureServices(final String subscriptionId, final Server server) {
        try {
            final String publicIp = getPublicIp();
            if (StringUtils.isBlank(publicIp)) {
                return false;
            }
            List<FirewallRuleInner> firewallRules = MySQLMvpModel2.listFirewallRules(subscriptionId, server);
            return firewallRules.stream()
                    .filter(e -> StringUtils.equals(NAME_ALLOW_ACCESS_TO_AZURE_SERVICES, e.name())).count() > 0;
        } catch (IOException e) {
            Lombok.sneakyThrow(e);
        }
        return false;
    }

    public static boolean enableAllowAccessToAzureServices(final String subscriptionId, final Server server) {
        final String ruleName = NAME_ALLOW_ACCESS_TO_AZURE_SERVICES;
        final FirewallRuleInner firewallRule = new FirewallRuleInner();
        firewallRule.withStartIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
        firewallRule.withEndIpAddress(IP_ALLOW_ACCESS_TO_AZURE_SERVICES);
        final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLClient(subscriptionId);
        mySQLManager.firewallRules().inner().createOrUpdate(server.resourceGroupName(), server.name(), ruleName, firewallRule);
        return true;
    }

    public static boolean isAllowAccessToLocalMachine(final String subscriptionId, final Server server) {
        try {
            final String publicIp = getPublicIp();
            if (StringUtils.isBlank(publicIp)) {
                return false;
            }
            List<FirewallRuleInner> firewallRules = MySQLMvpModel2.listFirewallRules(subscriptionId, server);
            return firewallRules.stream()
                    .filter(e -> StringUtils.equals(publicIp, e.startIpAddress()) && StringUtils.equals(publicIp, e.endIpAddress())).count() > 0;
        } catch (IOException e) {
            Lombok.sneakyThrow(e);
        }
        return false;
    }

    public static boolean enableAllowAccessToLocalMachine(final String subscriptionId, final Server server) {
        try {
            final String publicIp = getPublicIp();
            final String ruleName = NAME_PREFIX_ALLOW_ACCESS_TO_LOCAL + publicIp.replaceAll("\\.", "-");
            final FirewallRuleInner firewallRule = new FirewallRuleInner();
            firewallRule.withStartIpAddress(publicIp);
            firewallRule.withEndIpAddress(publicIp);
            final MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLClient(subscriptionId);
            mySQLManager.firewallRules().inner().createOrUpdate(server.resourceGroupName(), server.name(), ruleName, firewallRule);
        } catch (IOException e) {
            Lombok.sneakyThrow(e);
        }
        return true;

    }

    public static boolean updateSSLEnforcement(final String subscriptionId, final Server server, final SslEnforcementEnum sslEnforcement) {
        MySQLManager mySQLManager = AuthMethodManager.getInstance().getMySQLClient(subscriptionId);
        ServerUpdateParameters parameters = new ServerUpdateParameters();
        parameters.withSslEnforcement(sslEnforcement);
        mySQLManager.servers().inner().update(server.resourceGroupName(), server.name(), parameters);
        return true;
    }

    private static List<Server> listMySQLServersBySubscription(final String subscriptionId) throws IOException {
        return getMySQLManager(subscriptionId).servers().list();
    }

    private static MySQLManager getMySQLManager(String sid) throws IOException {
        return AuthMethodManager.getInstance().getMySQLClient(sid);
    }

    private static String getPublicIp() throws IOException {
        final URL url = new URL("http://whatismyip.akamai.com");
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        String ip;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
            while ((ip = in.readLine()) != null) {
                if (StringUtils.isNotBlank(ip)) {
                    break;
                }
            }
        }
        return ip;
    }
}
