package io.github.newhoo.project.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * MyProjectSwitcherSetting
 */
@State(name = "MyProjectSwitcherSetting", storages = {@Storage("my-project-switcher.settings.xml")})
public class MyProjectSwitcherSetting implements PersistentStateComponent<MyProjectSwitcherSetting> {

    private Set<String> projectDirectoryList = new HashSet<>(4);

    private Set<String> filterFolderList = new HashSet<>(4);

    private boolean enableFilterPathInSearchEvery = false;

    private String lastOpenProjectPath;

    @Nullable
    @Override
    public MyProjectSwitcherSetting getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MyProjectSwitcherSetting state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static MyProjectSwitcherSetting getInstance() {
        return ApplicationManager.getApplication().getService(MyProjectSwitcherSetting.class);
    }

    public Set<String> getProjectDirectoryList() {
        return projectDirectoryList;
    }

    public void setProjectDirectoryList(Set<String> projectDirectoryList) {
        this.projectDirectoryList = projectDirectoryList;
    }

    public Set<String> getFilterFolderList() {
        return filterFolderList;
    }

    public void setFilterFolderList(Set<String> filterFolderList) {
        this.filterFolderList = filterFolderList;
    }

    public boolean isEnableFilterPathInSearchEvery() {
        return enableFilterPathInSearchEvery;
    }

    public void setEnableFilterPathInSearchEvery(boolean enableFilterPathInSearchEvery) {
        this.enableFilterPathInSearchEvery = enableFilterPathInSearchEvery;
    }

    public String getLastOpenProjectPath() {
        return lastOpenProjectPath;
    }
    public void setLastOpenProjectPath(String lastOpenProjectPath) {
        this.lastOpenProjectPath = lastOpenProjectPath;
    }
}