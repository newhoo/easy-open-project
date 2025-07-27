package io.github.newhoo.project.navigate;

import com.intellij.CommonBundle;
import com.intellij.completion.ngram.slp.util.Pair;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.actions.searcheverywhere.ExtendedInfo;
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereCommandInfo;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereExtendedInfoProvider;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereUI;
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.ProjectWindowAction;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.Processor;
import com.intellij.util.text.Matcher;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MyProjectSearchEverywhereContributor
 */
public class MyProjectSearchEverywhereContributor implements WeightedSearchEverywhereContributor<MyProjectNavigationItem>, SearchEverywhereExtendedInfoProvider {

    private final SearchEverywhereCommandInfo FINDER_COMMAND = new SearchEverywhereCommandInfo("finder", SystemInfo.isMac ? ActionsBundle.message("action.RevealIn.name.mac") : ActionsBundle.message("action.RevealIn.name.other", IdeBundle.message("action.explorer.text")), this);

    private final Project project;
    private final AnActionEvent event;
    private final MyProjectSwitcherSetting myProjectSwitcherSetting;
    private List<MyProjectNavigationItem> foundProjectItemList;

    private static String lastOpenPath = null;

    public MyProjectSearchEverywhereContributor(@NotNull AnActionEvent event) {
        project = event.getData(CommonDataKeys.PROJECT);
        this.event = event;
        this.myProjectSwitcherSetting = MyProjectSwitcherSetting.getInstance();
    }

    @NotNull
    @Override
    public String getSearchProviderId() {
        return "MyProjectSearchEverywhereContributor";
    }

    @NotNull
    @Override
    public String getGroupName() {
        return messageWithChineseLangCheck("Projects", "项目");
    }

    private static String messageWithChineseLangCheck(String enValue, String chValue) {
        if (Locale.getDefault().toString().contains(Locale.SIMPLIFIED_CHINESE.toString())) {
            IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId("com.intellij.zh"));
            if (plugin != null && plugin.isEnabled()) {
                return chValue;
            }
        }
        return enValue;
    }

    @Override
    public int getSortWeight() {
        return 799;
    }

    @Override
    public String getAdvertisement() {
//        return DumbService.isDumb(myProject) ? IdeBundle.message("dumb.mode.results.might.be.incomplete") : null;
        String key = SystemInfo.isMac ? "Option" : "Alt";
        return messageWithChineseLangCheck("Press [" + key + "] to close selected project", "按 [" + key + "] 以关闭项目");
    }

    @NotNull
    @Override
    public List<AnAction> getActions(@NotNull Runnable onChanged) {
        CheckboxAction filterPathAction = new CheckboxAction(messageWithChineseLangCheck("Filter with path", "同时过滤路径")) {
            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return myProjectSwitcherSetting.isEnableFilterPathInSearchEvery();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                myProjectSwitcherSetting.setEnableFilterPathInSearchEvery(state);
                onChanged.run();
            }
        };
        return Arrays.asList(filterPathAction);
    }

    @Override
    public @NotNull List<SearchEverywhereCommandInfo> getSupportedCommands() {
        return Arrays.asList(this.FINDER_COMMAND);
    }

    @Override
    public @Nullable ExtendedInfo createExtendedInfo() {
        return new ExtendedInfo(
                o -> !(o instanceof MyProjectNavigationItem item) || !item.isOpen() ? null : getAdvertisement(),
                o -> null);
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
                        // if (selectedValue == null || EXTRA_ELEM.equals(selectedValue)) {
                        // return;
                        // }
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

                MyProjectNavigationItem item = (MyProjectNavigationItem) value;
                String name = item.getProjectName() + " ";
                String locationString = item.isOpen() ? "(opened)" : item.getProjectPath();
                renderer.setIcon(item.isOpen() ? ExecutionUtil.getLiveIndicator(item.getIcon()) : item.getIcon());

                ItemMatchers itemMatchers = getItemMatchers(list, value);
                SpeedSearchUtil.appendColoredFragmentForMatcher(name, renderer, nameAttributes, itemMatchers.nameMatcher, bgColor, selected);

                Matcher locationMatcher = itemMatchers.locationMatcher;
                if (itemMatchers.nameMatcher != null && !item.isOpen()) {
                    if (myProjectSwitcherSetting.isEnableFilterPathInSearchEvery()
                            && !itemMatchers.nameMatcher.matches(item.getProjectName())
                            && itemMatchers.nameMatcher.matches(item.getProjectPath())) {
                        locationMatcher = itemMatchers.nameMatcher;
                    }
                }

                FontMetrics fm = list.getFontMetrics(list.getFont());
                int maxWidth = list.getWidth() - fm.stringWidth(name) - myRightComponentWidth - 36;
                int fullWidth = fm.stringWidth(locationString);
                if (fullWidth < maxWidth) {
                    SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, locationMatcher, bgColor, selected);
                } else {
                    int adjustedWidth = Math.max(locationString.length() * maxWidth / fullWidth - 1, 3);
                    locationString = StringUtil.trimMiddle(locationString, adjustedWidth);
                    SpeedSearchUtil.appendColoredFragmentForMatcher(locationString, renderer, SimpleTextAttributes.GRAYED_ATTRIBUTES, locationMatcher, bgColor, selected);
                }
                return true;
            }
        };
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

    @Override
    public @Nullable Object getDataForItem(@NotNull MyProjectNavigationItem element, @NotNull String dataId) {
        return null;
    }

    @Override
    public boolean processSelectedItem(@NotNull MyProjectNavigationItem selected, int modifiers, @NotNull String searchText) {
        if (isCommand(searchText, FINDER_COMMAND)) {
            RevealFileAction.openFile(new File(selected.getProjectPath()));
            return true;
        }
        if (selected.isOpen()) {
            active(selected.getProjectPath(), event);
            return true;
        }
        openProject(selected, event);
        return true;
    }

    private static void openProject(MyProjectNavigationItem selected, @NotNull AnActionEvent event) {
        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }
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
        if (action instanceof ActionGroup) {
            AnAction[] children = e.getUpdateSession().children((ActionGroup) action).toArray(AnAction.EMPTY_ARRAY);
//            AnAction[] children = ((ActionGroup) action).getChildren(null, ActionManager.getInstance());
            for (AnAction child : children) {
                if (!(child instanceof ProjectWindowAction windowAction)) {
                    continue;
                }
                if (projectLocation.equals(windowAction.getProjectLocation())) {
                    windowAction.setSelected(e, true);
                    Optional.ofNullable(e.getProject()).ifPresent(currentProject -> {
                        lastOpenPath = currentProject.getPresentableUrl();
                    });
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

    private static Optional<String> extractFirstWord(String input) {
        return !StringUtil.isEmptyOrSpaces(input) && input.contains(" ") ? Optional.of(input.split(" ")[0]) : Optional.empty();
    }

    private String filterString(String input) {
        return extractFirstWord(input)
                .filter((firstWord) -> this.FINDER_COMMAND.getCommandWithPrefix().startsWith(firstWord))
                .map((firstWord) -> input.substring(firstWord.length() + 1))
                .orElse(input);
    }

    private static boolean isCommand(String input, SearchEverywhereCommandInfo command) {
        return input != null && extractFirstWord(input)
                .map((firstWord) -> command.getCommandWithPrefix().startsWith(firstWord))
                .orElse(false);
    }

    @Override
    public void fetchWeightedElements(@NotNull String pattern, @NotNull ProgressIndicator progressIndicator,
                                      @NotNull Processor<? super FoundItemDescriptor<MyProjectNavigationItem>> consumer) {
        SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
        if (!getSearchProviderId().equals(seManager.getSelectedTabID())) {
            if (!isCommand(pattern, FINDER_COMMAND)) {
                return;
            }
        }

        if (!StringUtil.isEmptyOrSpaces(pattern)) {
            pattern = this.filterString(pattern);
        }

        if (foundProjectItemList == null) {
            // 展示的项目列表
            foundProjectItemList = new LinkedList<>();

            // idea打开过的项目
            Set<String> ideKnownProjectPathSet = new HashSet<>();

            // 已经打开的项目
            Arrays.stream(ProjectUtil.getOpenProjects())
                  .sorted(Comparator.comparing(Project::getName))
                  .peek(openProject -> {
                      if (openProject.getPresentableUrl().equals(lastOpenPath)) {
                          ideKnownProjectPathSet.add(openProject.getPresentableUrl());
                          foundProjectItemList.add(new MyProjectNavigationItem(openProject.getName(), openProject.getPresentableUrl(), true));
                      }
                  })
                  .toList()
                  .forEach(openProject -> {
                      if (!openProject.getPresentableUrl().equals(lastOpenPath)) {
                          ideKnownProjectPathSet.add(openProject.getPresentableUrl());
                          foundProjectItemList.add(new MyProjectNavigationItem(openProject.getName(), openProject.getPresentableUrl(), true));
                      }
                  });

            // 使用idea自带的最近项目, 不会包含已打开的项目
            RecentProjectsManagerBase recentProjectsManagerBase = (RecentProjectsManagerBase) RecentProjectsManager.getInstance();
            for (String recentPath : recentProjectsManagerBase.getRecentPaths()) {
                if (!ideKnownProjectPathSet.contains(recentPath)) {
                    ideKnownProjectPathSet.add(recentPath);
                    foundProjectItemList.add(new MyProjectNavigationItem(recentProjectsManagerBase.getProjectName(recentPath), recentPath, false));
                }
            }

            // 使用自定义目录查找项目
            Set<String> workspaces = myProjectSwitcherSetting.getProjectDirectoryList();
            Set<String> filterFolderList = myProjectSwitcherSetting.getFilterFolderList();
            workspaces.stream()
                      .flatMap(workspace -> searchProject(new File(workspace), workspaces, filterFolderList))
                      .sorted(Comparator.comparing(e -> -e.getLastModify()))
                      .filter(e -> !ideKnownProjectPathSet.contains(e.getProjectPath()))
                      .forEach(foundProjectItemList::add);
        }

        MinusculeMatcher minusculeMatcher = NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
        List<Pair<MyProjectNavigationItem, Integer>> openProjectItemList = foundProjectItemList.stream()
                                                                                               .filter(MyProjectNavigationItem::isOpen)
                                                                                               .filter(item -> minusculeMatcher.matches(item.getProjectName()))
                                                                                               .map(item -> Pair.of(item, minusculeMatcher.matchingDegree(item.getProjectName())))
                                                                                               .sorted((p1, p2) -> Integer.compare(p2.right, p1.right))
                                                                                               .collect(Collectors.toList());
        for (Pair<MyProjectNavigationItem, Integer> pair : openProjectItemList) {
            if (!consumer.process(new FoundItemDescriptor<>(pair.left, Integer.MAX_VALUE))) {
                return;
            }
        }

        for (MyProjectNavigationItem item : foundProjectItemList) {
            if (item.isOpen()) {
                continue;
            }
            if (minusculeMatcher.matches(item.getProjectName())) {
                if (!consumer.process(new FoundItemDescriptor<>(item, minusculeMatcher.matchingDegree(item.getProjectName())))) {
                    return;
                }
            } else if (myProjectSwitcherSetting.isEnableFilterPathInSearchEvery() && minusculeMatcher.matches(item.getProjectPath())) {
                if (!consumer.process(new FoundItemDescriptor<>(item, minusculeMatcher.matchingDegree(item.getProjectPath())))) {
                    return;
                }
            }
        }
    }

    private static final Set<String> IGNORE_DIR = Stream.of(
            "target", "out", "build", "idea-sandbox", "logs", "src", "node_modules", "docs"
    ).collect(Collectors.toSet());

    private Stream<MyProjectNavigationItem> searchProject(@NotNull File workspace, Set<String> workspaces, Set<String> filterFolderList) {
        if (workspace.isDirectory()) {
            File[] childFiles = workspace.listFiles((dir, name) -> !IGNORE_DIR.contains(name) && !name.startsWith("."));
            if (childFiles != null) {
                return Arrays.stream(childFiles)
                             .filter(file -> file.isDirectory() && !workspaces.contains(file.getAbsolutePath()))
                             .filter(file -> filterFolderList.stream().noneMatch(s -> file.getName().contains(s)))
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
