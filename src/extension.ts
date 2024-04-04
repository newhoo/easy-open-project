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

  if (projects.length < 1) {
    projects.push({
      "label": "Add a folder firstly",
      "description": "",
      iconPath: new vscode.ThemeIcon("new-folder", undefined)
    })
  }

  openProject(projects);
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
 * do open project
 * @param projects vscode pick items
 */
function openProject(projects: vscode.QuickPickItem[]) {
  projects.sort((a, b) => a.label.toLowerCase() < b.label.toLowerCase() ? -1 : 1)
  vscode.window.showQuickPick(projects,
    {
      placeHolder: "Select project to open",
      canPickMany: false,
    }
  ).then((res: vscode.QuickPickItem | undefined) => {
    if (!res) return;
    // Add folder when project list is empty
    if (res.label === 'Add a folder firstly') {
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
          if (projects) {
            openProject(projects);
          }
        }
      })
      return
    }

    vscode.commands.executeCommand('vscode.openFolder', vscode.Uri.file(res.description!), {
      forceNewWindow: true,
    }).then(() => ({}),
      () => {
        vscode.window.showInformationMessage(`Could not open the project: ${res.description}`)
      }
    );
  })
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
    return dirent.isDirectory() && !dirent.name.startsWith('.') && !filterFolderNames.includes(dirent.name);
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