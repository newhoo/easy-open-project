package io.github.newhoo.project.setting;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.io.File;

public class ProjectSwitchSettingForm extends JPanel {

    private final DefaultListModel<String> defaultModel = new DefaultListModel<>();

    public ProjectSwitchSettingForm() {
        init();
    }

    private void init() {
//        this.setBorder(BorderFactory.createEmptyBorder());
        this.setLayout(new GridLayoutManager(1, 1));

        JBList<String> jbList = new JBList<>(defaultModel);
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(jbList);
        decorator.disableUpDownActions();
        decorator.setAddAction(button -> {
            String currentFileLocation = ProjectUtil.getBaseDir();

            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            VirtualFile toSelect = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(currentFileLocation));

            VirtualFile file = FileChooser.chooseFile(descriptor, null, toSelect);
            if (file != null && file.isDirectory() && file.isWritable()) {
                String filePath = FileUtil.toSystemDependentName(file.getPath());
                if (!defaultModel.contains(filePath)) {
                    defaultModel.addElement(filePath);
                }
            }
        });
        decorator.setRemoveAction(button -> ListUtil.removeSelectedItems(jbList));
        JPanel panel = decorator.createPanel();
        panel.setBorder(IdeBorderFactory.createTitledBorder("Project Directory", false));

        this.add(panel,
                new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
    }

    public DefaultListModel<String> getDefaultModel() {
        return defaultModel;
    }
}
