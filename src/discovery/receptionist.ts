import z from "zod";
import * as rpc from 'vscode-jsonrpc/node';
import * as net from "net";

let receptionist: Receptionist | null = null;


const PIPE_PATH = "\\\\.\\pipe\\figuraextras\\receptionist";

const world = z.object({
    name: z.string(),
    singleplayer: z.boolean()
});

const discoverySchema = z.array(
    z.object({
        version: z.string(),
        minecraftPath: z.string(),
        figuraPath: z.string(),
        id: z.string(),
        world: z.union([
            z.null(),
            world
        ])
    })
);

const getClientWorlds = z.array(world);

const startDAP = z.string();

export class Receptionist {
    connection: rpc.MessageConnection;
    beenClosed: boolean;


    constructor(connection: rpc.MessageConnection) {
        this.connection = connection;
        this.beenClosed = false;
        this.connection.onClose(() => this.beenClosed = true);
        connection.listen();
    }

    listClients(): Promise<z.infer<typeof discoverySchema>> {
        return this.connection.sendRequest("getClients").then(thing => discoverySchema.parse(thing));
    }

    getClientWorlds(id: string): Promise<z.infer<typeof getClientWorlds>> {
        return this.connection.sendRequest("getClientWorlds", id).then(thing => getClientWorlds.parse(thing));
    }

    joinSinglePlayerWorld(id: string, name: string): Promise<void> {
        return this.connection.sendRequest("joinSinglePlayerWorld", id, name);
    }

    startDAP(id: string, avatarPath: string): Promise<z.infer<typeof startDAP>> {
        return this.connection.sendRequest("startDAP", id, avatarPath).then(thing => startDAP.parse(thing));
    }

    closed() {
        return this.beenClosed;
    }

    static async createReceptionist(): Promise<Receptionist | null> {
        try {
            return await new Promise((resolve, reject) => {
                try {
                    let client = net.connect({
                        path: PIPE_PATH,
                        timeout: 1000
                    });

                    client.on("error", reject);

        
                    let connection = rpc.createMessageConnection(
                        new rpc.SocketMessageReader(client),
                        new rpc.SocketMessageWriter(client),
                        undefined, {
        
                        }
                    );
                
        
                    setTimeout(() => resolve(new Receptionist(connection)), 100);
                }
                catch(e) {
                    reject(e);
                }
            });
            }
        catch(e) {
            return null;
        }
    }

    static async getOrCreateReceptionist() {
        if(receptionist === null || receptionist.closed()) {
            receptionist = await Receptionist.createReceptionist();
        }
        return receptionist;
    }
}