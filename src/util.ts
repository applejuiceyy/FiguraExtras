import path from "path";
import fs from "fs";

export function pathContainsPath(container: string, child: string): boolean {
    const relative = path.relative(container, child);
    return !relative.startsWith('..' + path.sep) && relative !== ".." && !path.isAbsolute(relative);
}

export function realPath(path: string) {
    return new Promise<string>(resolve => fs.realpath(path, (_, o) => resolve(o)));
}