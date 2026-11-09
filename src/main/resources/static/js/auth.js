// Firebase Authentication (Google Sign-In) bootstrap.
//
// HOW TO ENABLE (after you create your Firebase project):
//   1. Firebase Console -> Project settings -> General -> Your apps -> Web app
//   2. Copy the SDK setup config (the `firebaseConfig` object) and paste it
//      into the variable below, replacing the placeholder values.
//   3. In the same console, Authentication -> Sign-in method -> Google -> Enable
//   4. Save & reload http://localhost:8080
//
// Until you do that, the "Sign in with Google" button does nothing and the
// app continues to work in DEMO mode (backend skips auth).

const firebaseConfig = {
    apiKey:            "AIzaSyCTmLbpoWPiSqk6ktUR_UoFjANSQUSXpWk",
    authDomain:        "new2canada-ca03c.firebaseapp.com",
    projectId:         "new2canada-ca03c",
    storageBucket:     "new2canada-ca03c.firebasestorage.app",
    messagingSenderId: "783449294250",
    appId:             "1:783449294250:web:68a5cf6e09c08df3c857d3",
    measurementId:     "G-Y1GMRKKC73"
};

let firebaseApp = null;
let firebaseAuth = null;
// Resolves after onAuthStateChanged has fired at least once — i.e. when
// Firebase has finished restoring (or rejecting) any persisted session
// from IndexedDB. Callers that need the bearer token MUST await this,
// otherwise they'll hit the API a few hundred ms before currentUser is
// populated and get a 401 from the backend.
let authReady = null;
// Set when the Firebase SDK cannot load (offline, blocked CDN). We then stop
// retrying on every API call and let the app run anonymously.
let authUnavailable = false;

async function ensureFirebase() {
    if (firebaseApp) return firebaseAuth;
    if (authUnavailable) return null;
    if (firebaseConfig.apiKey === "YOUR_API_KEY") return null; // not configured yet

    try {
        const appMod  = await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js");
        const authMod = await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js");

        firebaseApp  = appMod.initializeApp(firebaseConfig);
        firebaseAuth = authMod.getAuth(firebaseApp);

        let resolveReady;
        authReady = new Promise(r => { resolveReady = r; });
        let resolved = false;
        authMod.onAuthStateChanged(firebaseAuth, (user) => {
            renderAuthUI(user, authMod);
            if (!resolved) { resolved = true; resolveReady(); }
        });
        window.__firebaseAuthModule__ = authMod; // for sign-in button
        return firebaseAuth;
    } catch (err) {
        console.warn('Sign-in is unavailable:', err);
        authUnavailable = true;
        return null;
    }
}

function renderAuthUI(user, authMod) {
    const box = document.getElementById('authBox');
    if (!box) return;
    if (user) {
        const photo = user.photoURL
            ? `<img src="${user.photoURL}" alt="" class="w-8 h-8 rounded-full">`
            : `<span class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center text-primary material-symbols-outlined text-[18px]">person</span>`;
        box.innerHTML = `
            <div class="flex items-center gap-3">
                <span class="flex items-center gap-2 pl-1 pr-4 py-1 bg-surface-container rounded-full font-label-md text-label-md text-on-surface">${photo}<span>${user.displayName || user.email}</span></span>
                <button id="signOutBtn" class="font-label-md text-label-md text-on-surface-variant hover:text-primary transition-colors duration-200">Sign out</button>
            </div>`;
        document.getElementById('signOutBtn').onclick = () => authMod.signOut(firebaseAuth);
    } else {
        box.innerHTML = `<a href="/login.html" class="font-label-md text-label-md bg-primary text-on-primary px-6 py-2.5 rounded-full hover:shadow-lg active:scale-95 transition-all duration-200">Sign in</a>`;
    }
}

// Exposed to app.js — returns a fresh ID token or null if anonymous.
// Awaits the first onAuthStateChanged firing so we never report "anonymous"
// just because IndexedDB hasn't finished restoring the session yet.
window.getIdToken = async function () {
    const auth = await ensureFirebase();
    if (!auth) return null;
    if (authReady) await authReady;
    if (!auth.currentUser) return null;
    return await auth.currentUser.getIdToken();
};

// Used by login.html's "Sign in with Google" button.
window.signInWithGoogle = async function () {
    const auth = await ensureFirebase();
    if (!auth) throw new Error('Sign-in is not available right now. You can still search without an account.');
    const authMod = window.__firebaseAuthModule__;
    const provider = new authMod.GoogleAuthProvider();
    await authMod.signInWithPopup(auth, provider);
};

document.addEventListener('DOMContentLoaded', async () => {
    const auth = await ensureFirebase();
    // Render the placeholder Sign-in button even when firebase isn't configured.
    if (!auth) {
        const box = document.getElementById('authBox');
        if (box) box.innerHTML =
            `<span class="font-label-sm text-label-sm text-on-surface-variant" title="Sign-in is not available on this server">Sign-in unavailable</span>`;
    }
});
