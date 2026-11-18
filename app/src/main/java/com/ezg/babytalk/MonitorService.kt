package com.ezg.babytalk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.TimeUnit

class MonitorService : Service() {
    // Render service URL configured in render.yaml; use WSS in production.
    private val signalUrl = "wss://ezg-baby-talk-signal.onrender.com"
    private val http = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
    private var ws: WebSocket? = null
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var role = "baby"
    private var room = ""
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var stopping = false

    override fun onCreate() { super.onCreate(); makeChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopping = false
        role = intent?.getStringExtra("role") ?: "baby"
        room = intent?.getStringExtra("room") ?: ""
        startForeground(12, makeNotification("Oda sunucusuna bağlanıyor"))
        initWebRtc()
        connect()
        return START_NOT_STICKY
    }

    private fun makeChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("ezg_monitor", "EZG BABY TALK", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun makeNotification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "ezg_monitor")
            .setContentTitle("EZG BABY TALK").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off).setOngoing(true).build()
        else Notification.Builder(this).setContentTitle("EZG BABY TALK").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off).setOngoing(true).build()
    }

    private fun status(text: String) {
        getSystemService(NotificationManager::class.java).notify(12, makeNotification(text))
    }

    private fun initWebRtc() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
        val adm = JavaAudioDeviceModule.builder(this).createAudioDeviceModule()
        factory = PeerConnectionFactory.builder().setAudioDeviceModule(adm).createPeerConnectionFactory()
        adm.release()
        if (role == "parent") {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            run { audio.isSpeakerphoneOn = true }
        }
    }

    private fun connect() {
        val req = Request.Builder().url(signalUrl).build()
        ws = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("type","join").put("room",room).put("role",role).toString())
                status("Oda $room • eşleşme bekleniyor")
            }
            override fun onMessage(webSocket: WebSocket, text: String) { onSignal(JSONObject(text)) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                status("Bağlantı kesildi; yeniden bağlanılıyor")
                peer?.close(); peer?.dispose(); peer = null
                handler.postDelayed({ if (!stopping) connect() }, 5000)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { status("Bağlantı kapandı") }
        })
    }

    private fun onSignal(msg: JSONObject) {
        when (msg.optString("type")) {
            "joined" -> status("Oda $room • diğer telefon bekleniyor")
            "peer-joined" -> if (role == "baby") createPeer(true)
            "signal" -> {
                val data = msg.optJSONObject("data") ?: return
                when (data.optString("kind")) {
                    "offer" -> {
                        createPeer(false)
                        val offer = SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp"))
                        peer?.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() { createAnswer() }
                            override fun onSetFailure(error: String) { status("Teklif alınamadı: $error") }
                            override fun onCreateSuccess(desc: SessionDescription) {}
                            override fun onCreateFailure(error: String) {}
                        }, offer)
                    }
                    "answer" -> peer?.setRemoteDescription(simpleSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")))
                    "ice" -> peer?.addIceCandidate(IceCandidate(data.optString("id"), data.optInt("label"), data.optString("candidate")))
                }
            }
            "peer-left" -> status("Diğer telefon bağlantıyı kapattı")
            "error" -> status(msg.optString("message","Eşleşme hatası"))
        }
    }

    private fun createPeer(makeOffer: Boolean) {
        if (peer != null) return
        val servers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        peer = factory?.createPeerConnection(PeerConnection.RTCConfiguration(servers), object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) { status("Ses bağlantısı: $state") }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidate(candidate: IceCandidate) {
                send(JSONObject().put("kind","ice").put("id",candidate.sdpMid ?: "").put("label",candidate.sdpMLineIndex).put("candidate",candidate.sdp))
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onDataChannel(channel: DataChannel) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
        })
        if (role == "baby") {
            audioSource = factory?.createAudioSource(MediaConstraints())
            audioTrack = factory?.createAudioTrack("ezg-baby-microphone", audioSource)
            audioTrack?.setEnabled(true)
            audioTrack?.let { peer?.addTrack(it, listOf("ezg-baby-stream")) }
            status("Mikrofon açık • oda $room")
        }
        if (makeOffer) createOffer()
    }

    private fun createOffer() {
        peer?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peer?.setLocalDescription(simpleSdpObserver(), desc)
                send(JSONObject().put("kind","offer").put("sdp",desc.description))
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { status("Ses teklifi oluşturulamadı") }
            override fun onSetFailure(error: String) {}
        }, MediaConstraints())
    }

    private fun createAnswer() {
        peer?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peer?.setLocalDescription(simpleSdpObserver(), desc)
                send(JSONObject().put("kind","answer").put("sdp",desc.description))
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String) { status("Ses yanıtı oluşturulamadı") }
            override fun onSetFailure(error: String) {}
        }, MediaConstraints())
    }

    private fun simpleSdpObserver() = object : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) { status("SDP oluşturma hatası") }
        override fun onSetFailure(error: String) { status("SDP ayarlama hatası") }
    }

    private fun send(data: JSONObject) {
        ws?.send(JSONObject().put("type","signal").put("room",room).put("data",data).toString())
    }

    override fun onDestroy() {
        stopping = true
        handler.removeCallbacksAndMessages(null)
        ws?.close(1000,"user stopped")
        peer?.close(); peer?.dispose()
        audioTrack?.dispose(); audioSource?.dispose(); factory?.dispose()
        http.dispatcher.executorService.shutdown()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
