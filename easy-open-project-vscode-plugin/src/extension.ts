import * as vscode from 'vscode';

import path = require('path');
import fs = require('fs');

export function activate(context: vscode.ExtensionContext) {
  context.subscriptions.push(vscode.commands.registerCommand(`easyOpenProject.openProject`, openProjectCommand));
  context.subscriptions.push(vscode.commands.registerCommand(`easyOpenProject.addProjectFolders`, addProjectFoldersCommand));
}

export function deactivate() { }

/**
 * vscode command: openProject
 */
function openProjectCommand() {
  let projectDirs = <string[]>vscode.workspace.getConfiguration('easyOpenProject')['projectFolders']
  projectDirs = [...new Set(projectDirs)]
  let filterFolderNames = <string[]>vscode.workspace.getConfiguration('easyOpenProject')['filterFolderNames']
  filterFolderNames = [...new Set(filterFolderNames)]

  const projects: vscode.QuickPickItem[] = []
  projectDirs.forEach(dirpath => {
    if (!fs.existsSync(dirpath)) {
      vscode.window.showInformationMessage(`Remove noexist path: ${dirpath} ?`, 'Yes', 'No').then(choice => {
        if (choice === 'Yes') {
          const arr = (<string[]>vscode.workspace.getConfiguration('easyOpenProject')['projectFolders']).filter(s => s !== dirpath)
          vscode.workspace.getConfiguration().update('easyOpenProject.projectFolders', arr, true)
        }
      })
      return
    }
    projects.push(...readProjects(dirpath, projectDirs, filterFolderNames))
  });

  openProject(projects, true);
}

/**
 * vscode command: addProjectFolders
 */
function addProjectFoldersCommand() {
  vscode.window.showOpenDialog({
    canSelectFiles: false,
    canSelectFolders: true,
    canSelectMany: true
  }).then(uris => {
    if (uris) {
      uris.forEach(u => {
        const arr = (<string[]>vscode.workspace.getConfiguration('easyOpenProject')['projectFolders'])
        arr.push(u.fsPath);
        vscode.workspace.getConfiguration().update('easyOpenProject.projectFolders', arr, true)
      });
    }
  })
}

/**
 * open project
 * @param projects vscode pick items
 */
function openProject(projects: vscode.QuickPickItem[], showAddFolderItem: boolean) {
  let placeHolder = "Select project to open";
  if (projects.length > 1) {
    projects.sort((a, b) => a.label.toLowerCase() < b.label.toLowerCase() ? -1 : 1)
  } else if (projects.length === 0 && showAddFolderItem) {
    placeHolder = "Not found project, add folders firstly."
    projects.push({
      "label": "Add Folder",
      "description": "",
      iconPath: new vscode.ThemeIcon("new-folder", undefined)
    })
  }

  vscode.window.showQuickPick(projects,
    {
      placeHolder: placeHolder,
      canPickMany: false,
    }
  ).then((res: vscode.QuickPickItem | undefined) => {
    if (!res) return;

    // Add folder when project list is empty
    if (res.label === 'Add Folder') {
      vscode.window.showOpenDialog({
        canSelectFiles: false,
        canSelectFolders: true,
        canSelectMany: true
      }).then(uris => {
        if (uris) {
          const projectDirs: string[] = []
          projects = []
          uris.forEach(u => {
            const arr = (<string[]>vscode.workspace.getConfiguration('easyOpenProject')['projectFolders'])
            arr.push(u.fsPath);
            vscode.workspace.getConfiguration().update('easyOpenProject.projectFolders', arr, true)

            projectDirs.push(u.fsPath)
          });

          let filterFolderNames = <string[]>vscode.workspace.getConfiguration('easyOpenProject')['filterFolderNames']
          filterFolderNames = [...new Set(filterFolderNames)]
          projectDirs.forEach(fsPath => {
            projects.push(...readProjects(fsPath, projectDirs, filterFolderNames))
          })

          if (projects.length > 0) {
            openProject(projects, false);
          }
        }
      })
      return
    }

    const projectPath = res.description!;
    const forceNewWindow: boolean = vscode.workspace.getConfiguration('easyOpenProject')['openInNewWindow']
    if (forceNewWindow) {
      doOpenProject(projectPath, true);
      return
    }
    if (vscode.workspace.workspaceFile == undefined && (vscode.workspace.workspaceFolders == undefined || vscode.workspace.workspaceFolders.length < 1) && vscode.workspace.textDocuments.length < 1) {
      doOpenProject(projectPath, false);
      return
    }

    // select by user
    const openOperations: vscode.QuickPickItem[] = [
      {
        "label": "Yes"
      },
      {
        "label": "No"
      }
    ]
    vscode.window.showQuickPick(openOperations,
      {
        placeHolder: "Open project in new window",
        canPickMany: false,
      }
    ).then((res: vscode.QuickPickItem | undefined) => {
      if (!res) return;
      doOpenProject(projectPath, "Yes" === res.label);
    })
  })
}

/**
 * do open project
 * @param projectPath 
 * @param forceNewWindow 
 */
function doOpenProject(projectPath: string, forceNewWindow: boolean) {
  vscode.commands.executeCommand('vscode.openFolder', vscode.Uri.file(projectPath), {
    forceNewWindow: forceNewWindow,
  }).then(() => ({}),
    () => {
      vscode.window.showInformationMessage(`Could not open the project: ${projectPath}`)
    }
  );
}

/**
 * read project folder item from given folder path
 * @param dir folder path contains project folders
 * @param allDirs all absulute paths in the setting
 * @param filterFolderNames filter folder name
 * @returns vscode pick items
 */
function readProjects(dir: string, allDirs: string[], filterFolderNames: string[]): vscode.QuickPickItem[] {
  const projects: vscode.QuickPickItem[] = []
  fs.readdirSync(dir, { withFileTypes: true }).filter(dirent => {
    return dirent.isDirectory() && !dirent.name.startsWith('.') && filterFolderNames.filter((keyword) => dirent.name.indexOf(keyword) >= 0).length <= 0;
  }).forEach(function (dirent) {
    var filePath = path.join(dirent.path, dirent.name);
    if (!allDirs.includes(filePath)) {
      projects.push({
        "label": dirent.name,
        "description": filePath,
        iconPath: vscode.ThemeIcon.Folder
      })
    }
  })
  return projects
}