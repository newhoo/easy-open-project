package io.github.newhoo.project.setting;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.File;

public class ProjectSwitchSettingForm extends JPanel {

    private final DefaultListModel<String> folderModel = new DefaultListModel<>();
    private final DefaultListModel<String> filterFolderModel = new DefaultListModel<>();

    public ProjectSwitchSettingForm() {
        init();
    }

    private void init() {
//        this.setBorder(BorderFactory.createEmptyBorder());
        this.setLayout(new GridLayoutManager(2, 1));

        {
            JBList<String> jbList = new JBList<>(folderModel);
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(jbList);
            decorator.disableUpDownActions();
            decorator.setAddAction(button -> {
                String currentFileLocation = ProjectUtil.getBaseDir();

                FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                VirtualFile toSelect = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(currentFileLocation));

                VirtualFile file = FileChooser.chooseFile(descriptor, null, toSelect);
                if (file != null && file.isDirectory() && file.isWritable()) {
                    String filePath = FileUtil.toSystemDependentName(file.getPath());
                    if (!folderModel.contains(filePath)) {
                        folderModel.addElement(filePath);
                    }
                }
            });
            decorator.setRemoveAction(button -> ListUtil.removeSelectedItems(jbList));
            JPanel panel = decorator.createPanel();
            panel.setBorder(IdeBorderFactory.createTitledBorder("Project Directory", false));

            this.add(panel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
        }
        {
            JBList<String> jbList = new JBList<>(filterFolderModel);
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(jbList);
            decorator.disableUpDownActions();
            decorator.setAddAction(button -> {
                String folderName = Messages.showInputDialog("Input project name keyword such as docs.", "Filter Project Name", null);
                if (StringUtils.isNoneEmpty(folderName) && !filterFolderModel.contains(folderName)) {
                    filterFolderModel.addElement(folderName);
                }
            });
            decorator.setRemoveAction(button -> ListUtil.removeSelectedItems(jbList));
            JPanel panel = decorator.createPanel();
            panel.setBorder(IdeBorderFactory.createTitledBorder("Filter out Project by Keyword", false));

            this.add(panel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
        }
    }

    public DefaultListModel<String> getFolderModel() {
        return folderModel;
    }

    public DefaultListModel<String> getFilterFolderModel() {
        return filterFolderModel;
    }
}
