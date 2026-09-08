import React, { useEffect, useMemo, useRef, useState } from "react";
import { Geolocation } from '@capacitor/geolocation';
import { PlanetaryAlarm } from './src/plugins/planetaryAlarm';
import {
  Menu,
  X,
  Palette,
  BookOpen,
  Sparkles,
  Wand2,
  Orbit,
  ScrollText,
  MoonStar,
  Sigma,
  Clock,
  Bell,
  BellOff,
  MapPin,
} from "lucide-react";

const PLANET_EXTRA_POWERS = {
  Saturn: ["Time Management","Long-Term Planning","Building Lasting Habits","Overcoming Procrastination","Accountability","Resilience","Letting Go of the Past","Facing Fears","Responsibility","Delayed Gratification","Emotional Maturity","Setting Healthy Limits","Consistency","Career Longevity"],
  Jupiter: ["Higher Education","Travel","Mentorship","Confidence","Faith","Broadening Horizons","Publishing & Writing","Legal Matters","Philosophy","Gratitude","Big-Picture Thinking","Calculated Risk-Taking","Personal Growth","Celebration"],
  Mars: ["Physical Fitness","Assertiveness","Starting New Projects","Athletic Performance","Standing Up for Yourself","Ambition","Conflict Resolution","Stamina","Discipline in Training","Decisiveness","Cutting Ties","Breaking Bad Habits"],
  Sun: ["Self Esteem","Identity","Public Speaking","Joy","Mentorship","Career Visibility","Clarity of Purpose","Personal Branding","Energy Renewal","Recognition for Work"],
  Venus: ["Diplomacy","Reconciliation","Self Care","Aesthetics & Design","Financial Harmony","Social Grace","Partnership","Body Positivity","Peacemaking","Enjoyment of Life","Balance"],
  Mercury: ["Studying & Exams","Coding & Programming","Travel & Commuting","Contracts & Agreements","Editing & Proofreading","Problem Solving","Multitasking","Sales","Research","Interviews"],
  Moon: ["Emotional Regulation","Sleep & Rest","Self Care Routines","Family & Home Life","Nurturing Relationships","Comfort","Memory & Nostalgia","Motherhood","Habit Formation","Mood Awareness","Inner Child Healing","Safety & Security"],
};

const LETTER_VALUES = {
  A: 1, B: 2, C: 3, D: 4, E: 5, F: 6, G: 7,
  H: 8, I: 9, J: 10, K: 11, L: 12, M: 13,
  N: 14, O: 15, P: 16, Q: 17, R: 18, S: 19,
  T: 20, U: 21, V: 22, W: 23, X: 24, Y: 25, Z: 26,
};

const PLANETS = [
  {
    name: "Saturn",
    symbol: "♄",
    color: "#8b8b8b",
    size: 3,
    maxNumber: 9,
    powers: ["Protection","Discipline","Banishing","Boundaries","Wisdom","Stability","Binding","Meditation","Authority","Focus","Defense","Patience","Shadow Work","Isolation","Structure","Karmic Justice","Manifestation Through Delay","Maturity","Ending Cycles","Limitation","Endurance","Persistence","Containment","Grounding"],
    grid: [[4,9,2],[3,5,7],[8,1,6]]
  },
  {
    name: "Jupiter",
    symbol: "♃",
    color: "#3b82f6",
    size: 4,
    maxNumber: 16,
    powers: ["Luck","Wealth","Expansion","Success","Prosperity","Abundance","Growth","Leadership","Victory","Blessings","Financial Success","Good Fortune","Honor","Influence","Career Advancement","Justice","Opportunities","Spiritual Growth","Wisdom","Power","Royal Favor","Elevation","Optimism","Generosity"],
    grid: [[4,14,15,1],[9,7,6,12],[5,11,10,8],[16,2,3,13]]
  },
  {
    name: "Mars",
    symbol: "♂",
    color: "#ef4444",
    size: 5,
    maxNumber: 25,
    powers: ["Courage","Strength","Protection","Passion","Action","Willpower","Drive","Motivation","Power","Aggression","Fearlessness","Combat","Competition","Sexual Energy","Dominance","Breaking Obstacles","Physical Energy","War Energy","Force","Confidence","Initiative","Victory","Survival","Intensity"],
    grid: [[11,24,7,20,3],[4,12,25,8,16],[17,5,13,21,9],[10,18,1,14,22],[23,6,19,2,15]]
  },
  {
    name: "Sun",
    symbol: "☉",
    color: "#facc15",
    size: 6,
    maxNumber: 36,
    powers: ["Fame","Vitality","Creativity","Confidence","Recognition","Success","Radiance","Charisma","Leadership","Personal Power","Influence","Visibility","Achievement","Glory","Healing","Self Expression","Life Force","Authority","Solar Energy","Nobility","Illumination","Purpose","Inspiration","Strength"],
    grid: [[6,32,3,34,35,1],[7,11,27,28,8,30],[19,14,16,15,23,24],[18,20,22,21,17,13],[25,29,10,9,26,12],[36,5,33,4,2,31]]
  },
  {
    name: "Venus",
    symbol: "♀",
    color: "#16a34a",
    size: 7,
    maxNumber: 49,
    powers: ["Love","Beauty","Charm","Attraction","Harmony","Relationships","Romance","Pleasure","Luxury","Sensuality","Affection","Popularity","Self Love","Friendship","Emotional Healing","Fertility","Desirability","Art","Grace","Seduction","Softness","Passion","Magnetism","Intimacy"],
    grid: [[22,47,16,41,10,35,4],[5,23,48,17,42,11,29],[30,6,24,49,18,36,12],[13,31,7,25,43,19,37],[38,14,32,1,26,44,20],[21,39,8,33,2,27,45],[46,15,40,9,34,3,28]]
  },
  {
    name: "Mercury",
    symbol: "☿",
    color: "#f97316",
    size: 8,
    maxNumber: 64,
    powers: ["Communication","Learning","Business","Writing","Speech","Technology","Knowledge","Mental Clarity","Persuasion","Networking","Commerce","Teaching","Memory","Negotiation","Language","Adaptability","Quick Thinking","Cleverness","Logic","Information","Curiosity","Expression","Intellect","Strategy"],
    grid: [[64,2,3,61,60,6,7,57],[9,55,54,12,13,51,50,16],[17,47,46,20,21,43,42,24],[40,26,27,37,36,30,31,33],[32,34,35,29,28,38,39,25],[41,23,22,44,45,19,18,48],[49,15,14,52,53,11,10,56],[8,58,59,5,4,62,63,1]]
  },
  {
    name: "Moon",
    symbol: "☾",
    color: "#e5e7eb",
    size: 9,
    maxNumber: 81,
    powers: ["Dreams","Psychic Power","Intuition","Emotion","Divination","Mysticism","Imagination","Lunar Magick","Inner Vision","Spiritual Sensitivity","Subconscious Mind","Reflection","Clairvoyance","Night Energy","Emotional Healing","Psychic Protection","Astral Projection","Prophetic Dreams","Fantasy","Water Energy","Receptivity","Cycles","Sensitivity","Moon Rituals"],
    grid: [[37,78,29,70,21,62,13,54,5],[6,38,79,30,71,22,63,14,46],[47,7,39,80,31,72,23,55,15],[16,48,8,40,81,32,64,24,56],[57,17,49,9,41,73,33,65,25],[26,58,18,50,1,42,74,34,66],[67,27,59,10,51,2,43,75,35],[36,68,19,60,11,52,3,44,76],[77,28,69,20,61,12,53,4,45]]
  }
];

PLANETS.forEach((p) => {
  const extras = PLANET_EXTRA_POWERS[p.name] || [];
  extras.forEach((power) => {
    if (!p.powers.includes(power)) p.powers.push(power);
  });
});

const PLANETS_BY_NAME = PLANETS.reduce((acc, p) => {
  acc[p.name] = p;
  return acc;
}, {});

// Traditional ceremonial-planetary correspondences (Agrippa / 777 tradition).
// divine: Hebrew Divine Name (El = Jupiter, Elohim = Saturn, etc.)
const PLANETAL_CORRESPONDENCES = {
  Saturn: {
    sign: "♄",
    archangel: { en: "Cassiel", he: "קסיאל" },
    intelligence: { en: "Agiel", he: "אגיאל" },
    spirit: { en: "Zazel", he: "זאזל" },
    divine: { en: "YHVH Elohim", he: "יהוה אלוהים" },
  },
  Jupiter: {
    sign: "♃",
    archangel: { en: "Sachiel", he: "שכאל" },
    intelligence: { en: "Iophiel", he: "יופיאל" },
    spirit: { en: "Hismael", he: "היסמאל" },
    divine: { en: "El", he: "אל" },
  },
  Mars: {
    sign: "♂",
    archangel: { en: "Samael", he: "סמאל" },
    intelligence: { en: "Graphiel", he: "גרפיאל" },
    spirit: { en: "Bartzabel", he: "ברצבל" },
    divine: { en: "Elohim Gibor", he: "אלוהים גבור" },
  },
  Sun: {
    sign: "☉",
    archangel: { en: "Michael", he: "מיכאל" },
    intelligence: { en: "Nakhiel", he: "נחיאל" },
    spirit: { en: "Sorath", he: "סורת" },
    divine: { en: "YHVH Eloah Va-Daath", he: "יהוה אלוה ודעת" },
  },
  Venus: {
    sign: "♀",
    archangel: { en: "Haniel", he: "חניאל" },
    intelligence: { en: "Hagiel", he: "הגיאל" },
    spirit: { en: "Kedemel", he: "קדמאל" },
    divine: { en: "YHVH Tzabaoth", he: "יהוה צבאות" },
  },
  Mercury: {
    sign: "☿",
    archangel: { en: "Raphael", he: "רפאל" },
    intelligence: { en: "Tiriel", he: "תיריאל" },
    spirit: { en: "Taphthartharath", he: "תפתרתרת" },
    divine: { en: "Elohim Tzabaoth", he: "אלוהים צבאות" },
  },
  Moon: {
    sign: "☾",
    archangel: { en: "Gabriel", he: "גבריאל" },
    intelligence: { en: "Malkah Be-Tarshishim", he: "מלכה בתרשישים" },
    spirit: { en: "Schad Barschemoth", he: "שד ברשמות" },
    divine: { en: "Shaddai El Chai", he: "שדי אל חי" },
  },
};

// ---------------------------------------------------------------------------
// PLANETARY HOURS ENGINE
// Everything below is pure math (no network calls, no geocoding API).
// Location comes only from navigator.geolocation (built into the browser/OS)
// or manual lat/long entry. Timezone comes from Intl, also built-in.
// ---------------------------------------------------------------------------

const CHALDEAN_ORDER = ["Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon"];
// getDay(): 0 = Sunday ... 6 = Saturday
const DAY_RULERS = ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"];

function toRad(deg) { return (deg * Math.PI) / 180; }
function toDeg(rad) { return (rad * 180) / Math.PI; }

function dayOfYear(date) {
  const start = new Date(date.getFullYear(), 0, 0);
  return Math.floor((date - start) / 86400000);
}

// Classic sunrise/sunset (almanac) algorithm. Returns decimal local hours
// (0-24) for the given calendar date, or null for polar day/night.
function calcSunDecimalHour(date, lat, lon, isSunrise) {
  const zenith = 90.833;
  const N = dayOfYear(date);
  const lngHour = lon / 15;
  const t = isSunrise ? N + (6 - lngHour) / 24 : N + (18 - lngHour) / 24;

  const M = 0.9856 * t - 3.289;
  let L = M + 1.916 * Math.sin(toRad(M)) + 0.02 * Math.sin(toRad(2 * M)) + 282.634;
  L = (L + 360) % 360;

  let RA = toDeg(Math.atan(0.91764 * Math.tan(toRad(L))));
  RA = (RA + 360) % 360;
  const Lquadrant = Math.floor(L / 90) * 90;
  const RAquadrant = Math.floor(RA / 90) * 90;
  RA = (RA + (Lquadrant - RAquadrant)) / 15;

  const sinDec = 0.39782 * Math.sin(toRad(L));
  const cosDec = Math.cos(Math.asin(sinDec));
  const cosH =
    (Math.cos(toRad(zenith)) - sinDec * Math.sin(toRad(lat))) / (cosDec * Math.cos(toRad(lat)));

  if (cosH > 1 || cosH < -1) return null; // sun never rises/sets (polar extremes)

  let H = isSunrise ? 360 - toDeg(Math.acos(cosH)) : toDeg(Math.acos(cosH));
  H = H / 15;

  const T = H + RA - 0.06571 * t - 6.622;
  let UT = T - lngHour;
  UT = ((UT % 24) + 24) % 24;

  const utcOffsetHours = -date.getTimezoneOffset() / 60;
  return ((UT + utcOffsetHours) % 24 + 24) % 24;
}

function dateAtDecimalHour(baseDate, decimalHour) {
  const d = new Date(baseDate);
  d.setHours(0, 0, 0, 0);
  d.setTime(d.getTime() + decimalHour * 3600 * 1000);
  return d;
}

function getSunTimes(date, lat, lon) {
  const sunrise = calcSunDecimalHour(date, lat, lon, true);
  const sunset = calcSunDecimalHour(date, lat, lon, false);
  if (sunrise === null || sunset === null) return null;
  return {
    sunrise: dateAtDecimalHour(date, sunrise),
    sunset: dateAtDecimalHour(date, sunset),
  };
}

function addDays(date, n) {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

// Builds the 24 planetary hours (12 day + 12 night) for the sunrise-to-sunrise
// "planetary day" that starts on the given calendar date.
function getPlanetaryDayForDate(lat, lon, refDate) {
  const today = getSunTimes(refDate, lat, lon);
  const tomorrow = getSunTimes(addDays(refDate, 1), lat, lon);
  if (!today || !tomorrow) return null;

  const dayStart = today.sunrise;
  const dayEnd = today.sunset;
  const nightEnd = tomorrow.sunrise;

  const startIndex = CHALDEAN_ORDER.indexOf(DAY_RULERS[refDate.getDay()]);
  const dayLen = (dayEnd - dayStart) / 12;
  const nightLen = (nightEnd - dayEnd) / 12;

  const hours = [];
  for (let i = 0; i < 24; i++) {
    const isDay = i < 12;
    const start = isDay ? new Date(dayStart.getTime() + i * dayLen) : new Date(dayEnd.getTime() + (i - 12) * nightLen);
    const end = isDay ? new Date(dayStart.getTime() + (i + 1) * dayLen) : new Date(dayEnd.getTime() + (i - 11) * nightLen);
    const planet = CHALDEAN_ORDER[(startIndex + i) % 7];
    hours.push({
      key: `${start.getTime()}`,
      index: i,
      isDay,
      planet,
      start,
      end,
    });
  }

  return { hours, dayStart, dayEnd, nightEnd, rulerPlanet: CHALDEAN_ORDER[startIndex], refDate };
}

// Which calendar date's planetary day currently contains `now`
// (planetary days start at sunrise, not midnight).
function getPivotDate(lat, lon, now) {
  const today = getSunTimes(now, lat, lon);
  if (!today) return now;
  return now >= today.sunrise ? now : addDays(now, -1);

}

function formatClock(date) {
  return date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function formatDuration(ms) {
  if (ms < 0) ms = 0;
  const totalSeconds = Math.floor(ms / 1000);
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

function removeDuplicateLetters(text) {
  const seen = new Set();
  return text.split("").filter((char) => {
    if (seen.has(char)) return false;
    seen.add(char);
    return true;
  }).join("");
}

const THEMES = [
  { id: "night", label: "Midnight", glow: "rgba(139,92,246,0.2)" },
  { id: "violet", label: "Violet", glow: "rgba(139,92,246,0.28)" },
  { id: "ocean", label: "Ocean", glow: "rgba(14,165,233,0.24)" },
  { id: "emerald", label: "Emerald", glow: "rgba(16,185,129,0.22)" },
  { id: "rose", label: "Rose", glow: "rgba(244,114,182,0.22)" },
  { id: "light", label: "Light", glow: "rgba(139,92,246,0.15)" },
];

function loadPersist(key, fallback) {
  try {
    if (typeof localStorage === "undefined") return fallback;
    const raw = localStorage.getItem(key);
    return raw == null ? fallback : JSON.parse(raw);
  } catch (e) {
    return fallback;
  }
}

function savePersist(key, value) {
  try {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(key, JSON.stringify(value));
  } catch (e) { /* ignore */ }
}

export default function ChaosSigilForge() {
  const [planet, setPlanet] = useState(PLANETS[0]);
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuSection, setMenuSection] = useState("tool");
  const [showNumbers, setShowNumbers] = useState(false);
  const [showPowers, setShowPowers] = useState(false);
  const [sigilModalOpen, setSigilModalOpen] = useState(false);
  const [modalChooserOpen, setModalChooserOpen] = useState(false);
  const [showSign, setShowSign] = useState(true);
  const [showArchangel, setShowArchangel] = useState(false);
  const [showIntelligence, setShowIntelligence] = useState(false);
  const [showSpirit, setShowSpirit] = useState(false);
  const [showDivineName, setShowDivineName] = useState(false);
  const [showCorrLang, setShowCorrLang] = useState("en"); // en | he
  const longPressTimerRef = useRef(null);
  const longPressStartRef = useRef(null);
  const [removeVowels, setRemoveVowels] = useState(true);
  const [removeRepeats, setRemoveRepeats] = useState(true);
  const [strokeWidth, setStrokeWidth] = useState(4);
  const [curvature, setCurvature] = useState(0.8);
  const [intention, setIntention] = useState("I attract creative power and artistic recognition");

  // --- Planetary Hours state ---
  const [coords, setCoords] = useState(() => loadPersist("ps_coords", null)); // { lat, lon }
  const [locationStatus, setLocationStatus] = useState(() =>
    loadPersist("ps_coords", null) ? "granted" : "idle"
  ); // idle | requesting | denied | granted
  const [manualLat, setManualLat] = useState("");
  const [manualLon, setManualLon] = useState("");
  const [now, setNow] = useState(new Date());
  const [scheduledAlarms, setScheduledAlarms] = useState(() =>
    loadPersist("ps_alarms", [])
  ); // [{ ts, planet }]
  const [notifPermission, setNotifPermission] = useState(
    typeof Notification !== "undefined" ? Notification.permission : "unsupported"
  );
  const [leadMinutes, setLeadMinutes] = useState(() => loadPersist("ps_lead", 5));
  const [theme, setTheme] = useState(() => loadPersist("ps_theme", "night"));
  const [themeMenuOpen, setThemeMenuOpen] = useState(false);
  const timersRef = useRef([]);
  const webTimersRef = useRef({});

  const notifyKeys = useMemo(
    () => new Set(scheduledAlarms.map((a) => String(a.ts))),
    [scheduledAlarms]
  );

  // Persist settings so they survive closing/reopening the app.
  useEffect(() => savePersist("ps_coords", coords), [coords]);
  useEffect(() => savePersist("ps_alarms", scheduledAlarms), [scheduledAlarms]);
  useEffect(() => savePersist("ps_lead", leadMinutes), [leadMinutes]);
  useEffect(() => savePersist("ps_theme", theme), [theme]);

  // On launch, re-schedule any persisted reminders that are still in the future
  // so they keep working even if the app was closed (or the phone rebooted).
  useEffect(() => {
    if (!(typeof window !== "undefined" && window.Capacitor)) return;
    const nowMs = Date.now();
    const alarms = scheduledAlarms.filter((a) => a.ts > nowMs);
    const lead = leadMinutes;
    alarms.forEach((a) => {
      PlanetaryAlarm.schedule({
        timestamp: String(a.ts),
        planetName: a.planet,
        leadMinutes: lead,
      }).catch(console.error);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // On open (native APK), check + request notification access so alarms can work.
  useEffect(() => {
    if (!(typeof window !== "undefined" && window.Capacitor)) return;
    PlanetaryAlarm.hasNotificationPermission()
      .then((r) => setNotifPermission(r.value ? "granted" : "denied"))
      .catch(() => {});
    PlanetaryAlarm.requestNotificationPermission().catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const requestLocation = async () => {
    setLocationStatus("requesting");
    try {
      if (typeof window !== 'undefined' && window.Capacitor) {
        await Geolocation.requestPermissions({ permissions: ['location'] });
      }
      const pos = await Geolocation.getCurrentPosition({ enableHighAccuracy: false, timeout: 10000 });
      setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude });
      setLocationStatus("granted");
    } catch (e) {
      console.error(e);
      setLocationStatus("denied");
    }
  };

  const useManualCoords = () => {
    const lat = parseFloat(manualLat);
    const lon = parseFloat(manualLon);
    if (Number.isFinite(lat) && Number.isFinite(lon)) {
      setCoords({ lat, lon });
      setLocationStatus("granted");
    }
  };

  const [dayOffset, setDayOffset] = useState(0);

  const pivotDate = useMemo(() => {
    if (!coords) return now;
    return getPivotDate(coords.lat, coords.lon, now);
  }, [coords, now.toDateString()]);

  const viewedDate = useMemo(() => addDays(pivotDate, dayOffset), [pivotDate, dayOffset]);

  const planetaryDay = useMemo(() => {
    if (!coords) return null;
    return getPlanetaryDayForDate(coords.lat, coords.lon, viewedDate);
  }, [coords, viewedDate.toDateString()]);

  const currentHour = useMemo(() => {
    if (!planetaryDay || dayOffset !== 0) return null;
    return planetaryDay.hours.find((h) => now >= h.start && now < h.end) || null;
  }, [planetaryDay, now, dayOffset]);

  const viewedDayLabel = useMemo(() => {
    if (dayOffset === 0) return "Today";
    if (dayOffset === 1) return "Tomorrow";
    if (dayOffset === -1) return "Yesterday";
    return viewedDate.toLocaleDateString([], { weekday: "long", month: "short", day: "numeric" });
  }, [dayOffset, viewedDate]);

  const requestNotifPermission = async () => {
    if (typeof window !== 'undefined' && window.Capacitor) {
      try {
        await PlanetaryAlarm.requestNotificationPermission();
      } catch (e) { /* ignore */ }
    }
    if (typeof Notification === "undefined") return;
    Notification.requestPermission().then((perm) => setNotifPermission(perm));
  };

  const toggleNotify = async (hour) => {
    const ts = hour.start.getTime();
    const isAdding = !notifyKeys.has(hour.key);

    if (isAdding) {
      setScheduledAlarms((prev) => {
        const others = prev.filter((a) => a.ts !== ts);
        return [...others, { ts, planet: hour.planet }];
      });
    } else {
      setScheduledAlarms((prev) => prev.filter((a) => a.ts !== ts));
    }

    const opts = {
      timestamp: ts.toString(),
      planetName: hour.planet,
    };

    if (typeof window !== 'undefined' && window.Capacitor) {
      try {
        if (isAdding) {
          if (await PlanetaryAlarm.hasExactAlarmPermission && !(await PlanetaryAlarm.hasExactAlarmPermission()).value) {
            await PlanetaryAlarm.requestExactAlarmPermission();
          }
          await PlanetaryAlarm.schedule({ ...opts, leadMinutes });
        } else {
          await PlanetaryAlarm.cancel(opts);
        }
      } catch (e) {
        console.error("Failed to schedule/cancel native alarm", e);
      }
    } else {
      if (isAdding) {
        if (notifPermission === "default") await requestNotifPermission();
        if (typeof Notification !== "undefined" && Notification.permission === "granted") {
          const nowMs = Date.now();
          if (leadMinutes > 0) {
            const remindAt = ts - leadMinutes * 60000;
            if (remindAt > nowMs) {
              webTimersRef.current[ts] = setTimeout(
                () => new Notification(`Coming up: ${hour.planet} hour`, { body: `The ${hour.planet} hour begins in ${leadMinutes} min.` }),
                remindAt - nowMs
              );
            }
          }
          if (ts > nowMs) {
            if (webTimersRef.current[ts]) clearTimeout(webTimersRef.current[ts]);
            webTimersRef.current[ts] = setTimeout(
              () => new Notification(`${hour.planet} hour`, { body: `The ${hour.planet} hour has begun.` }),
              ts - nowMs
            );
          }
          alert(`Notification scheduled for ${hour.planet} at ${formatClock(hour.start)}${leadMinutes > 0 ? ` + reminder ${leadMinutes} min before` : ''}. Keep this tab open.`);
        } else {
          alert('Enable notifications in your browser to receive hour alerts.');
        }
      } else {
        if (webTimersRef.current[ts]) clearTimeout(webTimersRef.current[ts]);
        delete webTimersRef.current[ts];
        alert(`Reminder for ${hour.planet} at ${formatClock(hour.start)} is now off.`);
      }
    }
  };

  const uppercaseText = useMemo(() => {
    return intention.toUpperCase().replace(/[^A-Z]/g, "");
  }, [intention]);

  const noVowelsText = useMemo(() => {
    return removeVowels ? uppercaseText.replace(/[AEIOU]/g, "") : uppercaseText;
  }, [uppercaseText, removeVowels]);

  const sigilText = useMemo(() => {
    return removeRepeats ? removeDuplicateLetters(noVowelsText) : noVowelsText;
  }, [noVowelsText, removeRepeats]);

  const transformationSteps = useMemo(() => {
    return sigilText.split("").map((letter) => {
      const alphabetNumber = LETTER_VALUES[letter];

      return {
        letter,
        alphabetNumber,
        planetaryNumber: ((alphabetNumber - 1) % planet.maxNumber) + 1,
      };
    });
  }, [sigilText, planet]);

  const points = useMemo(() => {
    const cell = 360 / planet.size;
    const result = [];

    transformationSteps.forEach((step) => {
      planet.grid.forEach((row, rowIndex) => {
        row.forEach((value, colIndex) => {
          if (value === step.planetaryNumber) {
            result.push({
              x: colIndex * cell + cell / 2,
              y: rowIndex * cell + cell / 2,
            });
          }
        });
      });
    });

    return result;
  }, [planet, transformationSteps]);

  const pathData = useMemo(() => {
    if (points.length < 2) return "";

    let path = `M ${points[0].x} ${points[0].y}`;

    for (let i = 1; i < points.length; i++) {
      const prev = points[i - 1];
      const current = points[i];

      const controlX = (prev.x + current.x) / 2 + (current.y - prev.y) * curvature;
      const controlY = (prev.y + current.y) / 2 - (current.x - prev.x) * curvature;

      path += ` Q ${controlX} ${controlY}, ${current.x} ${current.y}`;
    }

    return path;
  }, [points, curvature]);

  const isNative = typeof window !== 'undefined' && window.Capacitor;

  const downloadSigil = (format) => {
    const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 360" width="360" height="360">
  <rect width="360" height="360" fill="black"/>
  <g transform="translate(180 180) scale(0.5) translate(-180 -180)">
    <path d="${pathData}" fill="none" stroke="${planet.color}" stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
</svg>`;
    const ext = format === 'svg' ? 'svg' : format === 'jpeg' ? 'jpg' : 'png';
    const mimeType = format === 'jpeg' ? 'image/jpeg' : format === 'svg' ? 'image/svg+xml' : 'image/png';
    const fileName = `sigil-${planet.name.toLowerCase()}-${Date.now()}.${ext}`;

    const triggerDownload = (url) => {
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.rel = 'noopener';
      document.body.appendChild(a);
      a.click();
      setTimeout(() => {
        document.body.removeChild(a);
        if (url.startsWith('blob:')) URL.revokeObjectURL(url);
      }, 1000);
    };

    const rasterToDataUrl = (onDone) => {
      const size = 2048;
      const canvas = document.createElement('canvas');
      canvas.width = size;
      canvas.height = size;
      const ctx = canvas.getContext('2d');
      const img = new Image();
      const dataUri = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgContent);
      img.onload = () => {
        ctx.drawImage(img, 0, 0, size, size);
        onDone(canvas.toDataURL(mimeType.replace('image/svg+xml','image/png'), 0.92));
      };
      img.onerror = () => {
        alert('Could not render the sigil to ' + format.toUpperCase() + '. Try the SVG export instead.');
      };
      img.src = dataUri;
    };

    if (isNative) {
      // Packaged APK: the WebView can't save blob/<a download>. Instead we write to
      // app cache (works on EVERY Android version, no permissions) and open the
      // Android system file/share sheet, where the user picks where to save.
      const fail = (info) => alert('Could not open the file to save it. ' + info);
      const viaSystemUI = (dataUrl, svg) =>
        PlanetaryAlarm.openFileWithSystemUI({ dataUrl, svg, fileName })
          .then((r) => (r.success ? null : fail(format.toUpperCase())))
          .catch(() => fail(format.toUpperCase()));
      if (format === 'svg') {
        viaSystemUI(null, svgContent);
        return;
      }
      rasterToDataUrl((dataUrl) => viaSystemUI(dataUrl, null));
      return;
    }

    if (format === 'svg') {
      const blob = new Blob([svgContent], { type: 'image/svg+xml' });
      triggerDownload(URL.createObjectURL(blob));
      return;
    }

    rasterToDataUrl((dataUrl) => triggerDownload(dataUrl));
  };

  // Fast on-device check that saving works, without generating a full sigil.
  const testDownload = () => {
    const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 360 360" width="360" height="360"><rect width="360" height="360" fill="black"/><circle cx="180" cy="180" r="120" fill="none" stroke="${planet.color}" stroke-width="12"/></svg>`;
    const fileName = `sigil-test-${Date.now()}.png`;
    if (isNative) {
      const canvas = document.createElement('canvas');
      canvas.width = 512;
      canvas.height = 512;
      const ctx = canvas.getContext('2d');
      const img = new Image();
      const dataUri = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgContent);
      img.onload = () => {
        ctx.drawImage(img, 0, 0, 512, 512);
        const dataUrl = canvas.toDataURL('image/png', 0.92);
        PlanetaryAlarm.openFileWithSystemUI({ dataUrl, fileName, svg: null })
          .then((r) => (r.success ? alert('Opened the test image — tap a save option in the system sheet to confirm it saves.') : alert('Could not open the test image.')))
          .catch(() => alert('Could not open the test image.'));
      };
      img.onerror = () => alert('Could not render the test image.');
      img.src = dataUri;
      return;
    }
    const blob = new Blob([svgContent], { type: 'image/svg+xml' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = fileName;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  };

  const closeSigilModal = () => {
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
    setModalChooserOpen(false);
    setSigilModalOpen(false);
  };

  const startSigilPress = (e) => {
    longPressStartRef.current = { x: e.clientX, y: e.clientY };
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
    setModalChooserOpen(false);
    longPressTimerRef.current = setTimeout(() => setModalChooserOpen(true), 500);
  };

  const cancelSigilPress = () => {
    if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
  };

  const moveSigilPress = (e) => {
    const start = longPressStartRef.current;
    if (start && (Math.abs(e.clientX - start.x) > 12 || Math.abs(e.clientY - start.y) > 12)) {
      if (longPressTimerRef.current) clearTimeout(longPressTimerRef.current);
    }
  };

  const corr = PLANETAL_CORRESPONDENCES[planet.name] || {};
  const langText = (o) =>
    showCorrLang === "he" ? (o && o.he) || (o && o.en) : (o && o.en) || (o && o.he);
  const archText = langText(corr.archangel);
  const intelText = langText(corr.intelligence);
  const spiritText = langText(corr.spirit);
  const divineText = langText(corr.divine);
  const sealWording = showCorrLang === "he";
  const sealNameSize = sealWording ? 26 : 18;

  // Talismanic seal drawn around the sigil: ring + planet sign + names at the quarters.
  const buildSeal = () => (
    <>
      {showSign && (
        <text
          x="180" y="180"
          textAnchor="middle" dominantBaseline="central"
          fontSize="240"
          fill={planet.color}
          opacity="0.13"
        >
          {corr.sign}
        </text>
      )}
      {(showArchangel || showIntelligence || showSpirit || showDivineName) && (
        <>
          <circle cx="180" cy="180" r="168" fill="none" stroke={planet.color} strokeWidth="1.4" opacity="0.4" />
          <circle cx="180" cy="180" r="146" fill="none" stroke={planet.color} strokeWidth="0.8" opacity="0.24" />
          {showArchangel && (
            <text x="180" y="46" textAnchor="middle" fill={planet.color} fillOpacity="0.9" fontWeight="bold" fontSize={sealNameSize}>
              {archText}
            </text>
          )}
          {showIntelligence && (
            <text x="326" y="180" textAnchor="middle" fill={planet.color} fillOpacity="0.9" fontWeight="bold" fontSize={sealNameSize} transform="rotate(90 326 180)">
              {intelText}
            </text>
          )}
          {showSpirit && (
            <text x="180" y="330" textAnchor="middle" fill={planet.color} fillOpacity="0.9" fontWeight="bold" fontSize={sealNameSize}>
              {spiritText}
            </text>
          )}
          {showDivineName && (
            <text x="34" y="180" textAnchor="middle" fill={planet.color} fillOpacity="0.9" fontWeight="bold" fontSize={sealNameSize} transform="rotate(-90 34 180)">
              {divineText}
            </text>
          )}
        </>
      )}
    </>
  );

  const activeTheme = THEMES.find((t) => t.id === theme) || THEMES[0];

  return (
    <div className="min-h-dvh bg-black text-white overflow-x-hidden relative" data-theme={theme}>
      <div
        className="absolute inset-0 pointer-events-none transition-all duration-700"
        style={{ backgroundImage: `radial-gradient(circle at top, ${activeTheme.glow}, transparent 40%)` }}
      />

      <header className="sticky top-0 z-50 backdrop-blur-xl bg-black/70 border-b border-white/10 shadow-[0_8px_30px_rgba(0,0,0,0.35)]">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 py-4 flex items-center justify-between safe-top">
          <div>
            <h1 className="text-2xl sm:text-3xl font-black bg-gradient-to-r from-violet-300 via-cyan-200 to-yellow-200 bg-clip-text text-transparent drop-shadow">
              Planetary Sigils
            </h1>
            <p className="text-[10px] uppercase tracking-[0.4em] text-zinc-500 mt-1">
              Chaos Magick Interface
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <button
                onClick={() => setThemeMenuOpen(!themeMenuOpen)}
                aria-label="Choose theme"
                className="w-12 h-12 rounded-2xl border border-white/10 bg-zinc-900/70 flex items-center justify-center hover:scale-105 transition-all"
              >
                <Palette size={20} />
              </button>
              {themeMenuOpen && (
                <div className="absolute right-0 top-14 w-48 rounded-2xl border border-white/10 bg-zinc-950/95 backdrop-blur-xl p-2 z-50 shadow-[0_0_30px_rgba(0,0,0,0.4)]">
                  <div className="px-3 py-1 text-[10px] uppercase tracking-widest text-zinc-500 font-bold">Theme</div>
                  {THEMES.map((t) => {
                    const isActive = t.id === theme;
                    return (
                      <button
                        key={t.id}
                        onClick={() => { setTheme(t.id); setThemeMenuOpen(false); }}
                        className={`w-full rounded-xl px-3 py-2 text-sm text-left flex items-center justify-between transition-all ${isActive ? 'bg-white/10 text-white' : 'text-zinc-300 hover:bg-white/[0.05]'}`}
                      >
                        <span>{t.label}</span>
                        <span className="w-3 h-3 rounded-full border border-white/40" style={{ backgroundColor: t.glow }} />
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              className="w-12 h-12 rounded-2xl border border-white/10 bg-zinc-900/70 flex items-center justify-center hover:scale-105 transition-all"
            >
              {menuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </header>

      {menuOpen && (
        <div className="fixed inset-0 z-40 bg-black/80 backdrop-blur-md overflow-y-auto">
          <div className="max-w-[900px] ml-auto min-h-dvh bg-zinc-950 border-l border-white/10 p-6 pb-[env(safe-area-inset-bottom,2rem)]">
            <div className="mt-16 flex gap-3 overflow-x-auto pb-4">
              {[
                { key: 'guide', label: 'Practice', icon: MoonStar, color: 'cyan' },
                { key: 'timing', label: 'Timing', icon: Clock, color: 'green' },
                { key: 'tool', label: 'Tool', icon: Wand2, color: 'yellow' },
              ].map((tab) => {
                const Icon = tab.icon;
                const active = menuSection === tab.key;

                return (
                  <button
                    key={tab.key}
                    onClick={() => setMenuSection(tab.key)}
                    className={`rounded-2xl px-5 py-4 border flex items-center gap-3 whitespace-nowrap transition-all duration-300 ${active ? 'scale-[1.03] shadow-[0_0_30px_rgba(255,255,255,0.15)] bg-white/10 border-white/30 text-white' : 'border-white/10 bg-white/[0.03] text-zinc-400 hover:bg-white/[0.06]'}`}
                  >
                    <Icon size={18} />
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {menuSection === 'guide' && (
              <div className="space-y-6 text-sm leading-relaxed text-zinc-300 mt-6">
                <h2 className="text-3xl font-black text-violet-200">Sigils & Sigil Magick</h2>

                <div className="space-y-5">
                  <div>
                    <h3 className="text-xl font-black text-white mb-3">What Is a Sigil?</h3>
                    <div className="space-y-4">
                      <p>
                        A sigil is a symbol built to carry a single intention. In chaos magick, a sigil is created by taking a written statement of desire and compressing it — through letter elimination, numerology, or abstraction — into a glyph that no longer visibly resembles the original sentence.
                      </p>
                      <p>
                        The word comes from the Latin <em>sigillum</em>, meaning "seal." Older magical traditions used sigils and seals engraved on metal, parchment, or talismans to represent spirits, planetary forces, or specific outcomes. Chaos magick, developed in the late 20th century by practitioners such as Austin Osman Spare and later Peter Carroll, stripped this practice of fixed belief systems and reframed it as a technology of the mind rather than a religious act.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">How Sigil Magick Works</h3>
                    <div className="space-y-4">
                      <p>
                        The core theory is that the conscious mind is a bottleneck. Desires that stay conscious get filtered through doubt, contradiction, and overthinking, which weakens them. Sigil magick works by encoding a desire into an abstract symbol so it can bypass conscious resistance and lodge directly in the subconscious.
                      </p>
                      <p>
                        Once the symbol is created, it is "charged" by staring at it during a state of altered focus — meditation, physical exhaustion, or trance are common routes — and then deliberately forgotten. The forgetting step matters: the belief is that a desire actively obsessed over stays tangled in conscious doubt, while one released to the subconscious can act without interference.
                      </p>
                      <p>
                        None of this claims to override physical law. It is generally understood, even within chaos magick, as a form of applied psychology and focused intent — a way of clarifying what you want and committing to it below the level of constant second-guessing.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">What You Can Do With a Sigil</h3>
                    <div className="space-y-4">
                      <p>
                        Sigils can be built around almost any single, clearly stated intention: confidence before an event, focus during a hard project, courage to have a difficult conversation, protection over a space, or emotional steadiness during a hard season. Because a sigil is abstract, it works best when the intention behind it is specific and honest rather than vague.
                      </p>
                      <p>
                        Practitioners typically keep a sigil private, place it somewhere it will be seen without being consciously studied (a wall, a journal, a phone background), or destroy it right after charging to symbolize release.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Planetary Magick</h3>
                    <div className="space-y-4">
                      <p>
                        Planetary magick is older than chaos magick and comes from Hermetic and astrological traditions. Each classical planet — Saturn, Jupiter, Mars, the Sun, Venus, Mercury, and the Moon — is treated as a symbolic force with its own domain: Saturn governs discipline and boundaries, Jupiter governs growth and abundance, Mars governs action and drive, the Sun governs vitality and identity, Venus governs love and harmony, Mercury governs communication and thought, and the Moon governs emotion and intuition.
                      </p>
                      <p>
                        Working "with" a planet means aligning an intention to the force whose domain matches it — using Mars for a fitness goal, Mercury for an exam, or the Moon for emotional healing, for example — rather than treating the planets as gods to be worshipped.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Planetary Hours</h3>
                    <div className="space-y-4">
                      <p>
                        Planetary hours are a timing system built on the same logic as planetary magick. Each day is split into 24 unequal hours — 12 running from sunrise to sunset, 12 from sunset to sunrise — and each one is ruled by one of the seven classical planets in a fixed rotating order. Because the length of daylight changes with the seasons, these "hours" stretch and shrink throughout the year and aren't the same as clock hours.
                      </p>
                      <p>
                        The influence of an hour is understood the same way as the influence of a planet itself: a Mars hour carries Mars's active, driving quality; a Venus hour carries Venus's harmonious, affectionate quality; a Moon hour carries the Moon's reflective, emotional quality — and so on for each planet's hour.
                      </p>
                      <p>
                        The benefit of working within the correct hour is reinforcement. An intention already aligned to a planet through its kamea gets a second, timing-based layer of alignment when it's also charged during that planet's hour — the theory being that the ritual is now working with the grain of that force twice over rather than once. It isn't considered mandatory; sigils are still charged successfully outside of matching hours. But choosing the right hour is treated as strengthening the working, similar to how choosing the right planet strengthens it over having no planetary alignment at all.
                      </p>
                      <p>
                        In practice this means checking which planet currently rules the hour before charging a sigil, and where possible, waiting for or planning around the hour that matches the sigil's intention — the Timing section of this app calculates that schedule for your location automatically.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Planetary Kameas</h3>
                    <div className="space-y-4">
                      <p>
                        A kamea is a magic square: a grid of numbers, unique to each planet, in which every row, column, and diagonal sums to the same total. Kameas appear in Hermetic and Renaissance magical texts, most notably Heinrich Cornelius Agrippa's <em>Three Books of Occult Philosophy</em>, as tools for constructing planetary talismans.
                      </p>
                      <p>
                        Each kamea's size matches its planet: Saturn's is 3×3, Jupiter's 4×4, Mars's 5×5, the Sun's 6×6, Venus's 7×7, Mercury's 8×8, and the Moon's 9×9 — mirroring older astrological associations between the planets and those numbers.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">How Kameas Affect Sigil Magick</h3>
                    <div className="space-y-4">
                      <p>
                        When a sigil is built on a planetary kamea, the letters of your intention are converted to numbers, those numbers are located inside the chosen planet's grid, and lines are drawn connecting them in sequence. The resulting shape is your sigil — but because it was traced across a specific planet's square, it's considered to carry that planet's symbolic quality alongside your intention.
                      </p>
                      <p>
                        This is why choosing the right planet matters as much as the wording of the intention itself: the kamea acts as the "channel" the sigil is drawn through, shaping which force the symbol is understood to be aligned with.
                      </p>
                    </div>
                  </div>
                </div>

                <h2 className="text-3xl font-black text-cyan-200 pt-4">HOW TO DO SIGIL MAGICK</h2>

                <div className="space-y-5">
                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Phase 1: Construction (The Sentence Method)</h3>

                    <div className="space-y-4">
                      <p>
                        <strong>Formulate the Statement of Intent:</strong> Write down your desire clearly and concisely. It must be in the positive present tense, avoiding phrases like “not” or “I want”.
                      </p>

                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-4">
                        <div className="text-red-200 font-black mb-2">Bad:</div>
                        <div>“I don't want to be broke.”</div>
                      </div>

                      <div className="rounded-2xl border border-green-500/20 bg-green-500/10 p-4">
                        <div className="text-green-200 font-black mb-2">Good:</div>
                        <div>“I HAVE ABUNDANT INCOME”</div>
                      </div>

                      <p>
                        <strong>Choose the Right Planet and Hour:</strong> Match your intention to the planet whose domain it falls under — Jupiter for abundance, Mars for courage or action, Venus for love, Mercury for study or communication, the Sun for confidence or recognition, the Moon for emotional healing, Saturn for discipline or boundaries. This is the planet whose kamea you'll build the sigil on. Where possible, plan to charge the sigil during that planet's hour for the same reason — the Timing section calculates the current schedule of hours for your location, so you can see when that planet's hour is coming up.
                      </p>

                      <p>
                        <strong>Eliminate Letters:</strong> Write the sentence in capital letters. Cross out all vowels (A, E, I, O, U).
                      </p>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 font-mono text-cyan-200 break-all">
                        I H A V E A B U N D A N T I N C O M E
                      </div>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 font-mono text-violet-200 break-all">
                        I H V B N D N T N C M
                      </div>

                      <p>
                        <strong>Eliminate Repeating Consonants:</strong> Cross out repeating consonants while keeping only the first occurrence.
                      </p>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 font-mono text-yellow-200 break-all">
                        Final Letters: I H V B N D T C M
                      </div>

                      <p>
                        <strong>Construct the Glyph:</strong> Turn the letters into numbers using alphabetical values.
                      </p>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 space-y-2 font-mono text-sm">
                        <div>A = 1</div>
                        <div>B = 2</div>
                        <div>C = 3</div>
                        <div>K = 11</div>
                        <div>X = 24</div>
                      </div>

                      <p>
                        Draw out a planetary kamea corresponding to your desire and trace lines or curves between the resulting numbers.
                      </p>

                      <p>
                        Sometimes a number is larger than the maximum number within a kamea. In that case reduce the number by adding the digits together.
                      </p>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 space-y-2 font-mono text-cyan-200">
                        <div>11 = 1 + 1 = 2</div>
                        <div>24 = 2 + 4 = 6</div>
                        <div>20 = 2 + 0 = 2</div>
                      </div>

                      <p>
                        Once the numbers fit inside the chosen kamea, connect them through lines or curves and a random-looking symbol will emerge — this becomes your sigil.
                      </p>

                      <p>
                        <strong>Goal:</strong> The final symbol should be simple enough to visualize mentally, yet abstract enough that the conscious mind no longer easily recognizes the original sentence.
                      </p>

                      <div className="rounded-2xl border border-violet-500/20 bg-violet-500/10 p-4 text-violet-100">
                        Everything described above can be done automatically with this app — simply write your statement of intent, choose your planetary force, and the sigil will be generated for you.
                      </div>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Phase 2: Charging (The Gnostic State)</h3>

                    <div className="space-y-4">
                      <p>
                        The goal is to move the sigil from the conscious mind into the subconscious through intense one-pointed focus during an altered state of consciousness known as Gnosis.
                      </p>

                      <p>
                        <strong>Select a Gnostic Technique:</strong> Use either inhibition methods (meditation, breath holding) or excitation methods (dance, emotional intensity, trance).
                      </p>

                      <div className="rounded-2xl border border-cyan-500/20 bg-cyan-500/10 p-4">
                        <div className="font-black text-cyan-100 mb-2">Example 1 — Inhibition</div>
                        <div>
                          Focus on the sigil while practicing 4-4-4-4 breathing: inhale 4 seconds, hold 4 seconds, exhale 4 seconds, pause 4 seconds.
                        </div>
                      </div>

                      <div className="rounded-2xl border border-yellow-500/20 bg-yellow-500/10 p-4">
                        <div className="font-black text-yellow-100 mb-2">Example 2 — Death Posture</div>
                        <div>
                          Stand on the balls of your feet with arms raised and body tensed until exhaustion. At the peak of mental emptiness, lock your gaze onto the sigil.
                        </div>
                      </div>

                      <p>
                        Another traditional method is gazing meditation (Trataka). Stare at the sigil without blinking while maintaining intense concentration until the image appears to glow, distort, disappear, or float.
                      </p>

                      <p>
                        <strong>Charge the Sigil:</strong> At the peak of gnosis, project emotional intensity into the symbol. Visualize it glowing, vibrating, burning, or radiating energy.
                      </p>

                      <p>
                        <strong>Banishment:</strong> At maximum intensity, abruptly shift your focus away from the ritual and consciously stop thinking about the desire.
                      </p>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-xl font-black text-white mb-3">Phase 3: Activation & Release</h3>

                    <div className="space-y-4">
                      <p>
                        <strong>Destruction:</strong> Burn, bury, or wash away the sigil to symbolize final release into the subconscious.
                      </p>

                      <p>
                        <strong>Forgetting:</strong> This is traditionally considered the most important step. The sigil should no longer be consciously obsessed over after activation.
                      </p>

                      <p>
                        <strong>Recording:</strong> Log the operation, date, method, and intention in a magical journal.
                      </p>

                      <div className="rounded-2xl border border-white/10 bg-black/40 p-4 space-y-3">
                        <div>
                          <div className="font-black text-green-200">Active Method</div>
                          <div className="text-zinc-300">The sigil is destroyed immediately after charging.</div>
                        </div>

                        <div>
                          <div className="font-black text-cyan-200">Passive Method</div>
                          <div className="text-zinc-300">The sigil is placed on the body, talisman, artwork, candle, or object and allowed to operate gradually over time.</div>
                        </div>
                      </div>

                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-4 text-red-100">
                        Important: Traditionally, sigils are not shown to others because outside attention is believed to weaken the operation through conscious interference.
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {menuSection === 'timing' && (
              <div className="space-y-4 mt-6">
                <h2 className="text-3xl font-black text-green-200">Planetary Hours</h2>
                <p className="text-zinc-400 text-sm">
                  Each hour of day and night is ruled by one of the seven planets. Sigil work is traditionally timed to the hour of the planet it's aligned with — a Mars sigil during a Mars hour, a Venus sigil during a Venus hour, and so on. This is calculated entirely on your device from your location and the current time — no internet connection required.
                </p>

                {!coords && (
                  <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-5 space-y-4">
                    <div className="flex items-center gap-2">
                      <MapPin className="text-green-300" size={18} />
                      <h3 className="font-black">Set Your Location</h3>
                    </div>
                    <p className="text-xs text-zinc-400">Needed to calculate today's sunrise and sunset, which the planetary hours are built on.</p>

                    <button
                      onClick={requestLocation}
                      disabled={locationStatus === 'requesting'}
                      className="w-full rounded-2xl py-4 border border-green-500/30 bg-green-500/10 text-green-100 font-bold hover:bg-green-500/20 transition-all duration-300"
                    >
                      {locationStatus === 'requesting' ? 'Requesting Location…' : 'Use My Location'}
                    </button>

                    {locationStatus === 'denied' && (
                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-100">
                        Location access was denied or unavailable. Enter coordinates manually below instead.
                      </div>
                    )}

                    <div className="pt-2 border-t border-white/5 space-y-3">
                      <div className="text-xs text-zinc-500 uppercase tracking-wide font-bold">Or Enter Manually</div>
                      <div className="grid grid-cols-2 gap-3">
                        <input
                          value={manualLat}
                          onChange={(e) => setManualLat(e.target.value)}
                          placeholder="Latitude"
                          className="rounded-xl bg-black/60 border border-white/10 p-3 min-h-12 text-sm outline-none"
                        />
                        <input
                          value={manualLon}
                          onChange={(e) => setManualLon(e.target.value)}
                          placeholder="Longitude"
                          className="rounded-xl bg-black/60 border border-white/10 p-3 min-h-12 text-sm outline-none"
                        />
                      </div>
                      <button
                        onClick={useManualCoords}
                        className="w-full rounded-2xl py-3 border border-white/10 bg-white/[0.03] text-sm font-bold hover:bg-white/[0.06] transition-all duration-300"
                      >
                        Use These Coordinates
                      </button>
                    </div>
                  </div>
                )}

                {coords && !planetaryDay && (
                  <div className="rounded-3xl border border-red-500/20 bg-red-500/10 p-5 text-sm text-red-100">
                    Couldn't calculate sunrise/sunset for this location (this can happen at extreme polar latitudes). Try a different coordinate.
                  </div>
                )}

                {coords && planetaryDay && (
                  <>
                    <div className="flex items-center justify-between text-xs text-zinc-500">
                      <span>{coords.lat.toFixed(2)}°, {coords.lon.toFixed(2)}°</span>
                      <button
                        onClick={() => { setCoords(null); setLocationStatus('idle'); setDayOffset(0); }}
                        className="underline hover:text-zinc-300"
                      >
                        Change Location
                      </button>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setDayOffset((d) => d - 1)}
                        className="rounded-xl px-3 py-2 min-h-12 border border-white/10 bg-white/[0.03] text-sm hover:bg-white/[0.06] transition-all duration-300"
                      >
                        ← Prev Day
                      </button>
                      <div className="flex-1 text-center text-sm font-bold">
                        {viewedDayLabel}
                      </div>
                      {dayOffset !== 0 && (
                        <button
                          onClick={() => setDayOffset(0)}
                          className="rounded-xl px-3 py-2 min-h-12 border border-white/10 bg-white/[0.03] text-xs hover:bg-white/[0.06] transition-all duration-300"
                        >
                          Today
                        </button>
                      )}
                      <button
                        onClick={() => setDayOffset((d) => d + 1)}
                        className="rounded-xl px-3 py-2 min-h-12 border border-white/10 bg-white/[0.03] text-sm hover:bg-white/[0.06] transition-all duration-300"
                      >
                        Next Day →
                      </button>
                    </div>

                    {dayOffset !== 0 && (
                      <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-3 text-xs text-zinc-400">
                        Viewing {viewedDayLabel.toLowerCase()}'s schedule — the live countdown only shows for today.
                      </div>
                    )}

                    {currentHour && (() => {
                      const p = PLANETS_BY_NAME[currentHour.planet];
                      const remaining = currentHour.end.getTime() - now.getTime();
                      const total = currentHour.end.getTime() - currentHour.start.getTime();
                      const progress = Math.min(100, Math.max(0, ((total - remaining) / total) * 100));
                      return (
                        <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-5 space-y-4">
                          <div className="text-xs uppercase tracking-wide text-zinc-500 font-bold">
                            Current Hour · {currentHour.isDay ? 'Day' : 'Night'} {(currentHour.index % 12) + 1} of 12
                          </div>
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="text-3xl font-black" style={{ color: p?.color }}>{currentHour.planet}</div>
                              <div className="text-sm text-zinc-400 mt-1">
                                {formatClock(currentHour.start)} – {formatClock(currentHour.end)}
                              </div>
                            </div>
                            <div className="text-right">
                              <div className="text-2xl font-black">{formatDuration(remaining)}</div>
                              <div className="text-xs text-zinc-500">remaining</div>
                            </div>
                          </div>
                          <div className="h-2 rounded-full bg-white/5 overflow-hidden">
                            <div className="h-full rounded-full transition-all duration-1000" style={{ width: `${progress}%`, backgroundColor: p?.color }} />
                          </div>
                          {p && (
                            <div className="flex flex-wrap gap-2 pt-1">
                              {p.powers.slice(0, 5).map((power) => (
                                <div key={power} className="px-3 py-1.5 rounded-full text-xs border border-white/10 bg-white/[0.03]">
                                  {power}
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })()}

                    {notifPermission === 'unsupported' && (
                      <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-3 text-xs text-zinc-400">
                        Notifications aren't supported in this preview. In the packaged app, this becomes a real scheduled alarm via Capacitor's Local Notifications, working even when the app is closed.
                      </div>
                    )}
                    {notifPermission === 'denied' && (
                      <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-3 text-xs text-red-100">
                        Notification permission was denied — enable it in your browser/app settings to use hour reminders.
                      </div>
                    )}

                    <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4">
                      <div className="flex items-center gap-2 mb-3">
                        <Bell className="text-green-300" size={18} />
                        <h3 className="font-black">Reminder Lead Time</h3>
                      </div>
                      <p className="text-xs text-zinc-400 mb-3">
                        Get a heads-up notification before the ringing alarm, or disable the heads-up.
                      </p>
                      <div className="grid grid-cols-3 gap-2">
                        {[0, 5, 10, 15, 30, 60].map((mins) => {
                          const active = leadMinutes === mins;
                          return (
                            <button
                              key={mins}
                              onClick={() => setLeadMinutes(mins)}
                              className={`rounded-2xl py-3 border text-sm font-bold transition-all duration-300 ${active ? 'border-green-400 bg-green-500/20 text-green-100' : 'border-white/10 bg-white/[0.03] text-zinc-400 hover:bg-white/[0.06]'}`}
                            >
                              {mins === 0 ? 'None' : mins >= 60 ? '1h' : `${mins}m`}
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl overflow-hidden">
                      <div className="p-4 border-b border-white/5 flex items-center gap-2">
                        <Clock className="text-green-300" size={18} />
                        <h3 className="font-black">{viewedDayLabel}'s 24 Hours</h3>
                      </div>
                      <div className="max-h-[480px] overflow-y-auto divide-y divide-white/5">
                        {planetaryDay.hours.map((h) => {
                          const p = PLANETS_BY_NAME[h.planet];
                          const isCurrent = currentHour && currentHour.key === h.key;
                          const isPast = h.end <= now;
                          const notified = notifyKeys.has(h.key);
                          return (
                            <div
                              key={h.key}
                              className={`flex items-center justify-between p-3 gap-3 ${isCurrent ? 'bg-white/[0.06]' : ''} ${isPast ? 'opacity-40' : ''}`}
                            >
                              <div className="flex items-center gap-3 min-w-0">
                                <div className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: p?.color }} />
                                <div className="min-w-0">
                                  <div className="font-bold text-sm truncate">{h.planet}</div>
                                  <div className="text-xs text-zinc-500">
                                    {formatClock(h.start)} – {formatClock(h.end)} · {h.isDay ? 'Day' : 'Night'} {(h.index % 12) + 1}
                                  </div>
                                </div>
                              </div>
                              <button
                                onClick={() => toggleNotify(h)}
                                disabled={isPast}
                                className={`shrink-0 rounded-xl p-2 min-h-12 min-w-12 border transition-all duration-300 ${notified ? 'border-green-400 bg-green-500/20 text-green-200' : 'border-white/10 bg-white/[0.03] text-zinc-500 hover:bg-white/[0.06]'} ${isPast ? 'opacity-30 cursor-not-allowed' : ''}`}
                              >
                                {notified ? <Bell size={16} /> : <BellOff size={16} />}
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    <p className="text-xs text-zinc-500">
                      Reminders fire while this app is open. Once packaged as an Android app, hour reminders become real scheduled OS notifications that work even if the app is closed.
                    </p>
                  </>
                )}
              </div>
            )}

            {menuSection === 'tool' && (
              <div className="space-y-4 mt-6">
                <h2 className="text-3xl font-black text-yellow-200">Planetary Sigil Generator</h2>
                <p className="text-zinc-400 text-sm">Use the main interface to create ceremonial chaos sigils using traditional planetary kameas and customizable reduction systems.</p>
                <button
                  onClick={() => setMenuOpen(false)}
                  className="rounded-2xl px-6 py-4 bg-yellow-500/20 border border-yellow-400/30 text-yellow-100 font-bold hover:scale-[1.02] transition-all"
                >
                  Open Planetary Sigil Generator
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="max-w-[1600px] mx-auto p-4 grid grid-cols-1 xl:grid-cols-[420px_minmax(0,1fr)] gap-4 safe-bottom">
        <div className="space-y-4 min-w-0">
          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4 shadow-[0_0_60px_rgba(139,92,246,0.08)]">
            <div className="flex items-center gap-2 mb-4">
              <ScrollText className="text-violet-300" size={18} />
              <h2 className="font-black">Intention</h2>
            </div>

            <textarea
              rows={4}
              value={intention}
              onChange={(e) => setIntention(e.target.value)}
              className="w-full rounded-2xl bg-black/60 border border-white/10 p-4 resize-none outline-none text-sm"
            />
          </div>

          <div className="flex gap-3 overflow-x-auto pb-2">
            {PLANETS.map((item) => (
              <button
                key={item.name}
                onClick={() => setPlanet(item)}
                className={`flex-shrink-0 rounded-3xl px-4 py-4 border transition-all duration-300 min-w-[100px] ${planet.name === item.name ? 'scale-105 shadow-[0_0_40px_rgba(255,255,255,0.12)]' : 'opacity-80 hover:opacity-100'}`}
                style={{
                  borderColor: item.color,
                  background: `${item.color}18`,
                }}
              >
                <div className="text-3xl mb-2" style={{ color: item.color }}>{item.symbol}</div>
                <div className="text-xs font-bold">{item.name}</div>
              </button>
            ))}
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl overflow-hidden">
            <button
              onClick={() => setShowPowers(!showPowers)}
              className="w-full p-4 flex items-center justify-between"
            >
              <div className="flex items-center gap-2">
                <MoonStar className="text-pink-300" size={18} />
                <h2 className="font-black">{planet.name} Powers</h2>
              </div>
              <div>{showPowers ? '−' : '+'}</div>
            </button>

            {showPowers && (
              <div className="border-t border-white/5 p-4 flex flex-wrap gap-2">
                {planet.powers.map((power) => (
                  <div
                    key={power}
                    className="px-3 py-2 rounded-full text-xs border border-white/10 bg-white/[0.03]"
                  >
                    {power}
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4">
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => setRemoveVowels(!removeVowels)}
                className={`rounded-2xl py-4 border text-sm font-bold transition-all duration-300 ${removeVowels ? 'border-violet-400 bg-violet-500/20 text-violet-100 shadow-[0_0_30px_rgba(139,92,246,0.45)] scale-[1.02]' : 'border-violet-500/20 bg-violet-500/5 text-zinc-400 hover:bg-violet-500/10'}`}
              >
                Remove Vowels
              </button>

              <button
                onClick={() => setRemoveRepeats(!removeRepeats)}
                className={`rounded-2xl py-4 border text-sm font-bold transition-all duration-300 ${removeRepeats ? 'border-cyan-400 bg-cyan-500/20 text-cyan-100 shadow-[0_0_30px_rgba(6,182,212,0.45)] scale-[1.02]' : 'border-cyan-500/20 bg-cyan-500/5 text-zinc-400 hover:bg-cyan-500/10'}`}
              >
                Remove Repeating Letters
              </button>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4">
            <div className="flex items-center gap-2 mb-4">
              <Sigma className="text-cyan-300" size={18} />
              <h2 className="font-black">Reduction Process</h2>
            </div>

            <div className="space-y-4 text-sm">
              <div>
                <div className="text-zinc-500 mb-1">Original Phrase</div>
                <div className="break-all">{uppercaseText}</div>
              </div>

              <div>
                <div className="text-zinc-500 mb-1">After Vowel Reduction</div>
                <div className="break-all text-cyan-200 tracking-[0.15em]">{noVowelsText}</div>
              </div>

              <div>
                <div className="text-zinc-500 mb-1">Final Sigil Letters</div>
                <div className="break-all text-violet-200 font-black tracking-[0.2em]">{sigilText}</div>
              </div>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl overflow-hidden">
            <button
              onClick={() => setShowNumbers(!showNumbers)}
              className="w-full p-4 flex items-center justify-between"
            >
              <h2 className="font-black">Number Transformation System</h2>
              <div>{showNumbers ? '−' : '+'}</div>
            </button>

            {showNumbers && (
              <div className="border-t border-white/5 p-4 max-h-[340px] overflow-y-auto space-y-3">
                {transformationSteps.map((step, index) => (
                  <div key={index} className="rounded-2xl border border-white/10 bg-black/40 p-4 flex items-center justify-between text-sm">
                    <div>
                      <div className="font-black text-violet-200 text-lg">{step.letter}</div>
                      <div className="text-zinc-500">Alphabet Value: {step.alphabetNumber}</div>
                    </div>

                    <div className="w-12 h-12 rounded-full bg-violet-500/20 border border-violet-400/30 flex items-center justify-center text-violet-100 font-black">
                      {step.planetaryNumber}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4">
            <h2 className="font-black mb-4">Sigil Customization</h2>

            <div className="space-y-5">
              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span>Stroke Width</span>
                  <span>{strokeWidth}</span>
                </div>

                <input
                  type="range"
                  min="1"
                  max="12"
                  value={strokeWidth}
                  onChange={(e) => setStrokeWidth(Number(e.target.value))}
                  className="w-full"
                />
              </div>

              <div>
                <div className="flex justify-between text-sm mb-2">
                  <span>Curvature</span>
                  <span>{curvature}</span>
                </div>

                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  value={curvature}
                  onChange={(e) => setCurvature(Number(e.target.value))}
                  className="w-full"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-4 min-w-0">
          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4 overflow-hidden">
            <div className="flex items-center gap-2 mb-4">
              <Orbit className="text-cyan-300" size={18} />
              <h2 className="text-2xl font-black">Kamea Matrix</h2>
            </div>

            <div className="rounded-3xl border border-white/10 bg-black/60 p-4 flex items-center justify-center overflow-hidden">
              <svg viewBox="0 0 360 360" className="w-full max-w-[700px] aspect-square">
                {planet.grid.map((row, rowIndex) =>
                  row.map((value, colIndex) => {
                    const cell = 360 / planet.size;

                    return (
                      <rect
                        key={`grid-${rowIndex}-${colIndex}`}
                        x={colIndex * cell}
                        y={rowIndex * cell}
                        width={cell}
                        height={cell}
                        fill="none"
                        stroke="rgba(255,255,255,0.1)"
                      />
                    );
                  })
                )}

                <path
                  d={pathData}
                  fill="none"
                  stroke={planet.color}
                  strokeWidth={strokeWidth}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />

                {planet.grid.map((row, rowIndex) =>
                  row.map((value, colIndex) => {
                    const cell = 360 / planet.size;

                    return (
                      <g key={`num-${rowIndex}-${colIndex}`}>
                        <circle
                          cx={colIndex * cell + cell / 2}
                          cy={rowIndex * cell + cell / 2}
                          r={planet.size >= 8 ? 8 : 11}
                          fill="rgba(0,0,0,0.85)"
                        />

                        <text
                          x={colIndex * cell + cell / 2}
                          y={rowIndex * cell + cell / 2}
                          textAnchor="middle"
                          dominantBaseline="middle"
                          fill="white"
                          fontSize={planet.size >= 8 ? 7 : 10}
                          fontWeight="bold"
                        >
                          {value}
                        </text>
                      </g>
                    );
                  })
                )}
              </svg>
            </div>
          </div>

          <div className="rounded-3xl border border-white/10 bg-zinc-950/90 backdrop-blur-xl p-4 overflow-hidden">
            <div className="flex items-center gap-2 mb-4">
              <Sparkles className="text-yellow-300" size={18} />
              <h2 className="text-2xl font-black">Final Sigil Output</h2>
            </div>

            <div className="rounded-3xl border border-white/10 bg-black/60 min-h-[280px] flex items-center justify-center p-6 overflow-hidden cursor-pointer group relative" onClick={() => setSigilModalOpen(true)}>
              <svg viewBox="0 0 360 360" className="w-full max-w-[280px] aspect-square">
                {buildSeal()}
                <g transform="translate(180 180) scale(0.5) translate(-180 -180)">
                  <path
                    d={pathData}
                    fill="none"
                    stroke={planet.color}
                    strokeWidth={strokeWidth}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </g>
              </svg>
              <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-black/40 rounded-3xl">
                <div className="px-4 py-2 rounded-xl bg-white/10 backdrop-blur-md border border-white/20 text-sm font-bold text-white">
                  Click to Expand
                </div>
              </div>
            </div>

            <div className="mt-3 rounded-2xl border border-white/10 bg-black/40 p-3 space-y-2 text-xs">
              <div className="flex items-center justify-between gap-3">
                <span className="text-zinc-500 grow">Wording</span>
                <div className="flex gap-1">
                  <button
                    onClick={() => setShowCorrLang('en')}
                    className={`px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${showCorrLang === 'en' ? 'border-violet-400 bg-violet-500/20 text-violet-100' : 'border-white/10 bg-white/[0.03] text-zinc-500'}`}
                  >
                    EN
                  </button>
                  <button
                    onClick={() => setShowCorrLang('he')}
                    className={`px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${showCorrLang === 'he' ? 'border-violet-400 bg-violet-500/20 text-violet-100' : 'border-white/10 bg-white/[0.03] text-zinc-500'}`}
                  >
                    עברית
                  </button>
                </div>
              </div>
              <div className="flex items-center justify-between gap-3">
                <span className="text-zinc-500 grow">Planet Sign</span>
                <button
                  onClick={() => setShowSign(!showSign)}
                  className={`px-3 py-1.5 rounded-full text-xs font-bold border transition-all ${showSign ? 'border-yellow-400 bg-yellow-500/20 text-yellow-100' : 'border-white/10 bg-white/[0.03] text-zinc-500'}`}
                >
                  {showSign ? 'On' : 'Off'}
                </button>
              </div>
              {[
                { key: 'archangel', label: 'Archangel', get: () => setShowArchangel(!showArchangel), on: showArchangel, val: corr.archangel?.en },
                { key: 'intelligence', label: 'Intelligence', get: () => setShowIntelligence(!showIntelligence), on: showIntelligence, val: corr.intelligence?.en },
                { key: 'spirit', label: 'Spirit', get: () => setShowSpirit(!showSpirit), on: showSpirit, val: corr.spirit?.en },
                { key: 'divine', label: 'Divine Name', get: () => setShowDivineName(!showDivineName), on: showDivineName, val: corr.divine?.en },
              ].map((row) => (
                <div key={row.key} className="flex items-center justify-between gap-3">
                  <span className="text-zinc-500 grow">{row.label}</span>
                  <span className="flex items-center gap-2">
                    {row.on && <span className="text-zinc-300 truncate max-w-[120px]">{row.val}</span>}
                    <button
                      onClick={row.get}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold border transition-all shrink-0 ${row.on ? 'border-cyan-400 bg-cyan-500/20 text-cyan-100' : 'border-white/10 bg-white/[0.03] text-zinc-500'}`}
                    >
                      {row.on ? 'On' : 'Off'}
                    </button>
                  </span>
                </div>
              ))}
            </div>

            {showArchangel || showIntelligence || showSpirit || showDivineName ? (
              <div className="mt-3 rounded-2xl border border-white/10 bg-black/40 p-3 space-y-2 text-xs">
                <div className="text-[10px] uppercase tracking-widest text-zinc-500 font-bold">Inscribed names</div>
                {showArchangel && <div className="flex justify-between gap-3"><span className="text-zinc-500">Archangel</span><span className="text-white text-right">{archText}</span></div>}
                {showIntelligence && <div className="flex justify-between gap-3"><span className="text-zinc-500">Intelligence</span><span className="text-white text-right">{intelText}</span></div>}
                {showSpirit && <div className="flex justify-between gap-3"><span className="text-zinc-500">Spirit</span><span className="text-white text-right">{spiritText}</span></div>}
                {showDivineName && <div className="flex justify-between gap-3"><span className="text-zinc-500">Divine Name</span><span className="text-zinc-200 text-right" dir={showCorrLang === 'he' ? 'rtl' : undefined}>{divineText}</span></div>}
              </div>
            ) : null}

            <div className="mt-3 grid grid-cols-3 gap-2">
              <button onClick={() => downloadSigil('svg')} disabled={!pathData} className="rounded-2xl py-3 border border-white/10 bg-white/[0.03] text-xs font-bold hover:bg-white/[0.06] transition-all duration-300 disabled:opacity-30 disabled:cursor-not-allowed">SVG</button>
              <button onClick={() => downloadSigil('png')} disabled={!pathData} className="rounded-2xl py-3 border border-white/10 bg-white/[0.03] text-xs font-bold hover:bg-white/[0.06] transition-all duration-300 disabled:opacity-30 disabled:cursor-not-allowed">PNG</button>
              <button onClick={() => downloadSigil('jpeg')} disabled={!pathData} className="rounded-2xl py-3 border border-white/10 bg-white/[0.03] text-xs font-bold hover:bg-white/[0.06] transition-all duration-300 disabled:opacity-30 disabled:cursor-not-allowed">JPEG</button>
            </div>
            <div className="mt-2">
              <button
                onClick={testDownload}
                className="w-full rounded-2xl py-3 border border-dashed border-emerald-400/40 bg-emerald-500/10 text-emerald-100 text-xs font-bold hover:bg-emerald-500/20 transition-all duration-300"
              >
                Test download on this device
              </button>
            </div>
          </div>

          {sigilModalOpen && (
            <div className="fixed inset-0 z-[60] bg-black/90 backdrop-blur-xl flex items-center justify-center p-4" onClick={closeSigilModal}>
              <div className="relative max-w-[90vw] max-h-[90dvh] pb-[env(safe-area-inset-bottom,1rem)]" onClick={(e) => e.stopPropagation()}>
                <button
                  onClick={closeSigilModal}
                  aria-label="Close sigil"
                  className="absolute -top-12 right-0 w-10 h-10 rounded-full border border-white/10 bg-zinc-900 flex items-center justify-center hover:scale-105 transition-all"
                >
                  <X size={18} />
                </button>

                <div
                  className="rounded-3xl border border-white/10 bg-black p-8 select-none cursor-pointer relative"
                  onPointerDown={startSigilPress}
                  onPointerUp={cancelSigilPress}
                  onPointerLeave={cancelSigilPress}
                  onPointerCancel={cancelSigilPress}
                  onPointerMove={moveSigilPress}
                >
                  <svg viewBox="0 0 360 360" className="w-[600px] max-w-full aspect-square">
                    {buildSeal()}
                    <g transform="translate(180 180) scale(0.5) translate(-180 -180)">
                      <path
                        d={pathData}
                        fill="none"
                        stroke={planet.color}
                        strokeWidth={strokeWidth * 1.5}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </g>
                  </svg>

                  {modalChooserOpen && (
                    <div
                      className="absolute bottom-6 left-1/2 -translate-x-1/2 flex gap-2 bg-black/90 border border-white/10 backdrop-blur-md rounded-2xl p-2"
                      onPointerDown={(e) => e.stopPropagation()}
                      onPointerUp={(e) => e.stopPropagation()}
                      onPointerMove={(e) => e.stopPropagation()}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <button
                        onClick={() => downloadSigil('svg')}
                        className="px-4 py-3 rounded-xl border border-white/10 bg-white/[0.06] text-sm font-bold hover:bg-white/[0.12] transition-all min-h-11"
                      >
                        SVG
                      </button>
                      <button
                        onClick={() => downloadSigil('png')}
                        className="px-4 py-3 rounded-xl border border-white/10 bg-white/[0.06] text-sm font-bold hover:bg-white/[0.12] transition-all min-h-11"
                      >
                        PNG
                      </button>
                      <button
                        onClick={() => downloadSigil('jpeg')}
                        className="px-4 py-3 rounded-xl border border-white/10 bg-white/[0.06] text-sm font-bold hover:bg-white/[0.12] transition-all min-h-11"
                      >
                        JPEG
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
