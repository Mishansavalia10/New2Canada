// Shared helpers used by every page.

// Thrown by api.get/post. `status` is the HTTP status (0 when the network failed).
class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
    }
}

const FRIENDLY_STATUS = {
    400: 'That input is too long. Shorten it and try again.',
    401: 'Please sign in to use this feature.',
    404: 'That service could not be found.',
    409: 'Something is already running. Please try again shortly.',
    429: 'Too many requests. Please wait a moment and try again.',
};

async function request(path, options) {
    let r;
    try {
        r = await fetch(path, options);
    } catch (_) {
        throw new ApiError("Can't reach the server. Check that it is running, then try again.", 0);
    }
    if (!r.ok) {
        let serverMsg = '';
        try { serverMsg = (await r.json()).error || ''; } catch (_) { /* not JSON */ }
        const fallback = r.status >= 500
            ? 'The server ran into a problem. Please try again.'
            : 'The request failed. Please try again.';
        throw new ApiError(serverMsg || FRIENDLY_STATUS[r.status] || fallback, r.status);
    }
    return r.json();
}

window.api = {
    async get(path) {
        return request(path, { headers: await authHeaders() });
    },
    async post(path, formData) {
        const headers = await authHeaders();
        headers['Content-Type'] = 'application/x-www-form-urlencoded';
        const body = new URLSearchParams(formData).toString();
        return request(path, { method: 'POST', headers, body });
    }
};

async function authHeaders() {
    const headers = {};
    try {
        if (window.getIdToken) {
            const token = await window.getIdToken();
            if (token) headers['Authorization'] = 'Bearer ' + token;
        }
    } catch (_) {
        // Sign-in is optional for search: fall back to anonymous requests.
    }
    return headers;
}

// Demo-mode banner toggler. Injects the banner text so every page can just
// drop in an empty <div id="demoBanner"> and we'll fill it.
async function checkDemoBanner() {
    try {
        const stats = await fetch('/api/debug').then(r => r.json());
        const banner = document.getElementById('demoBanner');
        if (banner && stats.demoMode) {
            if (!banner.innerHTML.trim()) {
                banner.innerHTML =
                    'Running in <b>DEMO mode</b>. Auth & cloud persistence are disabled — ' +
                    'add <code>serviceAccountKey.json</code> and your Firebase web config ' +
                    '(see <code>README.md</code>) to enable FULL mode.';
            }
            banner.style.display = 'block';
        }
    } catch (e) { /* server warming up */ }
}

// ---- Result card renderer ---------------------------------------------------
// One distinct colour per crawled source site, as a tonal chip: a soft
// tinted background with darker same-hue text. Dark-text-on-light-tint keeps
// every badge readable (WCAG AA) while allowing pleasant hues — gold, cyan,
// lime — that are too light to work as solid white-on-colour badges. Unknown
// sources fall back to a neutral slate chip.
const SOURCE_COLORS = {
    'kijiji.ca':      { bg: '#E8F5E9', text: '#1B5E20' }, // green
    'craigslist.org': { bg: '#F3E5F5', text: '#6A1B9A' }, // purple
    'zumper.com':     { bg: '#FFEBEE', text: '#C62828' }, // red
    'padmapper.com':  { bg: '#E0F2F1', text: '#00695C' }, // teal
    'viewit.ca':      { bg: '#E3F2FD', text: '#1565C0' }, // blue
    '4rent.ca':       { bg: '#FFF3E0', text: '#E65100' }, // orange
    'rentseeker.ca':  { bg: '#FCE4EC', text: '#AD1457' }, // pink
    'realtor.ca':     { bg: '#FEF3C7', text: '#B45309' }, // gold
    'liv.rent':       { bg: '#E0F7FA', text: '#0E7490' }, // cyan
    'rentola.ca':     { bg: '#F0FDD4', text: '#4D7C0F' }, // lime
};
function sourceChip(source) {
    return SOURCE_COLORS[String(source).toLowerCase()] || { bg: '#F1F5F9', text: '#475569' };
}

function renderResult(item) {
    const fields = item.fields || {};
    const cityProvince = [fields.city, fields.province].filter(Boolean).map(escape).join(', ');
    const safeUrl = item.url && item.url.startsWith('http') ? item.url : '#';

    const detailRows = [];
    if (fields.address) detailRows.push(`
        <div class="flex items-center gap-2 text-on-surface-variant text-body-sm">
            <span aria-hidden="true">🏠</span><span>${escape(fields.address)}</span>
        </div>`);
    if (cityProvince) detailRows.push(`
        <div class="flex items-center gap-2 text-on-surface-variant text-body-sm">
            <span aria-hidden="true">📍</span><span>${cityProvince}</span>
        </div>`);
    if (fields.bedrooms !== undefined) detailRows.push(`
        <div class="flex items-center gap-2 text-on-surface-variant text-body-sm">
            <span aria-hidden="true">🛏</span><span>${escape(String(fields.bedrooms))} BR</span>
        </div>`);

    const chip = sourceChip(item.source);
    const sourceBadge = item.source
        ? `<span class="shrink-0 font-label-sm px-2 py-1 rounded-md text-[10px] uppercase tracking-wider font-bold" style="background:${chip.bg};color:${chip.text}">${escape(item.source)}</span>`
        : '';
    const priceBadge = fields.price
        ? `<span class="bg-tertiary text-white font-label-md text-label-md px-3 py-1.5 rounded-full shadow-sm">$${escape(String(fields.price))}/mo</span>`
        : '<span></span>';

    return `
        <article class="apartment-card bg-white rounded-[24px] overflow-hidden border border-outline-variant/30 flex flex-col group">
            <div class="p-6 flex flex-col flex-grow">
                <div class="flex items-start justify-between gap-2 mb-3">
                    <h3 class="font-headline-sm text-headline-sm line-clamp-2">${escape(item.title || 'Untitled')}</h3>
                    ${sourceBadge}
                </div>
                <div class="space-y-3 mb-6">
                    ${detailRows.join('')}
                </div>
                <div class="mt-auto flex items-center justify-between gap-3">
                    ${priceBadge}
                    <a href="${safeUrl}" target="_blank" rel="noopener noreferrer" class="flex-1 text-center py-3 bg-primary text-white font-label-md text-label-md rounded-xl hover:bg-primary-container transition-colors active:scale-95">View listing</a>
                </div>
            </div>
        </article>`;
}

function escape(s) {
    return String(s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function skeletonList(n) {
    return Array.from({ length: n }, () => `
        <div class="skeleton-card">
            <div class="skeleton-line" style="width:60%"></div>
            <div class="skeleton-line" style="width:40%"></div>
            <div class="skeleton-line" style="width:80%"></div>
            <div class="skeleton-line" style="width:50%"></div>
        </div>`).join('');
}

function qsParam(name) {
    return new URLSearchParams(window.location.search).get(name) || '';
}

// Replaces a results area with a readable message and a retry button.
function renderErrorState(el, message, onRetry) {
    el.innerHTML = `
        <div class="empty-card md:col-span-2 lg:col-span-3" role="alert">
            <h3>Something didn't load</h3>
            <p>${escape(message)}</p>
            <button type="button" class="retry-btn mt-4 px-5 py-2 rounded-full bg-primary text-white font-label-md text-label-md">Try again</button>
        </div>`;
    const btn = el.querySelector('.retry-btn');
    if (btn && onRetry) btn.addEventListener('click', onRetry);
}

window.renderResult = renderResult;
window.renderErrorState = renderErrorState;
window.skeletonList = skeletonList;
window.qsParam      = qsParam;
window.escapeHtml   = escape;

document.addEventListener('DOMContentLoaded', checkDemoBanner);
