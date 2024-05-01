import path from "path";
import fs from "fs";
import * as os from "os";

export function pathContainsPath(container: string, child: string): boolean {
    const relative = path.relative(container, child);
    return !relative.startsWith('..' + path.sep) && relative !== ".." && !path.isAbsolute(relative);
}

export function realPath(path: string) {
    return new Promise<string>(resolve => fs.realpath(path, (_, o) => resolve(o)));
}

export function IPCPath(name: string) {
    return os.platform() === "win32" ? "\\\\.\\pipe\\" + name : "/tmp/unix_domain/" + name.replaceAll("\\", "/") + ".sock";
}