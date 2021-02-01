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

package com.microsoft.intellij.helpers;

import com.google.common.util.concurrent.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ApplicationSettings;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Log
public class IDEHelperImpl implements IDEHelper {

    private static final String APP_SERVICE_FILE_EDITING = "App Service File Editing";
    private static final String FILE_HAS_BEEN_DELETED = "File '%s' has been deleted from remote server, "
        + "do you want to create a new file with the changed content?";
    private static final String FILE_HAS_BEEN_MODIFIED = "File '%s' has been modified since you view it, do you still want to save your changes?";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    public static final String FILE_HAS_BEEN_SAVED = "File %s has been saved to Azure";

    @Override
    public void setApplicationProperty(@NotNull String name, @NotNull String value) {
        ApplicationSettings.getInstance().setProperty(name, value);
    }

    @Override
    public void unsetApplicationProperty(@NotNull String name) {
        ApplicationSettings.getInstance().unsetProperty(name);
    }

    @Override
    public String getApplicationProperty(@NotNull String name) {
        return ApplicationSettings.getInstance().getProperty(name);
    }

    @Override
    public void setApplicationProperties(@NotNull String name, @NotNull String[] value) {
        ApplicationSettings.getInstance().setProperties(name, value);
    }

    @Override
    public void unsetApplicatonProperties(@NotNull String name) {
        ApplicationSettings.getInstance().unsetProperty(name);
    }

    @Override
    public String[] getApplicationProperties(@NotNull String name) {
        return ApplicationSettings.getInstance().getProperties(name);
    }

    @Override
    public boolean isApplicationPropertySet(@NotNull String name) {
        return ApplicationSettings.getInstance().isPropertySet(name);
    }

    @Override
    public String getProjectSettingsPath() {
        return PluginUtil.getPluginRootDirectory();
    }

    @Override
    public void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile) {
        AzureTaskManager.getInstance().runLater(() -> FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile));
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        AzureTaskManager.getInstance().runLater(runnable, AzureTask.Modality.ANY);
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        AzureTaskManager.getInstance().runAndWait(runnable, AzureTask.Modality.ANY);
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Nullable
    @Override
    public String getProperty(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).getProperty(name);
    }

    public String getProperty(@NotNull String name, Object projectObject) {
        return AzureSettings.getSafeInstance((Project) projectObject).getProperty(name);
    }

    @NotNull
    @Override
    public String getPropertyWithDefault(@NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance().getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).setProperty(name, value);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).setProperty(name, value);
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).unsetProperty(name);
    }

    @Override
    public void unsetProperty(@NotNull String name, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).unsetProperty(name);
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).isPropertySet(name);
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).getProperties(name);
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name, Object projectObject) {
        return AzureSettings.getSafeInstance((Project) projectObject).getProperties(name);
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).setProperties(name, value);
    }

    @NotNull
    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        Project project = findOpenProject(projectDescriptor);

        List<ArtifactDescriptor> artifactDescriptors = new ArrayList<ArtifactDescriptor>();

        for (Artifact artifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            artifactDescriptors.add(new ArtifactDescriptor(artifact.getName(), artifact.getArtifactType().getId()));
        }

        return artifactDescriptors;
    }

    @NotNull
    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                                  @NotNull ArtifactDescriptor artifactDescriptor) {
        try {
            Project project = findOpenProject(projectDescriptor);

            final Artifact artifact = findProjectArtifact(project, artifactDescriptor);

            final SettableFuture<String> future = SettableFuture.create();

            Futures.addCallback(buildArtifact(project, artifact, false), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean succeded) {
                    if (succeded != null && succeded) {
                        future.set(artifact.getOutputFilePath());
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact"));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable instanceof ExecutionException) {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                                                  throwable.getCause()));
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                                                  throwable));
                    }
                }
            }, MoreExecutors.directExecutor());

            return future;
        } catch (AzureCmdException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public Object getCurrentProject() {
        return PluginUtil.getSelectedProject();
    }

    @NotNull
    private static byte[] getArray(@NotNull InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int readCount;
        byte[] data = new byte[16384];

        while ((readCount = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, readCount);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static ListenableFuture<Boolean> buildArtifact(@NotNull Project project, final @NotNull Artifact artifact, boolean rebuild) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>(1);
        artifacts.add(artifact);
        CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);
        ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

        CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                future.set(!aborted && errors == 0);
            }
        });

        return future;
    }

    @NotNull
    private static Project findOpenProject(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        Project project = null;

        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (StringUtils.equals(projectDescriptor.getName(), openProject.getName()) &&
                StringUtils.equals(projectDescriptor.getPath(), openProject.getBasePath())) {
                project = openProject;
                break;
            }
        }

        if (project == null) {
            throw new AzureCmdException("Unable to find an open project with the specified description.");
        }

        return project;
    }

    @NotNull
    private static Artifact findProjectArtifact(@NotNull Project project, @NotNull ArtifactDescriptor artifactDescriptor)
        throws AzureCmdException {
        Artifact artifact = null;

        for (Artifact projectArtifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            if (artifactDescriptor.getName().equals(projectArtifact.getName()) &&
                artifactDescriptor.getArtifactType().equals(projectArtifact.getArtifactType().getId())) {
                artifact = projectArtifact;
                break;
            }
        }

        if (artifact == null) {
            throw new AzureCmdException("Unable to find an artifact with the specified description.");
        }

        return artifact;
    }

    public void openLinkInBrowser(@NotNull String url) {
        try {
            BrowserUtil.browse(url);
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showException("Unexpected exception: " + e.getMessage(), e, "Browse Web App", true, false);
            DefaultLoader.getUIHelper().logError("Unexpected exception: " + e.getMessage(), e);
        }
    }

    @Override
    public @Nullable Icon getFileTypeIcon(String name, boolean isDirectory) {
        if (isDirectory) {
            if (Objects.equals(name, "/")) {
                return AllIcons.Nodes.CopyOfFolder;
            }
            return AllIcons.Nodes.Folder;
        }
        final FileType type = FileTypeManager.getInstance().getFileTypeByFileName(name);
        return type.getIcon();
    }

    private static final Key<String> APP_SERVICE_FILE_ID = new Key<>("APP_SERVICE_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String SUCCESS_DOWNLOADING = "File[%s] is successfully downloaded to [%s].";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    @AzureOperation(
        name = "open file[%s] in editor",
        params = {"$target.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    @SneakyThrows
    public void openAppServiceFile(final AppServiceFile target, Object context) {
        final AppServiceFileService fileService = AppServiceFileService.forApp(target.getApp());
        final FileEditorManager manager = FileEditorManager.getInstance((Project) context);
        final VirtualFile vFile = getOrCreateVirtualFile(target, manager);
        final OutputStream output = vFile.getOutputStream(null);
        final String title = String.format("Opening file (%s)...", vFile.getName());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            indicator.setText2("Checking file existence");
            this.checkExistence(target, fileService).ifPresent((file) -> {
                indicator.setText2("Loading file content");
                fileService
                    .getFileContent(file.getPath())
                    .doOnCompleted(() -> AzureTaskManager.getInstance().runLater(() -> openFileInEditor(file, vFile, manager), AzureTask.Modality.NONE))
                    .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
                    .subscribe(bytes -> writeContentToEditor(output, bytes), AzureExceptionHandler::onRxException);
            });
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    @AzureOperation(
        value = "load content of file[%s] into editor",
        params = {"$target.getName()"},
        type = AzureOperation.Type.TASK
    )
    private void writeContentToEditor(final OutputStream output, final byte[] bytes) {
        try {
            IOUtils.write(bytes, output);
        } catch (final IOException e) {
            final String error = "failed to load file content into editor";
            final String action = "try later or downloading it first";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
    }

    @AzureOperation(
        value = "check the existence of file[%s]",
        params = {"$target.getName()"},
        type = AzureOperation.Type.TASK
    )
    private Optional<AppServiceFile> checkExistence(final AppServiceFile target, final AppServiceFileService fileService) {
        final AppServiceFile file = fileService.getFileByPath(target.getPath());
        if (file == null) {
            final String failureFileDeleted = String.format("Target file (%s) has been deleted", target.getName());
            UIUtil.invokeLaterIfNeeded(() -> Messages.showWarningDialog(failureFileDeleted, "Open File"));
        }
        return Optional.ofNullable(file);
    }

    @AzureOperation(
        value = "open and focus the editor of file[%s]",
        params = {"$target.getName()"},
        type = AzureOperation.Type.TASK
    )
    private void openFileInEditor(final AppServiceFile target, VirtualFile virtualFile, FileEditorManager fileEditorManager) {
        final FileEditor[] editors = fileEditorManager.openFile(virtualFile, true, true);
        if (editors.length == 0) {
            final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", virtualFile.getName());
            Messages.showWarningDialog(failure, "Open File");
            return;
        }
        for (final FileEditor editor : editors) {
            if (editor instanceof TextEditor) {
                final String originContent = getTextEditorContent((TextEditor) editor);
                final MessageBusConnection messageBusConnection = fileEditorManager.getProject().getMessageBus().connect(editor);
                messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
                    @Override
                    public void beforeFileClosed(FileEditorManager source, VirtualFile file) {
                        try {
                            final String content = getTextEditorContent((TextEditor) editor);
                            if (file == virtualFile && !StringUtils.equals(content, originContent)) {
                                boolean result = DefaultLoader.getUIHelper().showYesNoDialog(
                                    editor.getComponent(), SAVE_CHANGES, APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
                                if (result) {
                                    saveFileToAzure(target, content, fileEditorManager.getProject());
                                }
                            }
                        } catch (RuntimeException e) {
                            AzureExceptionHandler.getInstance().handleException(e);
                        } finally {
                            messageBusConnection.disconnect();
                        }
                    }
                });
            }
        }
    }

    private static String getTextEditorContent(TextEditor textEditor) {
        return textEditor.getEditor().getDocument().getText();
    }

    @AzureOperation(
        name = "save file[%s] to azure",
        params = {"$appServiceFile.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    private void saveFileToAzure(final AppServiceFile appServiceFile, final String content, final Project project) {
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, String.format("Saving file (%s)...", appServiceFile.getName()), false, () -> {
            final AppServiceFileService fileService = AppServiceFileService.forApp(appServiceFile.getApp());
            final AppServiceFile target = fileService.getFileByPath(appServiceFile.getPath());
            final boolean deleted = target == null;
            final boolean outDated = ZonedDateTime.parse(target.getMtime()).isAfter(ZonedDateTime.parse(appServiceFile.getMtime()));
            boolean toSave = true;
            if (deleted) {
                toSave = DefaultLoader.getUIHelper().showYesNoDialog(null, String.format(FILE_HAS_BEEN_DELETED, appServiceFile.getName()),
                                                                     APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
            } else if (outDated) {
                toSave = DefaultLoader.getUIHelper().showYesNoDialog(
                    null, String.format(FILE_HAS_BEEN_MODIFIED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
            }
            if (toSave) {
                fileService.uploadFileToPath(content, appServiceFile.getPath());
                PluginUtil.showInfoNotification(APP_SERVICE_FILE_EDITING, String.format(FILE_HAS_BEEN_SAVED, appServiceFile));
            }
        }));
    }

    /**
     * user is asked to choose where to save the file is @param dest is null
     */
    @AzureOperation(
        name = "download file[%s] to local",
        params = {"$file.getName()"},
        type = AzureOperation.Type.SERVICE
    )
    @SneakyThrows
    public void saveAppServiceFile(@NotNull final AppServiceFile file, @NotNull Object context, @Nullable File dest) {
        final File destFile = Objects.isNull(dest) ? DefaultLoader.getUIHelper().showFileSaver("Download", file.getName()) : dest;
        if (Objects.isNull(destFile)) {
            return;
        }
        final OutputStream output = new FileOutputStream(destFile);
        final Project project = (Project) context;
        final String title = String.format("Downloading file (%s)...", file.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            AppServiceFileService
                .forApp(file.getApp())
                .getFileContent(file.getPath())
                .doOnCompleted(() -> notifyDownloadSuccess(file, destFile, ((Project) context)))
                .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
                .subscribe(bytes -> {
                    try {
                        IOUtils.write(bytes, output);
                    } catch (final IOException e) {
                        final String error = "failed to write data into local file";
                        final String action = "try later";
                        throw new AzureToolkitRuntimeException(error, e, action);
                    }
                }, AzureExceptionHandler::onRxException);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    private void notifyDownloadSuccess(final AppServiceFile file, final File dest, final Project project) {
        final String title = "File downloaded";
        final File directory = dest.getParentFile();
        final String message = String.format(SUCCESS_DOWNLOADING, file.getName(), directory.getAbsolutePath());
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION);
        notification.addAction(new AnAction(RevealFileAction.getActionName()) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                RevealFileAction.openFile(dest);
            }
        });
        notification.addAction(new AnAction("Open In Editor") {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                final VirtualFile virtualFile = VfsUtil.findFileByIoFile(dest, true);
                if (Objects.nonNull(virtualFile)) {
                    fileEditorManager.openFile(virtualFile, true, true);
                }
            }
        });
        Notifications.Bus.notify(notification);
    }

    private VirtualFile getOrCreateVirtualFile(AppServiceFile file, FileEditorManager manager) {
        synchronized (file) {
            return Arrays.stream(manager.getOpenFiles())
                         .filter(f -> StringUtils.equals(f.getUserData(APP_SERVICE_FILE_ID), file.getId()))
                         .findFirst().orElse(createVirtualFile(file, manager));
        }
    }

    @SneakyThrows
    private LightVirtualFile createVirtualFile(AppServiceFile file, FileEditorManager manager) {
        final LightVirtualFile virtualFile = new LightVirtualFile(file.getFullName());
        virtualFile.setFileType(FileTypeManager.getInstance().getFileTypeByFileName(file.getName()));
        virtualFile.setCharset(StandardCharsets.UTF_8);
        virtualFile.putUserData(APP_SERVICE_FILE_ID, file.getId());
        virtualFile.setWritable(true);
        return virtualFile;
    }
}
