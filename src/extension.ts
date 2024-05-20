// The module 'vscode' contains the VS Code extensibility API
// Import the module and reference it with the alias vscode in your code below
import * as vscode from 'vscode';
import { Receptionist } from './discovery/receptionist';
import { doInformedPicking } from './lifecycles';
import * as net from "net";
import { pathContainsPath, realPath, IPCPath } from './util';
import path from 'path';

async function getAvatarFolder(): Promise<{type: "OK", value: vscode.WorkspaceFolder} | {type: "ERROR", value: string}> {
	if(vscode.workspace.workspaceFolders === undefined) {
		return {
			type: "ERROR",
			value: "Workspace is not opened"
		};
	}
	let rootFolder = vscode.workspace.workspaceFolders[0];

	if(rootFolder.uri.scheme !== "file") {
		return {
			type: "ERROR",
			value: "Not a workspace in the filesystem"
		};
	}
	let avatarjson;
	try {
		avatarjson = await vscode.workspace.fs.readFile(vscode.Uri.joinPath(rootFolder.uri, "avatar.json"));
	}
	catch(error) {
		return {
			type: "ERROR",
			value: "avatar.json is not defined"
		};
	}
	
	try {
		JSON.parse(new TextDecoder().decode(avatarjson));
	}
	catch(error: any) {
		return {
			type: "ERROR",
			value: "Error whilst reading avatar.json:" + error.toString()
		};
	}
	return {
		type: "OK",
		value: rootFolder
	}
}

export function activate(context: vscode.ExtensionContext) {
	net.createServer().on("error", console.log).on("listening", console.log).listen("\\\\?\\pipe\\thingthing");

	context.subscriptions.push(vscode.commands.registerCommand('figuraextras.debugOnMCClient', () => {
		doInformedPicking<{id: string} & vscode.QuickPickItem>(async () => {
			let receptionist;
			let r = await getAvatarFolder();
			if(r.type === "ERROR") {
				return r;
			}
			let rootFolder = r.value;
			receptionist = await Receptionist.getOrCreateReceptionist();

			if(receptionist === null) {
				return {
					type: "INFO", value: "No minecraft clients with FiguraExtras available"
				};
			}

			let discovery = await receptionist.listClients();
			let perId: {[k: string]: typeof discovery[0]} = Object.create(null);
			let idx = 0;
			let ret: ({id: string} & vscode.QuickPickItem)[] = [];

			if(discovery !== null) {
				for (let i = 0; i < discovery.length; i++) {
					const element = discovery[i];
					if(pathContainsPath(await realPath(path.join(element.figuraPath, "avatars")), rootFolder.uri.fsPath)) {
						idx = i;
						ret.push({
							label: "Minecraft version " + element.version,
							id: element.instanceId,
							detail: element.minecraftPath + (element.world === null ? "   (Not in a world)" : ("   " + element.world.name + " (" + (element.world.singleplayer ? "singleplayer" : "multiplayer") + " server)"))
						});
						perId[element.instanceId] = element;
					}
				}
			}

			if(ret.length === 0) {
				return {
					type: "INFO", value: "No minecraft clients covering this avatar"
				};
			}
			//if(ret.length === 1) {
			//	return {type: "OK"};
			//}

			return {
				type: "PICK",
				value: ret,
				async picked(value) {
					if(perId[value.id].world === null) {
						doInformedPicking<vscode.QuickPickItem>(async () => {
							let receptionist = await Receptionist.getOrCreateReceptionist();
							if(receptionist === null) {
								return {
									type: "ERROR",
									value: "No minecraft clients with FiguraExtras available"
								};
							}

							let worlds = await receptionist.getClientWorlds(value.id);

							return {
								type: "PICK",
								value: worlds.map(v => {return {label: v.name};}),
								
								async picked(world) {
									doInformedPicking<{id: string} & vscode.QuickPickItem>(async () => {
										let receptionist = await Receptionist.getOrCreateReceptionist();
										if(receptionist === null) {
											return {
												type: "ERROR",
												value: "No minecraft clients with FiguraExtras available"
											};
										}
										
										await receptionist.joinSinglePlayerWorld(value.id, world.label);

										vscode.debug.startDebugging(rootFolder, {
											"type": "figura",
											"request": "attach",
											"name": "Debug",
											"id": value.id
										});
			
										return {
											type: "OK"
										};
									});
								},
							};
						});
					}
					else {
						vscode.debug.startDebugging(rootFolder, {
							"type": "figura",
							"request": "attach",
							"name": "Debug",
							"id": value.id
						});
					}
				},
			};
		});
	}));

	
	context.subscriptions.push(vscode.debug.registerDebugConfigurationProvider("figura", {
		async resolveDebugConfiguration(folder, debugConfiguration, token) {
			let o = await getAvatarFolder();
			if(o.type === "ERROR") {
				throw new Error(o.value);
			}
			debugConfiguration.avatarPath = o.value.uri.path;
			return debugConfiguration;
		},
	}));

	context.subscriptions.push(vscode.debug.registerDebugAdapterDescriptorFactory("figura", {
		
		async createDebugAdapterDescriptor(session, executable) {
			if(session.configuration.request === "attach") {
				let o = await getAvatarFolder();
				if(o.type === "ERROR") {
					return null;
				}
				let path = o.value;
				let receptionist = await Receptionist.getOrCreateReceptionist();
				if(receptionist === null) {
					throw new Error("No minecraft clients");
				}
				let dapPath = await receptionist.startDAP(session.configuration.id, path.uri.path);
				return new vscode.DebugAdapterNamedPipeServer(IPCPath(dapPath));
			}
		}
	}));
}

// This method is called when your extension is deactivated
export function deactivate() { }
