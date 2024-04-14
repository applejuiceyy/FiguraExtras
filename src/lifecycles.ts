import * as vscode from 'vscode';

export function tryDoing<T>(promise: (token: vscode.CancellationToken) => Promise<T>, into: (result: Thenable<T>, token: vscode.CancellationToken) => Thenable<any> | void, options?: {curriedToken?: vscode.CancellationToken, cancel?: () => void, allowRetry?: boolean}) {
	options = options ?? {};
	let token = new vscode.CancellationTokenSource();
	if(options.curriedToken !== undefined) {
		options.curriedToken.onCancellationRequested(() => token.cancel());
	}
	into(new Promise(resolve => {
		promise(token.token)
		.then(t => token.token.isCancellationRequested || resolve(t))
		.catch(err => {
			token.cancel();
            if(options.allowRetry === false) {
                vscode.window.showErrorMessage(err.toString());
                return;
            }
			console.error(err);
			console.error(err.stack);
			vscode.window.showErrorMessage(err.toString(), "Retry")
			.then(option => {
				if(option === "Retry") {
					tryDoing(promise, into, options);
				}
				else {
					options.cancel?.();
				}
			});
		});
	}), token.token)?.then(() => token.cancel());
}

export async function doInformedPicking<T extends vscode.QuickPickItem>(func: (token: vscode.CancellationToken) => Promise<{type: "PICK", value: readonly T[], picked: (value: T) => Promise<any>} | {type: "INFO" | "WARN" | "ERROR", value: string} | {type: "OK"}>) {
	tryDoing(
		func,
		(result, token) => {
			let t = new vscode.CancellationTokenSource();
			token.onCancellationRequested(() => t.cancel());
            let after: ((value: T) => Promise<any>) | null = null;
			let thisFuture = new Promise<readonly T[]>(resolve => {
				result.then(stuff => {
					if(stuff.type !== "PICK") {
						if(stuff.type === "OK") {
							return;
						}
						t.cancel();
						let promise;
						switch(stuff.type) {
							case 'INFO':promise = vscode.window.showInformationMessage(stuff.value, "Retry"); break;
							case 'WARN':promise = vscode.window.showWarningMessage(stuff.value, "Retry"); break;
							case 'ERROR':promise = vscode.window.showErrorMessage(stuff.value, "Retry"); break;
							default: throw new Error("incorrect type");
						}
						promise.then(val => {
							if(val === "Retry") {
								doInformedPicking(func);
							}
						});
					}
					else {
                        after = stuff.picked;
						resolve(stuff.value);
					}
				});
			});
			vscode.window.showQuickPick(thisFuture, {}, t.token).then(value => {
                if(value === undefined) {return;}
                if(after === null) {return;}
                let a = after;
                tryDoing(() => a(value), () => {}, {
                    allowRetry: false
                });
            });
		}
	);
}