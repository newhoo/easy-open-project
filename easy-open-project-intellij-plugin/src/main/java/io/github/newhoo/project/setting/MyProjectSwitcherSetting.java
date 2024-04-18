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

    private static final MyProjectSwitcherSetting INSTANCE = ApplicationManager.getApplication().getService(MyProjectSwitcherSetting.class);

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
        return INSTANCE;
    }

    public Set<String> getProjectDirectoryList() {
        return projectDirectoryList;
    }

    public void setProjectDirectoryList(Set<String> projectDirectoryList) {
        this.projectDirectoryList = projectDirectoryList;
    }
}