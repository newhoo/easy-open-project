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
        return "easy-open-project";
    }

    @Nls(capitalization = Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Easy Open Project";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return form;
    }

    @Override
    public boolean isModified() {
        {
            DefaultListModel<String> folderModel = form.getFolderModel();
            if (folderModel.size() != persistenceSettings.getProjectDirectoryList().size()) {
                return true;
            }
            for (int i = 0; i < folderModel.getSize(); i++) {
                if (!persistenceSettings.getProjectDirectoryList().contains(folderModel.get(i))) {
                    return true;
                }
            }
        }

        {
            DefaultListModel<String> filterFolderModel = form.getFilterFolderModel();
            if (filterFolderModel.size() != persistenceSettings.getFilterFolderList().size()) {
                return true;
            }
            for (int i = 0; i < filterFolderModel.getSize(); i++) {
                if (!persistenceSettings.getFilterFolderList().contains(filterFolderModel.get(i))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void apply() {
        persistenceSettings.setProjectDirectoryList(elementsAsSet(form.getFolderModel()));
        persistenceSettings.setFilterFolderList(elementsAsSet(form.getFilterFolderModel()));
    }

    @Override
    public void reset() {
        DefaultListModel<String> folderModel = form.getFolderModel();
        folderModel.clear();
        persistenceSettings.getProjectDirectoryList().forEach(folderModel::addElement);

        DefaultListModel<String> filterFolderModel = form.getFilterFolderModel();
        filterFolderModel.clear();
        persistenceSettings.getFilterFolderList().forEach(filterFolderModel::addElement);
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