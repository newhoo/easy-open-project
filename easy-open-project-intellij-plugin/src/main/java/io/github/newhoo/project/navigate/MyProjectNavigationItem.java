package io.github.newhoo.project.navigate;

import com.intellij.icons.AllIcons;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;

import javax.swing.*;

/**
 * MyProjectNavigationItem
 */
public class MyProjectNavigationItem implements NavigationItem {

    private final String projectName;
    private final String projectPath;
    private final long lastModify;
    private final Icon icon;
    private final boolean isOpen;

    public MyProjectNavigationItem(String projectName, String projectPath, boolean isOpen) {
        this(projectName, projectPath, 0, isOpen);
    }

    public MyProjectNavigationItem(String projectName, String projectPath, long lastModify, boolean isOpen) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.lastModify = lastModify;
        this.isOpen = isOpen;
        this.icon = /*isOpen ? AllIcons.General.ProjectTab : */AllIcons.Nodes.Module;
    }

    public long getLastModify() {
        return lastModify;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public String getProjectName() {
        return projectName;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getProjectPath() {
        return projectPath;
    }

    @Override
    public String getName() {
        return projectName;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                if (isOpen) {
                    return projectName + " (opened)";
                }
                return projectName + " - " + projectPath;
            }

            @Override
            public Icon getIcon(boolean unused) {
                return icon;
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public String toString() {
        return (isOpen ? "[current] " : "[recent] ") + projectPath;
    }
}
