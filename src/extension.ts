import * as vscode from 'vscode';

import path = require('path');
import fs = require('fs');

export function activate(context: vscode.ExtensionContext) {
  const disposable = vscode.commands.registerCommand(`easyOpenProject.openProject`, () => {
    let projectDirs = <string[]>vscode.workspace.getConfiguration('easyOpenProject')['projectFolders']
    projectDirs = [...new Set(projectDirs)]

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
      projects.push(...readProjects(dirpath, projectDirs))
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
  );
  context.subscriptions.push(disposable);

  const disposable1 = vscode.commands.registerCommand(`easyOpenProject.addProjectFolders`, () => {
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
  );
  context.subscriptions.push(disposable1);
}

function openProject(projects: vscode.QuickPickItem[]) {
  projects.sort((a, b) => a.label.toLowerCase() < b.label.toLowerCase() ? -1 : 1)
  vscode.window.showQuickPick(projects,
    {
      placeHolder: "Select project to open",
      canPickMany: false,
    }
  ).then((res: vscode.QuickPickItem | undefined) => {
    if (!res) return;
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
          projectDirs.forEach(fsPath => {
            projects.push(...readProjects(fsPath, projectDirs))
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

function readProjects(dir: string, allDirs: string[]): vscode.QuickPickItem[] {
  const projects: vscode.QuickPickItem[] = []
  fs.readdirSync(dir, { withFileTypes: true }).filter(dirent => {
    return dirent.isDirectory() && !dirent.name.startsWith('.');
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

export function deactivate() { }
