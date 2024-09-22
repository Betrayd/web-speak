
let wsConn: WebSocket;

let playersInScope = new Map<string, WebSpeakPlayer>();

let audioCtx = new AudioContext();
let listener = audioCtx.listener;

let ourPlayerID: string;

function packetizeData(type: string, data: string): string {
    return type + ";" + data;
}

function connectToWS(connectionAdress: string) {
    wsConn = new WebSocket(connectionAdress);

    wsConn.onopen = (v: Event) => {
        console.log("Connected to the game server");
    };

    wsConn.onerror = (v: Event) => {
        console.error("Connection error occured" + v);
    };

    wsConn.onclose = (v: CloseEvent) => {
        console.log("ws connection closed with reason: " + v.reason);
    };

    wsConn.onmessage = (msg: MessageEvent) => {
        let strData = msg.data as string;

        let data = strData.split(";", 2);

        switch (data[0]) {
            case "updateTransform": {
                interface PostionData { playerID: string, position: number[], rotation: number[] };
                let packetData = JSON.parse(data[1]) as PostionData;

                if (packetData.playerID == ourPlayerID) {
                    listener.positionX.value = packetData.position[0];
                    listener.positionY.value = packetData.position[1];
                    listener.positionZ.value = packetData.position[2];
                    listener.forwardX.value = packetData.rotation[0];
                    listener.forwardY.value = packetData.rotation[1];
                    listener.forwardZ.value = packetData.rotation[2];
                }
                else {
                    let con = playersInScope.get(packetData.playerID);
                    if (con != undefined && con.getLocalDescription == null && con.setRemoteDescription != null) {
                        con.setPosition(packetData.position, packetData.rotation);
                    }
                }
                break;
            }
            case "requestOffer": {
                interface UserID { playerID: string };
                let packetData = JSON.parse(data[1]) as UserID;

                let playerCreated = new WebSpeakPlayer(packetData.playerID);
                playerCreated.createOffer()
                    .then((offer) => {
                        wsConn.send(packetizeData("returnOffer", JSON.stringify(offer)));
                    });
                break;
            }
            case "handOffer": {
                interface Offer { playerID: string, offer: RTCSessionDescription };
                let packetData = JSON.parse(data[1]) as Offer;

                let playerCreated = new WebSpeakPlayer(packetData.playerID);
                playerCreated.createAnswer(packetData.offer)
                    .then((awnser) => {
                        wsConn.send(packetizeData("returnAnswer", JSON.stringify(awnser)));
                    });
                break;
            }
            case "handAnswer": {
                interface Answer { playerID: string, answer: RTCSessionDescription };
                let packetData = JSON.parse(data[1]) as Answer;

                let con = playersInScope.get(packetData.playerID);
                if (con == undefined) {
                    //something terrible has happened
                    break;
                }
                if (con.getLocalDescription == null) {
                    //something else horrible has happened

                }

                //set the description. We connected!
                con.setRemoteDescription

                break;
            }
            default:
                break;
        }
    };
}

function getRTCconfig(): RTCConfiguration {
    return {
        iceServers: [
            {
                urls: 'stun:stun2.1.google.com:19302'
            }
        ]
    };
}

class WebSpeakPlayer {
    //private playerID: string;
    private connection: RTCPeerConnection;
    private panner = new PannerNode(audioCtx, {
        panningModel: "HRTF",
        distanceModel: "linear",
        positionX: 0,
        positionY: 69420,
        positionZ: 0,
        orientationX: 0,
        orientationY: 0,
        orientationZ: 0,
        refDistance: 6,
        maxDistance: 26,
        rolloffFactor: 1,
        coneInnerAngle: 70,
        coneOuterAngle: 290,
        coneOuterGain: 0.4,
    });

    public constructor(playerID: string) {
        //this.playerID = playerID;
        this.connection = new RTCPeerConnection(getRTCconfig());
        playersInScope.set(playerID, this);
    }

    public getConnection(): RTCPeerConnection {
        return this.connection;
    }

    public async createOffer(): Promise<RTCSessionDescriptionInit> {
        let offer = await this.connection.createOffer();
        this.connection.setLocalDescription(offer);
        return offer;
    }

    public async createAnswer(offer: RTCSessionDescription): Promise<RTCSessionDescriptionInit> {
        this.connection.setRemoteDescription(offer);

        let answer = await this.connection.createAnswer();
        this.connection.setLocalDescription(answer);

        return answer;
    }

    public getLocalDescription(): RTCSessionDescription | null {
        return this.connection.localDescription;
    }

    public setRemoteDescription(description: RTCSessionDescription) {
        this.connection.setRemoteDescription(new RTCSessionDescription(description));
    }

    public setPosition(position: number[], rotation: number[]) {
        this.panner.positionX.value = position[0];
        this.panner.positionY.value = position[1];
        this.panner.positionZ.value = position[2];
        this.panner.orientationX.value = rotation[0];
        this.panner.orientationY.value = rotation[1];
        this.panner.orientationZ.value = rotation[2];
    }

    public remove() {

    }
}