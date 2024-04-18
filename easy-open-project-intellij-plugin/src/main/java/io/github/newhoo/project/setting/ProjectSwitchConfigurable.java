package io.github.newhoo.project.setting;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * ProjectSwitchConfigurable
 */
public class ProjectSwitchConfigurable implements SearchableConfigurable {

    private final MyProjectSwitcherSetting persistenceSettings = MyProjectSwitcherSetting.getInstance();
    private final ProjectSwitchSettingForm form = new ProjectSwitchSettingForm();

    @NotNull
    @Override
    public String getId() {
        return "project-switcher";
    }

    @Nls(capitalization = Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Project Switcher";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form;
    }

    @Override
    public boolean isModified() {
        DefaultListModel<String> defaultModel = form.getDefaultModel();
        if (defaultModel.size() != persistenceSettings.getProjectDirectoryList().size()) {
            return true;
        }

        int size = defaultModel.getSize();
        for (int i = 0; i < size; i++) {
            if (!persistenceSettings.getProjectDirectoryList().contains(defaultModel.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() {
        persistenceSettings.setProjectDirectoryList(elementsAsSet(form.getDefaultModel()));
    }

    @Override
    public void reset() {
        DefaultListModel<String> defaultModel = form.getDefaultModel();
        defaultModel.clear();
        persistenceSettings.getProjectDirectoryList().forEach(defaultModel::addElement);
    }

    private <E> Set<E> elementsAsSet(DefaultListModel<E> defaultListModel) {
        int size = defaultListModel.getSize();
        Set<E> result = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            result.add(defaultListModel.get(i));
        }
        return result;
    }
}