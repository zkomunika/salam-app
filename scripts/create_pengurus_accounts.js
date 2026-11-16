/**
 * Script untuk membuat akun seluruh pengurus ASAS di Firebase Auth dan Cloud Firestore.
 * Proyek: salam-app-5f6fe
 * Password seragam: asasedan2026
 */

const https = require('https');

const API_KEY = 'AIzaSyCWVs_3L-yp2r_HuqlMB7Gcmb_heEo771E';
const PROJECT_ID = 'salam-app-5f6fe';
const DEFAULT_PASSWORD = 'asasedan2026';

const PENGURUS_LIST = [
  { nama: 'ALFAN MAULANA ADIF', email: 'alfanmaulanaadif@asas.id', role: 'PENGURUS' },
  { nama: 'AZRIL IRHAM WAHID', email: 'azrilirhamwahid@asas.id', role: 'PENGURUS' },
  { nama: 'DIKI GUNGUN', email: 'dikigungun@asas.id', role: 'PENGURUS' },
  { nama: 'FIKRI M HILMI', email: 'fikrimhilmi@asas.id', role: 'PENGURUS' },
  { nama: 'M ARIFIN ILHAM', email: 'marifinilham@asas.id', role: 'PENGURUS' },
  { nama: 'M AZKA ANGGIRI', email: 'mazkaanggiri@asas.id', role: 'PENGURUS' },
  { nama: 'M FAJRUL HIKAM', email: 'mfajrulhikam@asas.id', role: 'PENGURUS' },
  { nama: 'M FIKRI HAMBALI', email: 'mfikrihambali@asas.id', role: 'PENGURUS' },
  { nama: 'M IMRON', email: 'mimron@asas.id', role: 'PENGURUS' },
  { nama: 'M NAJIB SAEFUL MILAH', email: 'mnajibsaefulmilah@asas.id', role: 'PENGURUS' },
  { nama: 'M THORIQ MUSTHAFA', email: 'mthoriqmusthafa@asas.id', role: 'PENGURUS' },
  { nama: 'M ZAKY AL-MUBAROK', email: 'mzakyalmubarok@asas.id', role: 'ADMIN' },
  { nama: 'M ZAKY AL-MUBAROK (Gmail)', email: 'mzakyalmubarok9.4@gmail.com', role: 'ADMIN' },
  { nama: 'MIFTAHUDIN', email: 'miftahudin@asas.id', role: 'PENGURUS' },
  { nama: 'MOH GHIYATS MAULANA ARIPIN', email: 'mohghiyatsmaulanaaripin@asas.id', role: 'PENGURUS' },
  { nama: 'QIZWIN NAIL M', email: 'qizwinnailm@asas.id', role: 'PENGURUS' },
  { nama: 'RIKI PRAYOKO', email: 'rikiprayoko@asas.id', role: 'PENGURUS' },
  { nama: 'UJANG MUHAMMAD RIZKI', email: 'ujangmuhammadrizki@asas.id', role: 'PENGURUS' },
  { nama: 'AKMAL MASYKUR HIDAYAT', email: 'akmalmasykurhidayat@asas.id', role: 'PENGURUS' },
  { nama: 'ALWI AHMAD ZEIN', email: 'alwiahmadzein@asas.id', role: 'PENGURUS' },
  { nama: 'M AZMAN ABDUL AZIZ', email: 'mazmanabdulaziz@asas.id', role: 'PENGURUS' },
  { nama: 'M KHOIRUL MILAL', email: 'mkhoirulmilal@asas.id', role: 'PENGURUS' },
  { nama: 'MAULANA HASYIM', email: 'maulanahasyim@asas.id', role: 'PENGURUS' }
];

function request(options, data) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          resolve({ status: res.statusCode, body: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, body });
        }
      });
    });
    req.on('error', reject);
    if (data) {
      req.write(typeof data === 'string' ? data : JSON.stringify(data));
    }
    req.end();
  });
}

async function createOrSignIn(email, password) {
  // First try signUp
  let res = await request({
    hostname: 'identitytoolkit.googleapis.com',
    path: `/v1/accounts:signUp?key=${API_KEY}`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, {
    email,
    password,
    returnSecureToken: true
  });

  if (res.status === 200) {
    return {
      uid: res.body.localId,
      idToken: res.body.idToken,
      created: true
    };
  }

  // If email already exists, sign in
  if (res.body?.error?.message === 'EMAIL_EXISTS') {
    let signInRes = await request({
      hostname: 'identitytoolkit.googleapis.com',
      path: `/v1/accounts:signInWithPassword?key=${API_KEY}`,
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    }, {
      email,
      password,
      returnSecureToken: true
    });

    if (signInRes.status === 200) {
      return {
        uid: signInRes.body.localId,
        idToken: signInRes.body.idToken,
        created: false
      };
    } else {
      throw new Error(`Sign in failed for ${email}: ${JSON.stringify(signInRes.body)}`);
    }
  }

  throw new Error(`SignUp failed for ${email}: ${JSON.stringify(res.body)}`);
}

async function saveFirestoreProfile(idToken, uid, profile) {
  const path = `/v1/projects/${PROJECT_ID}/databases/(default)/documents/users/${uid}`;
  const firestoreBody = {
    fields: {
      uid: { stringValue: uid },
      email: { stringValue: profile.email },
      nama: { stringValue: profile.nama },
      role: { stringValue: profile.role },
      createdAt: { integerValue: String(Date.now()) }
    }
  };

  const res = await request({
    hostname: 'firestore.googleapis.com',
    path: path,
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${idToken}`
    }
  }, firestoreBody);

  if (res.status !== 200) {
    throw new Error(`Failed to save Firestore doc for ${profile.email}: ${JSON.stringify(res.body)}`);
  }
  return res.body;
}

async function main() {
  console.log(`=== Memulai Pembuatan ${PENGURUS_LIST.length} Akun Pengurus ASAS ===\n`);
  let successCount = 0;
  let errorCount = 0;

  for (const [index, p] of PENGURUS_LIST.entries()) {
    try {
      process.stdout.write(`[${index + 1}/${PENGURUS_LIST.length}] ${p.nama} (${p.email})... `);
      const authData = await createOrSignIn(p.email, DEFAULT_PASSWORD);
      await saveFirestoreProfile(authData.idToken, authData.uid, p);
      console.log(`BERHASIL (${authData.created ? 'Akun Baru' : 'Sudah Ada, Update Profil'}) [Role: ${p.role}]`);
      successCount++;
    } catch (err) {
      console.log(`GAGAL: ${err.message}`);
      errorCount++;
    }
  }

  console.log(`\n=== Selesai: ${successCount} Berhasil, ${errorCount} Gagal ===`);
}

main();
