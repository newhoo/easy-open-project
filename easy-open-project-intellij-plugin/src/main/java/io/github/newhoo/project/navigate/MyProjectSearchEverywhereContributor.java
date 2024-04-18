package io.github.newhoo.project.navigate;

import com.intellij.CommonBundle;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereUI;
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ProjectWindowAction;
import com.intellij.openapi.wm.impl.ProjectWindowActionGroup;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.Processor;
import com.intellij.util.ui.UIUtil;
import io.github.newhoo.project.setting.MyProjectSwitcherSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MyProjectSearchEverywhereContributor
 */
public class MyProjectSearchEverywhereContributor implements WeightedSearchEverywhereContributor<MyProjectNavigationItem> {

    final AnActionEvent event;

    public MyProjectSearchEverywhereContributor(@NotNull AnActionEvent event) {
        this.event = event;
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "MyProjectSearchEverywhereContributor";
    }

    @NotNull
    @Override
    public String getGroupName() {
        return "Projects";
    }

    @Override
    public int getSortWeight() {
        return 799;
    }

    @NotNull
    @Override
    public ListCellRenderer<Object> getElementsRenderer() {
        return new SearchEverywherePsiRenderer(this) {

            private JList list = null;
            private final KeyAdapter closeProjectKeyAdapter = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyLocation() != KeyEvent.KEY_LOCATION_LEFT && e.getKeyLocation() != KeyEvent.KEY_LOCATION_RIGHT) {
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ALT && list != null) {
                        final Object selectedValue = list.getSelectedValue();
                        if (!(selectedValue instanceof MyProjectNavigationItem)) {
                            return;
                        }
//                        if (selectedValue == null || EXTRA_ELEM.equals(selectedValue)) {
//                            return;
//                        }
                        closeProject((MyProjectNavigationItem) selectedValue);
                    }
                }
            };

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component retComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (index == 0 && this.list == null) {
                    this.list = list;

                    Container parent = list.getParent();
                    try {
                        for (int i = 0; i < 5; i++) {
                            if (parent == null) {
                                break;
                            }
                            if (parent instanceof SearchEverywhereUI) {
                                ((SearchEverywhereUI) parent).getSearchField().addKeyListener(closeProjectKeyAdapter);
                                break;
                            } else {
                                if (parent.getComponentCount() > 0 && parent.getComponents()[0] instanceof SearchEverywhereUI) {
                                    SearchEverywhereUI component = ((SearchEverywhereUI) parent.getComponents()[0]);
                                    component.getSearchField().addKeyListener(closeProjectKeyAdapter);
                                    break;
                                }
                            }
                            parent = parent.getParent();
                        }
                    } catch (Exception e) {
                    }
                }
                return retComponent;
            }

            @Override
            protected boolean customizeNonPsiElementLeftRenderer(ColoredListCellRenderer renderer, JList list, Object value, int index, boolean selected, boolean hasFocus) {
                Color fgColor = list.getForeground();
                Color bgColor = UIUtil.getListBackground();
                TextAttributes attributes = getNavigationItemAttributes(value);
                SimpleTextAttributes nameAttributes = attributes != null ? SimpleTextAttributes.fromTextAttributes(attributes) : null;
                if (nameAttributes == null)
                    nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, fgColor);

                ItemMatchers itemMatchers = getItemMatchers(list, value);
                MyProjectNavigationItem item = (MyProjectNavigationItem) value;
                String name = item.getProjectName() + " ";
                String locationString = item.isOpen() ? "(opened)" : item.getProjectPath();

                SpeedSearchUtil.appendColoredFragmentForMatcher(name, renderer, nameAttributes, itemMatchers.nameMatcher, bgColor, selected);
                renderer.setIcon(item.isOpen() ? ExecutionUtil.getLiveIndicator(item.getIcon()) : item.getIcon());

                FontMetrics fm = list.getFontMetrics(list.getFont());
                int maxWidth = list.getWidth() - fm.stringWidth(name) - myRightComponentWidth - 36;
                int fullWidth = fm.stringWidth(locationString);
                if (fullWidth < maxWidth) {
                    SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, itemMatchers.nameMatcher, bgColor, selected);
                } else {
                    int adjustedWidth = Math.max(locationString.length() * maxWidth / fullWidth - 1, 3);
                    locationString = StringUtil.trimMiddle(locationString, adjustedWidth);
                    SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, itemMatchers.nameMatcher, bgColor, selected);
                }
                return true;
            }
        };
    }

    @Override
    public @Nullable
    Object getDataForItem(@NotNull MyProjectNavigationItem element, @NotNull String dataId) {
        return null;
    }

    @Override
    public boolean processSelectedItem(@NotNull MyProjectNavigationItem selected, int modifiers, @NotNull String searchText) {
        if (selected.isOpen()) {
            active(selected.getProjectPath(), event);
            return true;
        }
        openProject(selected, event);
        return true;
    }

    private static void openProject(MyProjectNavigationItem selected, @NotNull AnActionEvent event) {
        Project project = event.getRequiredData(CommonDataKeys.PROJECT);
        String projectBasePath = selected.getProjectPath();
        if (Files.exists(Paths.get(projectBasePath))) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    if (ProjectManager.getInstance().loadAndOpenProject(projectBasePath) != null) {
                        active(projectBasePath, event);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            if (Messages.showDialog(project, IdeBundle
                    .message("message.the.path.0.does.not.exist.maybe.on.remote", FileUtil.toSystemDependentName(projectBasePath)), IdeBundle.message("dialog.title.reopen.project"), new String[]{CommonBundle.getOkButtonText(), IdeBundle.message("button.remove.from.list")}, 0, Messages.getErrorIcon()) == 1) {
                RecentProjectsManager.getInstance().removePath(projectBasePath);
            }
        }
    }

    /**
     * 切换项目窗口
     *
     * @param projectLocation
     * @see com.intellij.openapi.wm.impl.ProjectWindowAction
     * @see PlatformActions.xml OpenProjectWindows
     */
    private static void active(String projectLocation, @NotNull AnActionEvent e) {
        AnAction action = ActionManager.getInstance().getAction("OpenProjectWindows");
        if (action instanceof ProjectWindowActionGroup) {
            final AnAction[] children = ((ProjectWindowActionGroup) action).getChildren(null);
            for (AnAction child : children) {
                if (!(child instanceof ProjectWindowAction)) {
                    continue;
                }
                final ProjectWindowAction windowAction = (ProjectWindowAction) child;
                if (projectLocation.equals(windowAction.getProjectLocation())) {
                    windowAction.setSelected(e, true);
                    return;
                }
            }
        }
    }

    /**
     * @see com.intellij.ide.actions.CloseProjectsActionBase
     */
    private static void closeProject(MyProjectNavigationItem selectedItem) {
        if (!selectedItem.isOpen()) {
            return;
        }
        IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
        Arrays.stream(ProjectManager.getInstance().getOpenProjects())
              .filter(project -> selectedItem.getProjectPath().equals(project.getPresentableUrl()))
              .forEach(project -> {
                  WindowManager.getInstance().updateDefaultFrameInfoOnProjectClose(project);
                  ProjectManager.getInstance().closeAndDispose(project);
                  RecentProjectsManager.getInstance().updateLastProjectPath();
              });
        WelcomeFrame.showIfNoProjectOpened();
    }

    @Override
    public boolean isEmptyPatternSupported() {
        return true;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    @Override
    public boolean showInFindResults() {
        return false;
    }

    private List<FoundItemDescriptor<MyProjectNavigationItem>> foundItemDescriptorList;

    @Override
    public void fetchWeightedElements(@NotNull String pattern, @NotNull ProgressIndicator progressIndicator, @NotNull Processor<? super FoundItemDescriptor<MyProjectNavigationItem>> consumer) {
        Project project = event.getProject();
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
        if (!getSearchProviderId().equals(seManager.getSelectedTabID())) {
            return;
        }

        if (foundItemDescriptorList == null) {
            foundItemDescriptorList = new ArrayList<>(16);

            // 已经打开的项目
            Project[] openProjects = ProjectUtil.getOpenProjects();
            for (Project openProject : openProjects) {
                FoundItemDescriptor<MyProjectNavigationItem> descriptor = new FoundItemDescriptor<>(new MyProjectNavigationItem(openProject.getName(), openProject.getPresentableUrl(), true), 1);
                foundItemDescriptorList.add(descriptor);
            }


            // 使用idea自带的最近项目, 不会包含已打开的项目
            Set<String> filterRecentProjectPathSet = Arrays.stream(openProjects).map(Project::getPresentableUrl).collect(Collectors.toSet());
            RecentProjectsManagerBase recentProjectsManagerBase = (RecentProjectsManagerBase) RecentProjectsManager.getInstance();
            List<String> recentPaths = recentProjectsManagerBase.getRecentPaths();
            for (String recentPath : recentPaths) {
                if (!filterRecentProjectPathSet.contains(recentPath)) {
                    String projectName = recentProjectsManagerBase.getProjectName(recentPath);
                    filterRecentProjectPathSet.add(recentPath);
                    foundItemDescriptorList.add(new FoundItemDescriptor<>(new MyProjectNavigationItem(projectName, recentPath, false), 0));
                }
            }

            // 使用自定义目录查找项目
            Set<String> workspaces = MyProjectSwitcherSetting.getInstance().getProjectDirectoryList();
            workspaces.stream()
                      .flatMap(workspace -> searchProject(new File(workspace), workspaces))
                      .sorted(Comparator.comparing(e -> -e.getLastModify()))
                      .filter(e -> !filterRecentProjectPathSet.contains(e.getProjectPath()))
                      .forEach(o -> foundItemDescriptorList.add(new FoundItemDescriptor<>(o, 0)));
        }

        MinusculeMatcher minusculeMatcher = NameUtil.buildMatcher("*" + pattern + "*", NameUtil.MatchingCaseSensitivity.NONE);
        for (FoundItemDescriptor<MyProjectNavigationItem> foundItemDescriptor : foundItemDescriptorList) {
            if (minusculeMatcher.matches(foundItemDescriptor.getItem().getProjectName())
                    || minusculeMatcher.matches(foundItemDescriptor.getItem().getProjectPath())) {
                if (!consumer.process(foundItemDescriptor)) {
                    return;
                }
            }
        }
    }

    private static final Set<String> IGNORE_DIR = Stream.of(
            "target", "out", "build", "idea-sandbox", "logs", "src", "node_modules", "doc", "distributions", "docs"
    ).collect(Collectors.toSet());

    private Stream<MyProjectNavigationItem> searchProject(@NotNull File workspace, Set<String> workspaces) {
        if (workspace.isDirectory()) {
            File[] childFiles = workspace.listFiles((dir, name) -> !IGNORE_DIR.contains(name) && !name.startsWith("."));
            if (childFiles != null) {
                return Arrays.stream(childFiles)
                             .filter(file -> file.isDirectory() && !workspaces.contains(file.getAbsolutePath()))
                             .map(file -> new MyProjectNavigationItem(file.getName(), backslashToSlash(file.getAbsolutePath()), file.lastModified(), false));
            }
        }
        return Stream.empty();
    }

    private String backslashToSlash(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replace("\\", "/");
    }

    public static class Factory implements SearchEverywhereContributorFactory<MyProjectNavigationItem> {
        @NotNull
        @Override
        public SearchEverywhereContributor<MyProjectNavigationItem> createContributor(@NotNull AnActionEvent initEvent) {
            return new MyProjectSearchEverywhereContributor(initEvent);
        }
    }
}
