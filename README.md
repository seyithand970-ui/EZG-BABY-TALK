# EZG BABY TALK

Android bebek monitörü: bebek telefonundaki mikrofonu ebeveyn telefonuna internet üzerinden WebRTC ses bağlantısıyla aktarmayı hedefler.

## Bu sürümde eklenenler
- Bebek / ebeveyn rolü ve 6 haneli oda kodu
- Android foreground microphone service
- WebRTC ses teklifi/yanıtı ve ICE sinyalleşmesi
- WebSocket sinyal sunucusu (`server/`)
- Render dağıtım tanımı (`render.yaml`)
- GitHub Actions üzerinden debug APK derlemesi

## Sunucuyu dağıtma
1. [Render Blueprint](https://render.com/deploy?repo=https://github.com/seyithand970-ui/EZG-BABY-TALK) bağlantısını aç.
2. GitHub hesabını Render'a bağla ve `ezg-baby-talk-signal` servisini oluştur.
3. Dağıtım bitince `https://ezg-baby-talk-signal.onrender.com/health` adresinde `ok: true` görmelisin.

## Önemli sınırlamalar
- Android istemcide şu an sinyal adresi `wss://ezg-baby-talk-signal.onrender.com` olarak ayarlı.
- İlk WebRTC yapılandırması STUN kullanır; bazı operatör/NAT ağlarında bağlantı için ayrıca TURN rölesi gerekir.
- Render ücretsiz servisleri boşta kaldığında uyuyabilir; bu nedenle her zaman açık izleme için uygun değildir.
- Bu henüz saha testi yapılmış, üretim güvenilirliğinde bir bebek monitörü değildir. İki ayrı Android cihazda, farklı internet ağlarında test edilmelidir. Güvenli bebek uykusu ve yetişkin gözetiminin yerini tutmaz.
