
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

    wsConn.onmessage = async (msg: MessageEvent) => {
        let strData = msg.data as string;

        console.log("gotMessage: ");
        console.log(msg);

        let splitData = strData.split(";");
        let data = [splitData.shift(), splitData.join(';')];

        if (data[1] == undefined) {
            console.log("I hate typescript. This  is an error.");
            data[1] = "I'm not undefined this sucks";
        }

        switch (data[0]) {
            case "localPlayerInfo": {
                interface parseFail { playerID: string };
                let packetData = JSON.parse(data[1]);
                ourPlayerID = packetData.playerID;
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

                console.log("playerID: ");
                if (ourPlayerID != null && packetData.playerID == ourPlayerID) {
                    listener.positionX.value = packetData.pos[0];
                    listener.positionY.value = packetData.pos[1];
                    listener.positionZ.value = packetData.pos[2];
                    listener.forwardX.value = packetData.rot[0];
                    listener.forwardY.value = packetData.rot[1];
                    listener.forwardZ.value = packetData.rot[2];
                    console.log("usPos: " + packetData.pos);
                }
                else {
                    let con = playersInScope.get(packetData.playerID);
                    console.log("trying updating players: ");
                    console.log(con);
                    if (con != undefined && con.getLocalDescription != null) {
                        con.setPosition(packetData.pos, packetData.rot);
                    }
                }
                break;
            }
            case "requestOffer": {
                //data[1] just contains a playerID in this case
                let strData = data[1];
                let playerCreated = await WebSpeakPlayer.create(strData);
                playerCreated.createOffer()
                    .then((offer) => {
                        interface PositionData { playerID: string, rtcSessionDescription: RTCSessionDescriptionInit };
                        let returnData: PositionData = {
                            playerID: strData,
                            rtcSessionDescription: offer
                        };
                        console.log("Created packet offer:");
                        console.log(packetizeData("returnOffer", JSON.stringify(returnData)));
                        wsConn.send(packetizeData("returnOffer", JSON.stringify(returnData)));
                    });
                break;
            }
            case "handOffer": {
                console.log("hand offer: ");
                console.log(data[1]);
                interface Offer { playerID: string, rtcSessionDescription: RTCDescriptionInterface };
                let packetData = JSON.parse(data[1]) as Offer;

                let playerCreated = await WebSpeakPlayer.create(packetData.playerID);
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
                urls: ['stun:stun1.l.google.com:19302', 'stun:stun2.l.google.com:19302'],
            },
        ],
        iceCandidatePoolSize: 10,
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
        positionY: 0,
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

    private constructor() {
        //this.playerID = playerID;
        this.connection = new RTCPeerConnection(getRTCconfig());
    }

    public static async create(otherPlayerID: string): Promise<WebSpeakPlayer> {
        let createP = new WebSpeakPlayer();

        let source: AudioBufferSourceNode;
        fetch("panner-node_viper.ogg")
        .then((response) => response.arrayBuffer())
        .then((downloadedBuffer) => audioCtx.decodeAudioData(downloadedBuffer))
        .then((decodedBuffer) => {
          source = new AudioBufferSourceNode(audioCtx, {
            buffer: decodedBuffer,
          });
          source.connect(createP.panner);
          createP.panner.connect(audioCtx.destination);
          source.loop = true;
          source.start(0);
    }).catch((e) => {
        console.error(`Error while preparing the audio data ${e.err}`);
      });

        await navigator.mediaDevices.getUserMedia({
            audio: true,
            video: false,
        }).then((stream) => {
            if (stream.active != false) {
                console.log("all audio tracks on this device: ");
                console.log(stream.getAudioTracks());
                
                stream.getTracks().forEach((track) => {
                    createP.connection.addTrack(track, stream);
                  });
            }
        });
        createP.connection.ontrack = ((ev) => {
            console.log("new track added!");
            if (ev.track.kind == "audio") {
                console.log("audio track");
                //console.log(ev);
                //let source = new MediaStreamAudioSourceNode(audioCtx, {
                //    mediaStream: ev.streams[0],
                //});
                //ev.track.getSettings();
                //source.connect(createP.panner);
                //createP.panner.connect(audioCtx.destination);
            }
        });
        createP.connection.onicecandidate = function (event) {
            console.log("trying to send an ice canidate");
            if (event.candidate) {
                interface IceCanidate { playerID: string, rtcSessionDescription: RTCIceCandidate };
                let output: IceCanidate = {
                    playerID: otherPlayerID,
                    rtcSessionDescription: event.candidate
                };
                console.log("NON NULL ICE CANIDATESEND");
                wsConn.send(packetizeData("returnIce", JSON.stringify(output)));
            }
        };

        console.log("rtcPeer:");
        console.log(createP.connection);
        playersInScope.set(otherPlayerID, createP);
        return createP;
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
        if (this.connection == null) {
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
        console.log("otherPos: " + position);
    }

    public remove() {

    }
}