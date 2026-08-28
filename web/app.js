const registeredShops = [
    { coordinates: { lat: 36.397650673276345, lon: 140.50535377876412 }, name: "山岡家 ひたちなか店" },
    { coordinates: { lat: 36.366183045752884, lon: 140.48297166948979 }, name: "山岡家 水戸城南店" },
    { coordinates: { lat: 36.537558996534784, lon: 140.63643157503827 }, name: "山岡家 日立金沢店" },
    { coordinates: { lat: 36.537658129508564, lon: 140.41588693877145 }, name: "山岡家 常陸大宮店" },
    { coordinates: { lat: 36.377374656642665, lon: 140.3608206027508 }, name: "山岡家 水戸内原店" },
    { coordinates: { lat: 36.31298564606419, lon: 140.44880778716356 }, name: "山岡家 水戸南店" },
    { coordinates: { lat: 36.19229574055287, lon: 140.2941730730318 }, name: "山岡家 石岡店" },
    { coordinates: { lat: 36.13136315812844, lon: 140.22564799696343 }, name: "山岡家 かすみがうら店" },
    { coordinates: { lat: 36.085492839913535, lon: 140.2060142271692 }, name: "山岡家 土浦店" },
    { coordinates: { lat: 36.07663120071107, lon: 140.1059249155306 }, name: "山岡家 つくば中央店" },
    { coordinates: { lat: 36.04915550750586, lon: 140.0848427526962 }, name: "山岡家 谷田部店" },
    { coordinates: { lat: 35.99932258990889, lon: 140.1533127114314 }, name: "山岡家 牛久店" },
    { coordinates: { lat: 35.915208766063834, lon: 140.63690426791038 }, name: "山岡家 神栖店" }
];

const SEARCH_RADIUS_METERS = 100000;
const KOKO_RADIUS_METERS = 100;
const SPECIAL_EFFECT_RADIUS_METERS = 50;

const imageFiles = ["yamaokaya.png", "gyoza.png", "miso.jpg", "shio.jpg", "tokusei_miso.jpg", "kara_miso.jpg"];
let selectedImageIndex = 0;

let currentHeadingDegrees = 0;
let currentLocation = null;
let nearestShop = null;

let watchId = null;

// Animation state
let currentRotation = 0; // Smooth rotation value
let targetRotation = 0;  // Target rotation value based on sensors

// DOM Elements
const overlay = document.getElementById("permission-overlay");
const btnRequestPermission = document.getElementById("btn-request-permission");
const titleText = document.getElementById("title-text");
const loadingView = document.getElementById("loading-view");
const errorView = document.getElementById("error-view");
const distanceView = document.getElementById("distance-view");
const errorMessage = document.getElementById("error-message");
const btnRetry = document.getElementById("btn-retry");
const kokoBox = document.getElementById("koko-special-announcement");
const kokoShopName = document.getElementById("koko-shop-name");
const distanceText = document.getElementById("distance-text");
const actionButtonsContainer = document.getElementById("action-buttons-container");
const btnAction = document.getElementById("btn-action");
const directionImage = document.getElementById("direction-image");

// Math util
function calculateDistanceMeters(from, to) {
    const R = 6371e3;
    const p1 = from.lat * Math.PI / 180;
    const p2 = to.lat * Math.PI / 180;
    const dp = (to.lat - from.lat) * Math.PI / 180;
    const dl = (to.lon - from.lon) * Math.PI / 180;
    const a = Math.sin(dp/2) * Math.sin(dp/2) +
              Math.cos(p1) * Math.cos(p2) *
              Math.sin(dl/2) * Math.sin(dl/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}

function calculateBearing(from, to) {
    const lat1 = from.lat * Math.PI / 180;
    const lat2 = to.lat * Math.PI / 180;
    const deltaLon = (to.lon - from.lon) * Math.PI / 180;
    const y = Math.sin(deltaLon) * Math.cos(lat2);
    const x = Math.cos(lat1) * Math.sin(lat2) -
              Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
    let bearing = Math.atan2(y, x) * 180 / Math.PI;
    return (bearing + 360) % 360;
}

function findNearestShop(currentLat, currentLon) {
    let nearest = null;
    const current = { lat: currentLat, lon: currentLon };
    for (const shop of registeredShops) {
        const dist = calculateDistanceMeters(current, shop.coordinates);
        if (dist > SEARCH_RADIUS_METERS) continue;
        const bearing = calculateBearing(current, shop.coordinates);
        if (!nearest || dist < nearest.distanceMeters) {
            nearest = {
                name: shop.name,
                coordinates: shop.coordinates,
                distanceMeters: dist,
                bearingDegrees: bearing
            };
        }
    }
    return nearest;
}

// Smoothing / Loop
function animate() {
    // Shortest path interpolation for 360 wrap-around
    let diff = targetRotation - currentRotation;
    while (diff < -180) diff += 360;
    while (diff > 180) diff -= 360;
    
    // Smooth factor (smaller = smoother, larger = snappier)
    currentRotation += diff * 0.15; 
    
    directionImage.style.transform = `rotate(${currentRotation}deg)`;
    requestAnimationFrame(animate);
}

function updateState() {
    if (!nearestShop) return;

    const isKoko = nearestShop.distanceMeters <= KOKO_RADIUS_METERS;
    const isSpecialRange = nearestShop.distanceMeters <= SPECIAL_EFFECT_RADIUS_METERS;

    // Title
    titleText.textContent = isKoko ? "Yamaokaya is Koko!!!" : "Yamaokaya is Doko";

    // UI elements update
    if (isKoko) {
        if (isSpecialRange) {
            distanceText.style.display = "none";
            kokoBox.style.display = "flex";
            kokoShopName.textContent = nearestShop.name + " is Koko!!!";
            btnAction.classList.add("btn-large");
        } else {
            kokoBox.style.display = "none";
            distanceText.style.display = "block";
            distanceText.textContent = Math.round(nearestShop.distanceMeters) + " m";
            btnAction.classList.remove("btn-large");
        }
        actionButtonsContainer.style.display = "block";
    } else {
        kokoBox.style.display = "none";
        distanceText.style.display = "block";
        distanceText.textContent = (nearestShop.distanceMeters / 1000).toFixed(2) + " km";
        actionButtonsContainer.style.display = "none";
    }

    // Update target rotation for the animation loop
    targetRotation = nearestShop.bearingDegrees - currentHeadingDegrees;
}

function startTracking() {
    overlay.style.display = "none";
    showLoading();

    if ("geolocation" in navigator) {
        watchId = navigator.geolocation.watchPosition(
            (position) => {
                currentLocation = { lat: position.coords.latitude, lon: position.coords.longitude };
                nearestShop = findNearestShop(currentLocation.lat, currentLocation.lon);
                if (nearestShop) {
                    showDistance();
                    document.getElementById("btn-share").style.display = "flex";
                    updateState();
                } else {
                    showError("100km以内に山岡家が見つかりませんでした。");
                }
            },
            (err) => showError("位置情報の取得に失敗しました。 (" + err.message + ")"),
            { enableHighAccuracy: true, maximumAge: 0, timeout: 10000 }
        );
    } else {
        showError("お使いのブラウザは位置情報サービスに対応していません。");
    }

    // Orientation
    if (window.DeviceOrientationEvent && typeof DeviceOrientationEvent.requestPermission === 'function') {
        DeviceOrientationEvent.requestPermission().then(state => {
            if (state === 'granted') window.addEventListener('deviceorientation', handleOrientation);
        }).catch(console.error);
    } else {
        window.addEventListener('deviceorientationabsolute', handleOrientation);
        window.addEventListener('deviceorientation', handleOrientation);
    }

    // Start loop
    requestAnimationFrame(animate);
}

function handleOrientation(event) {
    let heading = 0;
    if (event.webkitCompassHeading) {
        heading = event.webkitCompassHeading;
    } else if (event.absolute === true && event.alpha !== null) {
        heading = 360 - event.alpha;
    } else {
        heading = event.alpha ? 360 - event.alpha : 0;
    }
    currentHeadingDegrees = heading;
    updateState();
}

function showLoading() {
    loadingView.style.display = "flex";
    errorView.style.display = "none";
    distanceView.style.display = "none";
}

function showError(msg) {
    loadingView.style.display = "none";
    distanceView.style.display = "none";
    errorView.style.display = "flex";
    errorMessage.textContent = "エラー: " + msg;
}

function showDistance() {
    loadingView.style.display = "none";
    errorView.style.display = "none";
    distanceView.style.display = "flex";
}

// Listeners
btnRequestPermission.addEventListener("click", startTracking);
btnRetry.addEventListener("click", () => {
    if (watchId !== null) navigator.geolocation.clearWatch(watchId);
    startTracking();
});
directionImage.addEventListener("click", () => {
    selectedImageIndex = (selectedImageIndex + 1) % imageFiles.length;
    directionImage.src = `img/${imageFiles[selectedImageIndex]}`;
});
btnAction.addEventListener("click", () => {
    alert("Web版ではチェックイン機能は未実装です。山岡家公式サイトをご覧ください！");
    window.open("https://www.yamaokaya.com/menus/yamaokaya/regular/", "_blank");
});
document.getElementById("btn-share").addEventListener("click", () => {
    if (nearestShop) {
        const text = `山岡家 ${nearestShop.name} まで ${nearestShop.distanceMeters > 1000 ? (nearestShop.distanceMeters/1000).toFixed(2) + 'km' : Math.round(nearestShop.distanceMeters) + 'm'}\n#YamaokayaisDoko`;
        if (navigator.share) {
            navigator.share({ title: 'Yamaokaya is Doko', text: text, url: 'https://koba9813.github.io/yamaokaya/' }).catch(console.error);
        } else {
            alert(`シェア内容:\n${text}`);
        }
    }
});

// Service Worker Registration for PWA
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('./sw.js').then(reg => {
            console.log('ServiceWorker registration successful with scope: ', reg.scope);
        }, err => {
            console.error('ServiceWorker registration failed: ', err);
        });
    });
}
