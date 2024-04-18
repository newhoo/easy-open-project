package io.github.newhoo.project.navigate;

import com.intellij.ide.actions.SearchEverywhereBaseAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

/**
 * MyProjectSearchAction
 */
public class MyProjectSearchAction extends SearchEverywhereBaseAction implements DumbAware {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        showInSearchEverywherePopup("MyProjectSearchEverywhereContributor", e, false, false);
    }
}
