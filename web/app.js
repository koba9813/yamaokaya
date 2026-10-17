const SEARCH_RADIUS_METERS = 100000;
const KOKO_RADIUS_METERS = 50;
const CHECK_IN_COOLDOWN_MS = 3 * 60 * 60 * 1000;
const MENU_URL = 'https://www.yamaokaya.com/menus/yamaokaya/regular/';

const imageFiles = [
    'yamaokaya.png',
    'gyoza.png',
    'miso.jpg',
    'shio.jpg',
    'tokusei_miso.jpg',
    'kara_miso.jpg'
];

let registeredShops = [];
let selectedImageIndex = 0;

let currentHeadingDegrees = 0;
let currentLocation = null;
let nearestShop = null;
let watchId = null;

let currentRotation = 0;
let targetRotation = 0;

// DOM Elements
const overlay = document.getElementById('permission-overlay');
const btnRequestPermission = document.getElementById('btn-request-permission');
const titleText = document.getElementById('title-text');
const loadingView = document.getElementById('loading-view');
const errorView = document.getElementById('error-view');
const distanceView = document.getElementById('distance-view');
const errorMessage = document.getElementById('error-message');
const btnRetry = document.getElementById('btn-retry');
const kokoBox = document.getElementById('koko-special-announcement');
const kokoShopName = document.getElementById('koko-shop-name');
const distanceText = document.getElementById('distance-text');
const actionButtonsContainer = document.getElementById('action-buttons-container');
const btnAction = document.getElementById('btn-action');
const directionImage = document.getElementById('direction-image');
const btnShare = document.getElementById('btn-share');
const btnMenu = document.getElementById('btn-menu');
const feedbackMessage = document.getElementById('feedback-message');
const versionText = document.getElementById('version-text');

versionText.textContent = 'Ver Web PWA';

// ---------- Math utilities ----------
function toRad(deg) { return deg * Math.PI / 180; }

function calculateDistanceMeters(from, to) {
    const R = 6371e3;
    const p1 = toRad(from.lat);
    const p2 = toRad(to.lat);
    const dp = toRad(to.lat - from.lat);
    const dl = toRad(to.lon - from.lon);
    const a = Math.sin(dp / 2) * Math.sin(dp / 2) +
              Math.cos(p1) * Math.cos(p2) *
              Math.sin(dl / 2) * Math.sin(dl / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function calculateBearing(from, to) {
    const lat1 = toRad(from.lat);
    const lat2 = toRad(to.lat);
    const deltaLon = toRad(to.lon - from.lon);
    const y = Math.sin(deltaLon) * Math.cos(lat2);
    const x = Math.cos(lat1) * Math.sin(lat2) -
              Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
    let bearing = Math.atan2(y, x) * 180 / Math.PI;
    return (bearing + 360) % 360;
}

function normalizeDegrees(value) {
    let normalized = value % 360;
    if (normalized < 0) normalized += 360;
    return normalized;
}

function formatDistance(meters) {
    if (meters < 1000) {
        return `${Math.round(meters)} m`;
    }
    return `${(meters / 1000).toFixed(2)} km`;
}

function findNearestShop(currentLat, currentLon) {
    const current = { lat: currentLat, lon: currentLon };
    let nearest = null;
    for (const shop of registeredShops) {
        const dist = calculateDistanceMeters(current, shop);
        if (dist > SEARCH_RADIUS_METERS) continue;
        if (!nearest || dist < nearest.distanceMeters) {
            nearest = {
                name: shop.name,
                prefecture: shop.prefecture,
                coordinates: { lat: shop.lat, lon: shop.lon },
                distanceMeters: dist,
                bearingDegrees: calculateBearing(current, shop)
            };
        }
    }
    return nearest;
}

// ---------- UI helpers ----------
function showLoading() {
    loadingView.style.display = 'flex';
    errorView.style.display = 'none';
    distanceView.style.display = 'none';
}

function showError(msg) {
    loadingView.style.display = 'none';
    distanceView.style.display = 'none';
    errorView.style.display = 'flex';
    errorMessage.textContent = 'エラー: ' + msg;
}

function showDistance() {
    loadingView.style.display = 'none';
    errorView.style.display = 'none';
    distanceView.style.display = 'flex';
}

function updateState() {
    if (!nearestShop) return;

    const isKoko = nearestShop.distanceMeters <= KOKO_RADIUS_METERS;

    titleText.textContent = isKoko ? 'Yamaokaya is Koko!!!' : 'Yamaokaya is Doko';

    const imageHint = document.querySelector('.image-hint');
    if (isKoko) {
        distanceText.style.display = 'none';
        kokoBox.style.display = 'flex';
        kokoShopName.textContent = nearestShop.name + ' is Koko!!!';
        actionButtonsContainer.style.display = 'block';
        directionImage.style.display = 'none';
        if (imageHint) imageHint.style.display = 'none';
    } else {
        kokoBox.style.display = 'none';
        distanceText.style.display = 'block';
        distanceText.textContent = formatDistance(nearestShop.distanceMeters);
        actionButtonsContainer.style.display = 'none';
        directionImage.style.display = 'block';
        if (imageHint) imageHint.style.display = 'block';
    }

    btnShare.style.display = 'flex';
    btnMenu.style.display = 'flex';

    targetRotation = nearestShop.bearingDegrees - currentHeadingDegrees;
}

function animate() {
    let diff = targetRotation - currentRotation;
    while (diff < -180) diff += 360;
    while (diff > 180) diff -= 360;
    currentRotation += diff * 0.12;
    directionImage.style.transform = `rotate(${currentRotation}deg)`;
    requestAnimationFrame(animate);
}

// ---------- Orientation ----------
function handleOrientation(event) {
    let heading = 0;
    if (event.webkitCompassHeading != null) {
        heading = event.webkitCompassHeading;
    } else if (event.absolute === true && event.alpha != null) {
        heading = 360 - event.alpha;
    } else if (event.alpha != null) {
        heading = 360 - event.alpha;
    }
    currentHeadingDegrees = heading;
    updateState();
}

function requestOrientationPermission() {
    return new Promise((resolve) => {
        if (window.DeviceOrientationEvent && typeof DeviceOrientationEvent.requestPermission === 'function') {
            DeviceOrientationEvent.requestPermission()
                .then((state) => {
                    if (state === 'granted') {
                        window.addEventListener('deviceorientation', handleOrientation);
                    }
                    resolve(state);
                })
                .catch((err) => {
                    console.error('Device orientation permission error:', err);
                    resolve('denied');
                });
        } else if ('ondeviceorientationabsolute' in window) {
            window.addEventListener('deviceorientationabsolute', handleOrientation);
            resolve('granted');
        } else {
            window.addEventListener('deviceorientation', handleOrientation);
            resolve('granted');
        }
    });
}

// ---------- Location ----------
function startLocationTracking() {
    if (!('geolocation' in navigator)) {
        showError('お使いのブラウザは位置情報サービスに対応していません。');
        return;
    }

    watchId = navigator.geolocation.watchPosition(
        (position) => {
            currentLocation = {
                lat: position.coords.latitude,
                lon: position.coords.longitude
            };
            nearestShop = findNearestShop(currentLocation.lat, currentLocation.lon);
            if (nearestShop) {
                showDistance();
                updateState();
            } else {
                showError('100km以内に山岡家が見つかりませんでした。');
            }
        },
        (err) => {
            showError('位置情報の取得に失敗しました。 (' + err.message + ')');
        },
        { enableHighAccuracy: true, maximumAge: 0, timeout: 15000 }
    );
}

// ---------- Controls ----------
async function startTracking() {
    overlay.style.display = 'none';
    showLoading();

    // Load shop data if not already loaded
    if (registeredShops.length === 0) {
        try {
            const response = await fetch('./shops.json');
            if (!response.ok) throw new Error('shops.json の読み込みに失敗しました');
            registeredShops = await response.json();
        } catch (err) {
            showError(err.message || '店舗データの読み込みに失敗しました。');
            return;
        }
    }

    await requestOrientationPermission();
    startLocationTracking();
    requestAnimationFrame(animate);
}

btnRequestPermission.addEventListener('click', startTracking);

btnRetry.addEventListener('click', () => {
    if (watchId !== null) navigator.geolocation.clearWatch(watchId);
    startTracking();
});

directionImage.addEventListener('click', () => {
    selectedImageIndex = (selectedImageIndex + 1) % imageFiles.length;
    directionImage.src = `img/${imageFiles[selectedImageIndex]}`;
});

btnShare.addEventListener('click', () => {
    if (!nearestShop) return;
    const text = `山岡家 ${nearestShop.name} まで ${formatDistance(nearestShop.distanceMeters)}\n#YamaokayaisDoko`;
    if (navigator.share) {
        navigator.share({
            title: 'Yamaokaya is Doko',
            text: text,
            url: 'https://koba9813.github.io/yamaokaya/'
        }).catch(() => {});
    } else {
        alert(`シェア内容:\n${text}`);
    }
});

btnMenu.addEventListener('click', () => {
    window.open(MENU_URL, '_blank');
});

btnAction.addEventListener('click', () => {
    if (!nearestShop) return;
    const key = `checkin_${nearestShop.name}`;
    const last = parseInt(localStorage.getItem(key) || '0', 10);
    const now = Date.now();
    if (now - last < CHECK_IN_COOLDOWN_MS) {
        const remain = CHECK_IN_COOLDOWN_MS - (now - last);
        const minutes = Math.ceil(remain / 60000);
        feedbackMessage.textContent = `同じ店舗のチェックインは3時間に1回までです（あと${minutes}分）`;
        feedbackMessage.style.display = 'block';
        return;
    }
    localStorage.setItem(key, String(now));
    feedbackMessage.textContent = `${nearestShop.name} にチェックインしました！`;
    feedbackMessage.style.display = 'block';
});

// ---------- Service Worker ----------
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js').then((reg) => {
            console.log('ServiceWorker registered:', reg.scope);
        }, (err) => {
            console.error('ServiceWorker registration failed:', err);
        });
    });
}
