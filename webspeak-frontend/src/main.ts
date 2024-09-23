
let wsConn: WebSocket;

let playersInScope = new Map<string, WebSpeakPlayer>();

let audioCtx: AudioContext;
let listener: AudioListener;

let ourPlayerID: string;

export function helloWorld() {
    start();
    // put your code in here
}

function packetizeData(type: string, data: string): string {
    return type + ";" + data;
}

export function start() {
    let urlParams = new URLSearchParams(window.location.search);
    let serverAdress = urlParams.get('serverAdress');
    let sessionID = urlParams.get('sessionID');

    audioCtx = new AudioContext();
    listener = audioCtx.listener;

    console.log("serverAdress: " + serverAdress);
    console.log("sessionID: " + sessionID);
    if (serverAdress != null && sessionID != null) {
        connectToWS(serverAdress + "/connect?id=" + sessionID);
    }
}

function connectToWS(connectionAdress: string) {
    wsConn = new WebSocket(connectionAdress);

    wsConn.onopen = (v: Event) => {
        console.log("Connected to the game server");
    };

    wsConn.onerror = (v: Event) => {
        console.error("Connection error occured: ");
        console.log(v);
    };

    wsConn.onclose = (v: CloseEvent) => {
        console.log("ws connection closed with reason: ");
        console.log(v);
    };

    wsConn.onmessage = (msg: MessageEvent) => {
        let strData = msg.data as string;

        console.log("gotMessage: ");
        console.log(msg);

        let data = strData.split(";", 2);

        switch (data[0]) {
            case "localPlayerInfo": {
                ourPlayerID = data[1];
                console.log("set our player ID: " + ourPlayerID);
                break;
            }
            case "handIce": {
                let packetData = JSON.parse(data[1]);

                let con = playersInScope.get(packetData.playerID);
                if (con == undefined) {
                    //something terrible has happened
                    break;
                }
                if (con.getLocalDescription == null) {
                    //something else horrible has happened

                }

                console.log("got ice canidate!");
                con.getConnection().addIceCandidate(packetData.rtcSessionDescription);

                break;
            }
            case "updateTransform": {
                interface PostionData { playerID: string, pos: number[], rot: number[] };
                let packetData = JSON.parse(data[1]) as PostionData;

                if (ourPlayerID != null && packetData.playerID == ourPlayerID) {
                    listener.positionX.value = packetData.pos[0];
                    listener.positionY.value = packetData.pos[1];
                    listener.positionZ.value = packetData.pos[2];
                    listener.forwardX.value = packetData.rot[0];
                    listener.forwardY.value = packetData.rot[1];
                    listener.forwardZ.value = packetData.rot[2];
                }
                else {
                    let con = playersInScope.get(packetData.playerID);
                    if (con != undefined && con.getLocalDescription != null) {
                        con.setPosition(packetData.pos, packetData.rot);
                    }
                }
                break;
            }
            case "requestOffer": {
                //data[1] just contains a playerID in this case
                let playerCreated = new WebSpeakPlayer(data[1]);
                playerCreated.createOffer()
                    .then((offer) => {
                        interface PositionData { playerID: string, rtcSessionDescription: RTCSessionDescriptionInit };
                        let returnData: PositionData = {
                            playerID: data[1],
                            rtcSessionDescription: offer
                        };
                        console.log(offer);
                        wsConn.send(packetizeData("returnOffer", JSON.stringify(returnData)));
                    });
                break;
            }
            case "handOffer": {
                interface Offer { playerID: string, rtcSessionDescription: RTCDescriptionInterface };
                let packetData = JSON.parse(data[1]) as Offer;

                let playerCreated = new WebSpeakPlayer(packetData.playerID);
                let x = new RTCDesc(packetData.rtcSessionDescription);
                playerCreated.createAnswer(x)
                    .then((awnser) => {
                        interface PositionData { playerID: string, rtcSessionDescription: RTCSessionDescriptionInit };
                        let returnData: PositionData = {
                            playerID: packetData.playerID,
                            rtcSessionDescription: awnser
                        };
                        wsConn.send(packetizeData("returnAnswer", JSON.stringify(returnData)));
                        console.log("sent awnser: ");
                        console.log(awnser);
                    });
                break;
            }
            case "handAnswer": {
                interface Answer { playerID: string, rtcSessionDescription: RTCDescriptionInterface };
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
                con.setRemoteDescription(new RTCDesc(packetData.rtcSessionDescription));

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

interface RTCDescriptionInterface { sdp: string, type: RTCSdpType };
class RTCDesc implements RTCSessionDescriptionInit {
    public sdp?: string | undefined;
    public type: RTCSdpType;

    constructor(x: RTCDescriptionInterface) {
        this.sdp = x.sdp;
        this.type = x.type;
    }
};

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

    public constructor(otherPlayerID: string) {
        //this.playerID = playerID;
        this.connection = new RTCPeerConnection(getRTCconfig());

        navigator.mediaDevices.getUserMedia({
            audio: true,
            video: false,
          }).then((stream) => {
            if(stream.active != false)
            {
                console.log("all audio tracks on this device: ");
                console.log(stream.getAudioTracks());
                this.connection.addTrack(stream.getAudioTracks()[0]);
            }
        });
        this.connection.addEventListener(
            "track",
            (e) => {
                console.log("differentAddtrackworked");
            },
            false,
          );
        this.connection.ontrack = ((ev) => 
            {
                console.log("new track added!");
                let source = new MediaStreamAudioSourceNode(audioCtx, {
                    mediaStream: ev.streams[0],
                  });
                source.connect(this.panner);
                this.panner.connect(audioCtx.destination);
            });
        this.connection.onicecandidate = function (event) { 
            if (event.candidate) { 
                interface IceCanidate {playerID : string, rtcSessionDescription : RTCIceCandidate};
                let output: IceCanidate = {
                    playerID: otherPlayerID,
                    rtcSessionDescription: event.candidate
                };
                wsConn.send(packetizeData("returnIce", JSON.stringify(output)));
            } 
         }; 
            
        console.log("rtcPeer:");
        console.log(this.connection);
        playersInScope.set(otherPlayerID, this);
    }

    public getConnection(): RTCPeerConnection {
        return this.connection;
    }

    public async createOffer(): Promise<RTCSessionDescriptionInit> {
        let offer = await this.connection.createOffer();
        this.connection.setLocalDescription(offer);
        return offer;
    }

    public async createAnswer(offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> {
        this.setRemoteDescription(offer);

        let answer = await this.connection.createAnswer();
        this.connection.setLocalDescription(answer);

        return answer;
    }

    public getLocalDescription(): RTCSessionDescription | null {
        return this.connection.localDescription;
    }

    public setRemoteDescription(description: RTCSessionDescriptionInit) {
        if(this.connection == null)
        {
            console.error("something really bad happened. Expect everything to be broken. The end is nigh.");
        }
        this.connection.setRemoteDescription(description);
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