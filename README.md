# Lifx VPN — Android / WireGuard

Lifx VPN adalah aplikasi Android client WireGuard. Aplikasi ini membuat tunnel sungguhan melalui library WireGuard resmi; aplikasi **tidak menyertakan server VPN**. Anda harus memiliki server WireGuard dan file konfigurasi client yang valid.

## Build APK melalui GitHub Actions

1. Buat repository GitHub baru bernama `LifxVPN`.
2. Upload semua isi folder proyek ini ke root repository (folder `.github` juga harus ikut).
3. Buka tab **Actions** dan izinkan Actions bila diminta.
4. Pilih workflow **Build Lifx VPN APK**, lalu tekan **Run workflow**. Workflow juga berjalan saat push ke branch `main` atau `master`.
5. Buka run yang selesai, lalu unduh artifact `LifxVPN-debug-apk`.
6. Ekstrak artifact ZIP dan pasang `app-debug.apk` di Android. Izinkan instalasi dari sumber tersebut bila Android memintanya.

APK debug untuk pengujian; sebelum distribusi publik, siapkan signing key release dan kebijakan privasi yang sesuai.

## Membuat server WireGuard di Ubuntu VPS

Contoh dasar di server Ubuntu 22.04/24.04. Jalankan dari console VPS sebagai administrator. Pilih provider dan region yang Anda percaya; biaya VPS mungkin berlaku.

```bash
sudo apt update
sudo apt install -y wireguard
umask 077
wg genkey | tee server_private.key | wg pubkey > server_public.key
wg genkey | tee client_private.key | wg pubkey > client_public.key
```

Aktifkan forwarding IPv4 dengan membuat `/etc/sysctl.d/99-wireguard.conf` berisi:

```text
net.ipv4.ip_forward=1
```

Lalu jalankan `sudo sysctl --system`. Konfigurasi interface server `/etc/wireguard/wg0.conf` (ganti `eth0` sesuai interface publik VPS dan gunakan key yang dihasilkan):

```ini
[Interface]
Address = 10.66.66.1/24
ListenPort = 51820
PrivateKey = <ISI server_private.key>
PostUp = iptables -A FORWARD -i %i -j ACCEPT; iptables -A FORWARD -o %i -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i %i -j ACCEPT; iptables -D FORWARD -o %i -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE
```

Buka UDP port `51820` di firewall VPS dan cloud firewall. Pastikan konfigurasi NAT sesuai distro/provider. Buat `/etc/wireguard/wg0.conf` hanya bisa dibaca root, lalu aktifkan:

```bash
sudo chmod 600 /etc/wireguard/wg0.conf
sudo systemctl enable --now wg-quick@wg0
sudo wg show
```

Tambahkan peer client ke konfigurasi server:

```ini
[Peer]
PublicKey = <ISI client_public.key>
AllowedIPs = 10.66.66.2/32
```

Setelah mengedit server config, jalankan `sudo systemctl restart wg-quick@wg0`. Buat konfigurasi client di bawah. **Jangan masukkan server private key ke aplikasi client.**

## Konfigurasi client untuk ditempel ke Lifx VPN

Ganti placeholder dengan key/IP yang benar. `Endpoint` harus memakai IP publik atau domain VPS. `DNS` dapat diubah sesuai pilihan Anda.

```ini
[Interface]
PrivateKey = <ISI client_private.key>
Address = 10.66.66.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = <ISI server_public.key>
Endpoint = <IP-PUBLIK-VPS>:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```

Jika server hanya menyediakan IPv4, jangan mengklaim IPv6 terlindungi kecuali routing IPv6 juga dikonfigurasi. Untuk menghindari kebocoran IPv6, atur dukungan IPv6 dengan benar atau sesuaikan `AllowedIPs` dan kebijakan perangkat. Konfigurasi full-tunnel `0.0.0.0/0, ::/0` akan merutekan seluruh IPv4 dan IPv6 melalui tunnel, jadi server harus mendukung rute yang sesuai.

## Catatan keamanan

- Jangan commit file `.conf`, private key, atau kredensial server ke repository publik.
- Contoh perintah server di atas adalah template, bukan konfigurasi universal. Sesuaikan nama interface jaringan, firewall, NAT, dan routing untuk VPS Anda.
- Pastikan Anda punya izin untuk menggunakan server dan patuhi hukum/ketentuan provider.
- Tombol Connect meminta izin VPN Android dan hanya menandai terhubung setelah backend WireGuard berhasil mengaktifkan tunnel.
- Untuk pemakaian produksi, lakukan audit keamanan, pengujian kebocoran DNS/IPv6, serta konfigurasi signing release.

## Teknologi

- Kotlin + Android SDK
- WireGuard tunnel library
- GitHub Actions untuk build APK debug
