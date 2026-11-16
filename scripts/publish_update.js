/**
 * Script untuk mempublikasikan update aplikasi SALAM APP ke Firestore secara otomatis.
 *
 * Penggunaan:
 *   node scripts/publish_update.js
 *   node scripts/publish_update.js --url "https://domain.com/app.apk" --notes "Catatan rilis..."
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const API_KEY = 'AIzaSyCWVs_3L-yp2r_HuqlMB7Gcmb_heEo771E';
const PROJECT_ID = 'salam-app-5f6fe';
const ADMIN_EMAIL = 'mzakyalmubarok@asas.id';
const ADMIN_PASS = 'asasedan2026';

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(body) });
        } catch {
          resolve({ status: res.statusCode, body });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(typeof data === 'string' ? data : JSON.stringify(data));
    req.end();
  });
}

function parseBuildGradle() {
  const gradlePath = path.join(__dirname, '..', 'app', 'build.gradle.kts');
  const content = fs.readFileSync(gradlePath, 'utf8');

  const codeMatch = content.match(/versionCode\s*=\s*(\d+)/);
  const nameMatch = content.match(/versionName\s*=\s*"([^"]+)"/);

  return {
    versionCode: codeMatch ? parseInt(codeMatch[1], 10) : 1,
    versionName: nameMatch ? nameMatch[1] : '1.0'
  };
}

async function main() {
  const gradleInfo = parseBuildGradle();
  console.log(`=== Publikasi Otomatis In-App Update SALAM APP ===`);
  console.log(`Versi terdeteksi dari build.gradle.kts: v${gradleInfo.versionName} (kode: ${gradleInfo.versionCode})`);

  // Parse CLI args
  const args = process.argv.slice(2);
  let apkUrl = '';
  let notes = 'Pembaruan sistem, peningkatan performa, dan perbaikan antarmuka.';
  let forceUpdate = false;

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--url' && args[i + 1]) apkUrl = args[++i];
    if (args[i] === '--notes' && args[i + 1]) notes = args[++i];
    if (args[i] === '--force') forceUpdate = true;
  }

  // 1. Login Admin to get token
  process.stdout.write('Mengotentikasi akun Admin... ');
  const authRes = await request({
    hostname: 'identitytoolkit.googleapis.com',
    path: `/v1/accounts:signInWithPassword?key=${API_KEY}`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email: ADMIN_EMAIL,
    password: ADMIN_PASS,
    returnSecureToken: true
  });

  if (authRes.status !== 200) {
    console.error('\nGAGAL otentikasi admin:', authRes.body);
    process.exit(1);
  }
  const idToken = authRes.body.idToken;
  console.log('OK');

  // 2. Fetch existing config if apkUrl is empty
  if (!apkUrl) {
    process.stdout.write('Membaca konfigurasi URL terakhir dari Firestore... ');
    const getRes = await request({
      hostname: 'firestore.googleapis.com',
      path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/app_config/update`,
      method: 'GET',
      headers: { 'Authorization': `Bearer ${idToken}` }
    });

    if (getRes.status === 200 && getRes.body?.fields?.apkUrl?.stringValue) {
      apkUrl = getRes.body.fields.apkUrl.stringValue;
      console.log(`Ditemukan: ${apkUrl}`);
    } else {
      apkUrl = `https://github.com/pantau-santri/app/releases/latest/download/SALAM_APP_v${gradleInfo.versionName}.apk`;
      console.log(`Default template: ${apkUrl}`);
    }
  }

  // 3. Update app_config/update in Firestore
  process.stdout.write(`Mempublikasikan info update (v${gradleInfo.versionName}, code ${gradleInfo.versionCode})... `);
  const patchBody = {
    fields: {
      latestVersionCode: { integerValue: String(gradleInfo.versionCode) },
      latestVersionName: { stringValue: gradleInfo.versionName },
      apkUrl: { stringValue: apkUrl },
      releaseNotes: { stringValue: notes },
      forceUpdate: { booleanValue: forceUpdate },
      updatedAt: { integerValue: String(Date.now()) }
    }
  };

  const updateRes = await request({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${PROJECT_ID}/databases/(default)/documents/app_config/update`,
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${idToken}`
    }
  }, patchBody);

  if (updateRes.status === 200) {
    console.log('BERHASIL!');
    console.log('\nDetail Update Terpublikasi:');
    console.log(`- Versi: v${gradleInfo.versionName} (kode: ${gradleInfo.versionCode})`);
    console.log(`- URL: ${apkUrl}`);
    console.log(`- Catatan: ${notes}`);
    console.log(`- Wajib Update: ${forceUpdate}`);
    console.log('\nSemua perangkat pengurus yang membuka aplikasi akan langsung mendeteksi update ini.');
  } else {
    console.error('\nGAGAL mempublikasikan:', updateRes.body);
    process.exit(1);
  }
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
