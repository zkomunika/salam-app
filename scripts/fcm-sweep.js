/**
 * Script FCM Sweep Otomatis untuk GitHub Actions
 * Menjalankan pemeriksaan izin santri yang terlambat di Cloud Firestore
 * dan mengirimkan Push Notification FCM ke seluruh pengurus.
 * 
 * 100% Gratis - Berjalan di GitHub Actions cron tanpa perlu Firebase Cloud Functions Blaze.
 */

const admin = require('firebase-admin');

// Inisialisasi Firebase Admin dengan Service Account Key dari environment variable
const serviceAccountKeyJson = process.env.FIREBASE_SERVICE_ACCOUNT_KEY;

if (!serviceAccountKeyJson) {
  console.error("Error: Environment variable FIREBASE_SERVICE_ACCOUNT_KEY tidak ditemukan.");
  process.exit(1);
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(serviceAccountKeyJson);
} catch (e) {
  console.error("Error parsing FIREBASE_SERVICE_ACCOUNT_KEY:", e.message);
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const messaging = admin.messaging();

async function runSweep() {
  console.log("=== Memulai Pemeriksaan Izin Terlambat ===");
  const todayStr = new Date().toISOString().slice(0, 10); // Format YYYY-MM-DD
  console.log(`Tanggal hari ini: ${todayStr}`);

  // 1. Ambil seluruh data santri untuk pemetaan nama & kamar
  const santriSnapshot = await db.collection('santri').get();
  const santriMap = new Map();
  santriSnapshot.forEach(doc => {
    santriMap.set(Number(doc.id), doc.data());
  });

  // 2. Ambil izin yang belum kembali (tanggalKembali == null)
  const izinSnapshot = await db.collection('izin')
    .where('tanggalKembali', '==', null)
    .get();

  const overdueList = [];
  izinSnapshot.forEach(doc => {
    const data = doc.data();
    if (data.batasAwal && data.batasAwal < todayStr) {
      const santri = santriMap.get(Number(data.santriId)) || { nama: "Santri", kamar: "-" };
      overdueList.push({
        izinId: doc.id,
        nama: santri.nama,
        kamar: santri.kamar,
        batasAwal: data.batasAwal
      });
    }
  });

  console.log(`Ditemukan ${overdueList.length} santri terlambat.`);
  if (overdueList.length === 0) {
    console.log("Tidak ada santri terlambat hari ini. Selesai.");
    return;
  }

  // 3. Kumpulkan seluruh FCM token pengurus
  const usersSnapshot = await db.collection('users').get();
  const allTokens = [];

  for (const userDoc of usersSnapshot.docs) {
    const tokensSnapshot = await userDoc.ref.collection('fcmTokens').get();
    tokensSnapshot.forEach(tDoc => {
      const token = tDoc.data().token || tDoc.id;
      if (token) allTokens.push(token);
    });
  }

  console.log(`Ditemukan ${allTokens.length} token FCM pengurus.`);
  if (allTokens.length === 0) {
    console.log("Belum ada token FCM pengurus terdaftar.");
    return;
  }

  // 4. Kirim notifikasi untuk setiap santri yang terlambat
  for (const item of overdueList) {
    const message = {
      notification: {
        title: `Peringatan: ${item.nama} Terlambat!`,
        body: `Santri kamar ${item.kamar} belum kembali (Batas: ${item.batasAwal}). Ketuk untuk melihat.`
      },
      data: {
        type: 'TERLAMBAT',
        izinId: String(item.izinId)
      },
      tokens: allTokens
    };

    try {
      const response = await messaging.sendEachForMulticast(message);
      console.log(`Notifikasi terkirim untuk ${item.nama}: Berhasil: ${response.successCount}, Gagal: ${response.failureCount}`);
    } catch (err) {
      console.error(`Gagal mengirim FCM untuk ${item.nama}:`, err.message);
    }
  }

  console.log("=== Pemeriksaan Selesai ===");
}

runSweep().catch(err => {
  console.error("Terjadi kesalahan fatal:", err);
  process.exit(1);
});
