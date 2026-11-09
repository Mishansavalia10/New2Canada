// Single-page controller.
//
// The master search box at the top fans a single query out to every feature —
// apartment search, autocomplete, spell check and validate all light up at
// once. Each feature underneath also keeps its own input so it can be driven
// by hand independently of the master box.

const TYPE = 'apartments';

/* =========================================================================
 * Live stats — refreshed on load and after every search so "Searches served"
 * (and the index/trie/dictionary sizes) stay current without a page reload.
 * ====================================================================== */
async function loadStats() {
    try {
        const s = await fetch('/api/debug').then(r => r.json());
        setText('statDocs', s.totalDocs);
        setText('statTerms', s.trieSize);
        setText('statDict', s.dictionarySize);
        setText('statQueries', s.totalQueries);
    } catch (e) { /* warming up */ }
}
loadStats();
function setText(id, v) { const el = document.getElementById(id); if (el) el.textContent = v; }

/* =========================================================================
 * Province / city filter data (mirrors the old apartments page)
 * ====================================================================== */
const ALL_CITIES_BY_PROVINCE = {
    AB: ['Calgary', 'Edmonton', 'Red Deer', 'Lethbridge', 'St. Albert', 'Medicine Hat', 'Grande Prairie', 'Airdrie', 'Spruce Grove', 'Leduc', 'Fort McMurray'],
    BC: ['Vancouver', 'Victoria', 'Surrey', 'Burnaby', 'Richmond', 'Abbotsford', 'Kelowna', 'Kamloops', 'Nanaimo', 'Prince George', 'Coquitlam', 'Langley'],
    MB: ['Winnipeg', 'Brandon', 'Steinbach', 'Thompson', 'Portage la Prairie'],
    NB: ['Saint John', 'Moncton', 'Fredericton', 'Dieppe', 'Miramichi'],
    NL: ["St. John's", 'Mount Pearl', 'Corner Brook', 'Conception Bay South'],
    NS: ['Halifax', 'Sydney', 'Dartmouth', 'Truro', 'New Glasgow'],
    NT: ['Yellowknife', 'Hay River', 'Inuvik'],
    NU: ['Iqaluit', 'Rankin Inlet', 'Arviat'],
    ON: ['Toronto', 'Ottawa', 'Mississauga', 'Brampton', 'Hamilton', 'London', 'Markham', 'Vaughan', 'Kitchener', 'Windsor', 'Richmond Hill', 'Oakville', 'Burlington', 'Oshawa', 'Barrie', 'St. Catharines', 'Guelph', 'Cambridge', 'Whitby', 'Kingston', 'Waterloo', 'Sudbury', 'Thunder Bay', 'Niagara Falls'],
    PE: ['Charlottetown', 'Summerside', 'Stratford', 'Cornwall'],
    QC: ['Montreal', 'Quebec City', 'Laval', 'Gatineau', 'Longueuil', 'Sherbrooke', 'Saguenay', 'Levis', 'Trois-Rivieres', 'Terrebonne', 'Saint-Jean-sur-Richelieu', 'Repentigny', 'Brossard'],
    SK: ['Saskatoon', 'Regina', 'Prince Albert', 'Moose Jaw', 'Swift Current', 'Yorkton'],
    YT: ['Whitehorse', 'Dawson City', 'Watson Lake'],
};
const ALL_PROVINCES = Object.keys(ALL_CITIES_BY_PROVINCE);

const provSel  = document.getElementById('provinceFilter');
const citySel  = document.getElementById('cityFilter');
const clearBtn = document.getElementById('clearFilters');
let citiesByProvince = {};

async function loadFacets() {
    try {
        const data = await api.get('/api/facets?type=' + TYPE);
        const crawled = data.citiesByProvince || {};
        citiesByProvince = {};
        for (const prov of ALL_PROVINCES) {
            citiesByProvince[prov] = Array.from(new Set([
                ...(ALL_CITIES_BY_PROVINCE[prov] || []),
                ...(crawled[prov] || []),
            ])).sort();
        }
        for (const prov of Object.keys(crawled)) {
            if (!citiesByProvince[prov]) citiesByProvince[prov] = crawled[prov];
        }
        const provinces = Array.from(new Set([...ALL_PROVINCES, ...(data.provinces || [])])).sort();
        fillSelect(provSel, provinces, 'Any province', '');
        fillSelect(citySel, [], 'Select a province first', '');
        citySel.disabled = true;
    } catch (e) {
        // Facets are optional: keep the province list usable from the static data.
        fillSelect(provSel, ALL_PROVINCES, 'Any province', '');
        fillSelect(citySel, [], 'Select a province first', '');
        citySel.disabled = true;
    }
}

function fillSelect(sel, values, anyLabel, preselect) {
    sel.innerHTML = `<option value="">${anyLabel}</option>` +
        values.map(v => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
    if (preselect && values.includes(preselect)) sel.value = preselect;
}

provSel.addEventListener('change', () => {
    if (provSel.value) {
        fillSelect(citySel, citiesByProvince[provSel.value] || [], 'Any city', '');
        citySel.disabled = false;
    } else {
        fillSelect(citySel, [], 'Select a province first', '');
        citySel.disabled = true;
    }
    runApartments();
});
citySel.addEventListener('change', runApartments);
clearBtn.addEventListener('click', () => {
    aptQ.value = '';
    provSel.value = '';
    fillSelect(citySel, [], 'Select a province first', '');
    citySel.disabled = true;
    runApartments();
});

function filterQuery() {
    const parts = [];
    if (provSel.value) parts.push('province=' + encodeURIComponent(provSel.value));
    if (citySel.value) parts.push('city='     + encodeURIComponent(citySel.value));
    return parts.length ? '&' + parts.join('&') : '';
}

/* =========================================================================
 * Apartments feature
 * ====================================================================== */
const aptQ       = document.getElementById('aptQ');
const aptResults = document.getElementById('aptResults');
const aptCount   = document.getElementById('aptCount');
const aptDym     = document.getElementById('aptDym');
const aptAcList  = document.getElementById('aptAcList');

async function runApartments() {
    const query = aptQ.value.trim();
    if (query) return runApartmentSearch(query);
    return runTopPicks();
}

async function runApartmentSearch(query) {
    aptDym.innerHTML = '';
    aptCount.textContent = '';
    aptResults.innerHTML = skeletonList(3);
    try {
        const data = await api.get(`/api/search?type=${TYPE}&q=${encodeURIComponent(query)}${filterQuery()}`);
        if (data.didYouMean) {
            aptDym.innerHTML = `<div class="did-you-mean">Did you mean <a href="#" id="aptDymLink">${escapeHtml(data.didYouMean)}</a>?</div>`;
            document.getElementById('aptDymLink').onclick = (ev) => {
                ev.preventDefault(); aptQ.value = data.didYouMean; runApartmentSearch(data.didYouMean);
            };
        }
        renderAptCount(data.results.length, query);
        if (data.results.length) {
            showResults(data.results);
        } else {
            aptResults.innerHTML = `<div class="empty-card md:col-span-2 lg:col-span-3"><h3>No matches</h3><p>Try a broader term or clear filters.</p></div>`;
            aptPager.innerHTML = '';
        }
        loadHistory();   // the server just recorded this query — reflect it now
        loadStats();     // bump "Searches served" without a page reload
    } catch (e) {
        renderErrorState(aptResults, e.message, () => runApartmentSearch(query));
        aptPager.innerHTML = '';
    }
}

async function runTopPicks() {
    aptDym.innerHTML = '';
    aptCount.textContent = '';
    aptResults.innerHTML = skeletonList(3);
    try {
        const data = await api.get(`/api/pagerank?type=${TYPE}${filterQuery()}`);
        renderAptCount((data.top || []).length, null);
        if (!data.top.length) {
            const active = provSel.value || citySel.value;
            aptResults.innerHTML = active
                ? `<div class="empty-card md:col-span-2 lg:col-span-3">No apartments match the current filters.</div>`
                : `<div class="empty-card md:col-span-2 lg:col-span-3"><h3>Still indexing…</h3><p>The crawler hasn't finished its first pass. Refresh in a few seconds.</p></div>`;
            aptPager.innerHTML = '';
            return;
        }
        showResults(data.top);
    } catch (e) {
        renderErrorState(aptResults, e.message, runTopPicks);
        aptPager.innerHTML = '';
    }
}

/* ---- Pagination: 9 cards per page, shared by search + top picks ---- */
const aptPager = document.getElementById('aptPager');
const PAGE_SIZE = 9;
let pagedResults = [];
let pagedPage = 1;

function showResults(list) {
    pagedResults = list || [];
    pagedPage = 1;
    renderPage();
}

function renderPage() {
    const total = pagedResults.length;
    const pages = Math.ceil(total / PAGE_SIZE);
    pagedPage = Math.min(Math.max(1, pagedPage), Math.max(1, pages));
    const start = (pagedPage - 1) * PAGE_SIZE;
    aptResults.innerHTML = pagedResults.slice(start, start + PAGE_SIZE).map(renderResult).join('');
    renderPager(pages);
}

function renderPager(pages) {
    if (!aptPager) return;
    if (pages <= 1) { aptPager.innerHTML = ''; return; }
    const p = pagedPage;
    const btn = (label, page, { active = false, disabled = false } = {}) => {
        const style = active
            ? 'bg-primary text-on-primary'
            : 'bg-surface-container text-on-surface hover:bg-surface-container-high';
        const off = disabled ? 'opacity-40 pointer-events-none' : '';
        return `<button data-page="${page}" class="pager-btn ${style} ${off} min-w-[2.25rem] h-9 px-3 rounded-full font-label-md text-label-md transition-colors">${label}</button>`;
    };
    // Show 1, last, and a small window around the current page (with ellipses).
    const shown = [...new Set([1, 2, pages - 1, pages, p - 1, p, p + 1])]
        .filter(n => n >= 1 && n <= pages).sort((a, b) => a - b);
    let html = btn('‹ Prev', p - 1, { disabled: p <= 1 });
    let prev = 0;
    for (const n of shown) {
        if (n - prev > 1) html += `<span class="px-1 text-on-surface-variant">…</span>`;
        html += btn(String(n), n, { active: n === p });
        prev = n;
    }
    html += btn('Next ›', p + 1, { disabled: p >= pages });
    aptPager.innerHTML = html;
    [...aptPager.querySelectorAll('.pager-btn')].forEach(b => b.onclick = () => {
        pagedPage = parseInt(b.dataset.page, 10);
        renderPage();
        document.getElementById('apartments').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
}

function renderAptCount(n, query) {
    const bits = [];
    if (provSel.value) bits.push('province=' + provSel.value);
    if (citySel.value) bits.push('city=' + citySel.value);
    const suffix = bits.length ? ` · filtered by <b>${escapeHtml(bits.join(' · '))}</b>` : '';
    const qBit = query ? ` for "<b>${escapeHtml(query)}</b>"` : ' (top-ranked)';
    aptCount.innerHTML = `Showing <b>${n}</b> apartments${qBit}${suffix}`;
}

document.getElementById('aptForm').addEventListener('submit', (e) => { e.preventDefault(); runApartments(); });
document.getElementById('topBtn').addEventListener('click', () => { aptQ.value = ''; runTopPicks(); });

// Apartment-box autocomplete dropdown
attachAutocompleteDropdown(aptQ, aptAcList, (word) => { aptQ.value = word; runApartmentSearch(word); });

/* =========================================================================
 * Autocomplete feature (own box)
 * ====================================================================== */
const acInput   = document.getElementById('acInput');
const acResults = document.getElementById('acResults');
let acTimer;
acInput.addEventListener('input', () => {
    clearTimeout(acTimer);
    acTimer = setTimeout(() => runAutocomplete(acInput.value.trim()), 100);
});
document.getElementById('acForm').addEventListener('submit', (e) => { e.preventDefault(); runAutocomplete(acInput.value.trim()); });

async function runAutocomplete(term) {
    if (term.length < 1) { acResults.innerHTML = `<li class="text-on-surface-variant text-body-sm">Suggestions appear as you type.</li>`; return; }
    try {
        const data = await api.get('/api/autocomplete?q=' + encodeURIComponent(term) + '&limit=15');
        const words = data.suggestions || [];
        acResults.innerHTML = words.length
            ? words.map(w => `<li class="flex items-center gap-2 py-1.5 px-3 rounded-lg hover:bg-surface-container cursor-pointer ac-suggest"><span>${escapeHtml(w)}</span></li>`).join('')
            : `<li class="text-on-surface-variant text-body-sm">No completions yet — the live crawl is still warming up.</li>`;
        [...acResults.querySelectorAll('.ac-suggest')].forEach(li => li.onclick = () => {
            const w = li.querySelector('span:last-child').textContent;
            aptQ.value = w; runApartmentSearch(w);
            document.getElementById('apartments').scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    } catch (e) {
        acResults.innerHTML = `<li class="text-error text-body-sm">${escapeHtml(e.message)}</li>`;
    }
}

/* =========================================================================
 * Spell check feature (own box)
 * ====================================================================== */
const spellInput = document.getElementById('spellInput');
const spellOut   = document.getElementById('spellOut');
document.getElementById('spellForm').addEventListener('submit', (e) => { e.preventDefault(); runSpellcheck(spellInput.value.trim()); });

async function runSpellcheck(word) {
    if (!word) { spellOut.innerHTML = ''; return; }
    try {
        const data = await api.get('/api/spellcheck?word=' + encodeURIComponent(word));
        if (data.valid) {
            spellOut.innerHTML = `<div class="validate-out valid">✅ "<b>${escapeHtml(data.word)}</b>" is spelled correctly.</div>`;
            return;
        }
        const s = data.suggestions || [];
        if (!s.length) {
            spellOut.innerHTML = `<div class="validate-out invalid">❌ No close matches for "<b>${escapeHtml(data.word)}</b>".</div>`;
            return;
        }
        const list = s.map(x => `<li><a href="#" class="spell-pick" data-w="${escapeHtml(x)}">${escapeHtml(x)}</a></li>`).join('');
        spellOut.innerHTML = `<div class="did-you-mean">Did you mean <b>${escapeHtml(data.corrected)}</b>?</div>
            <div class="rounded-2xl bg-surface-container-low p-4 mt-3">
                <h3 class="font-label-md text-label-md text-on-surface mb-1">Top suggestions</h3>
                <p class="text-body-sm text-on-surface-variant mb-2">Closest spellings first, then the most common.</p>
                <ol class="list-decimal pl-5 space-y-1 text-body-md">${list}</ol>
            </div>`;
        [...spellOut.querySelectorAll('.spell-pick')].forEach(a => a.onclick = (ev) => {
            ev.preventDefault();
            const w = a.dataset.w;
            aptQ.value = w; runApartmentSearch(w);
            document.getElementById('apartments').scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    } catch (e) {
        spellOut.innerHTML = `<div class="validate-out invalid">${escapeHtml(e.message)}</div>`;
    }
}

/* =========================================================================
 * Validate feature (own box)
 * ====================================================================== */
const valInput = document.getElementById('valInput');
const valType  = document.getElementById('valType');
const valOut   = document.getElementById('valOut');
document.getElementById('valForm').addEventListener('submit', (e) => { e.preventDefault(); runValidate(valType.value, valInput.value); });

async function runValidate(type, value) {
    if (!value.trim()) { valOut.innerHTML = ''; return; }
    try {
        const data = await api.post('/api/validate', { type, value });
        valOut.innerHTML = `<div class="validate-out ${data.valid ? 'valid' : 'invalid'}">${data.valid ? '✅' : '❌'} <span><b>${escapeHtml(data.value)}</b> — ${escapeHtml(data.message)}</span></div>`;
    } catch (e) {
        valOut.innerHTML = `<div class="validate-out invalid">${escapeHtml(e.message)}</div>`;
    }
}

/* =========================================================================
 * Master search — fans out to every feature at once
 * ====================================================================== */
const masterQ  = document.getElementById('masterQ');
const masterAc = document.getElementById('masterAc');

document.getElementById('masterForm').addEventListener('submit', (e) => {
    e.preventDefault();
    masterAc.style.display = 'none';
    runEverything(masterQ.value.trim());
});

[...document.querySelectorAll('.quick-chip')].forEach(chip => chip.onclick = () => {
    masterQ.value = chip.dataset.quick;
    runEverything(chip.dataset.quick);
});

function runEverything(query) {
    if (!query) { runTopPicks(); return; }

    // 1) Apartments — mirror into its own box and search
    aptQ.value = query;
    runApartmentSearch(query);

    // 2) Autocomplete — use the query as the prefix
    acInput.value = query;
    runAutocomplete(query);

    // 3) Spell check — check the first (or only) word of the query
    const firstWord = query.split(/\s+/)[0];
    spellInput.value = firstWord;
    runSpellcheck(firstWord);

    // 4) Validate — auto-detect if the query looks like a postal/email/phone/SIN
    const guessed = guessValidatorType(query);
    if (guessed) {
        valType.value = guessed;
        valInput.value = query;
        runValidate(guessed, query);
    }

    document.getElementById('apartments').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// Cheap heuristic so the validator lights up on obviously-structured input.
function guessValidatorType(v) {
    const s = v.trim();
    if (/@/.test(s)) return 'email';
    if (/^[A-Za-z]\d[A-Za-z]\s?\d[A-Za-z]\d$/.test(s)) return 'postal';
    if (/^[\d\s\-().+]{7,}$/.test(s)) return /^\d[\s-]?\d{2}[\s-]?\d{3}[\s-]?\d{3}$/.test(s) ? 'sin' : 'phone';
    return null;
}

// Master-box autocomplete dropdown
attachAutocompleteDropdown(masterQ, masterAc, (word) => { masterQ.value = word; runEverything(word); });

/* =========================================================================
 * Shared autocomplete-dropdown helper
 * ====================================================================== */
function attachAutocompleteDropdown(input, dropdown, onPick) {
    let timer;
    // Submitting the search or pressing Escape should close the suggestions,
    // otherwise they stay on top of the results they were meant to help with.
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' || e.key === 'Enter') dropdown.style.display = 'none';
    });
    if (input.form) input.form.addEventListener('submit', () => { dropdown.style.display = 'none'; });
    input.addEventListener('input', () => {
        clearTimeout(timer);
        timer = setTimeout(async () => {
            const term = input.value.trim();
            if (term.length < 2) { dropdown.style.display = 'none'; return; }
            try {
                const data = await fetch('/api/autocomplete?q=' + encodeURIComponent(term)).then(r => r.json());
                const words = data.suggestions || [];
                if (!words.length) { dropdown.style.display = 'none'; return; }
                dropdown.innerHTML = `<div class="py-2">` + words.map(w =>
                    `<div class="ac-item"><span>${escapeHtml(w)}</span></div>`).join('') + `</div>`;
                dropdown.style.display = 'block';
                [...dropdown.querySelectorAll('.ac-item')].forEach(el => el.onclick = () => {
                    dropdown.style.display = 'none';
                    onPick(el.querySelector('span:last-child').textContent);
                });
            } catch (e) { dropdown.style.display = 'none'; }
        }, 120);
    });
    document.addEventListener('click', (e) => {
        if (!dropdown.contains(e.target) && e.target !== input) dropdown.style.display = 'none';
    });
}

/* =========================================================================
 * Search history — Firestore-backed in FULL mode (needs sign-in), in-memory
 * in DEMO mode. Refreshed after every search so it updates instantly.
 * ====================================================================== */
const historyOut = document.getElementById('historyOut');

function fmtTime(ts) {
    const d = new Date(Number(ts));
    if (!ts || isNaN(d.getTime())) return '';
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' ' +
           d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

async function loadHistory() {
    if (!historyOut) return;
    try {
        const data = await api.get('/api/history');
        const terms  = Object.entries(data.frequencies || {}).sort((a, b) => b[1] - a[1]);
        const recent = data.recent || [];

        if (!terms.length && !recent.length) {
            historyOut.innerHTML = `<p class="text-on-surface-variant text-body-sm">No searches yet — run a search above and it will show up here.</p>`;
            return;
        }

        const freqRows = terms.map(([term, count]) => `
            <div class="flex items-center justify-between gap-3 py-1.5 border-b border-outline-variant/30 last:border-0">
                <span class="text-on-surface truncate">${escapeHtml(term)}</span>
                <span class="shrink-0 bg-secondary-container text-on-secondary-container font-label-sm text-label-sm px-2.5 py-0.5 rounded-full">${count}×</span>
            </div>`).join('') || `<p class="text-on-surface-variant text-body-sm">Nothing yet.</p>`;

        const recentRows = recent.map(r => `
            <div class="flex items-center justify-between gap-3 py-1.5 border-b border-outline-variant/30 last:border-0">
                <span class="text-on-surface truncate">${escapeHtml(r.q || '')}</span>
                <span class="shrink-0 text-on-surface-variant text-body-sm">${fmtTime(r.timestamp)}</span>
            </div>`).join('') || `<p class="text-on-surface-variant text-body-sm">Nothing yet.</p>`;

        historyOut.innerHTML = `
            <div class="grid grid-cols-1 md:grid-cols-2 gap-x-10 gap-y-6">
                <div>
                    <h3 class="font-label-md text-label-md text-on-surface mb-2">Most-searched terms <span class="text-on-surface-variant">(${terms.length})</span></h3>
                    <div class="max-h-[22rem] overflow-y-auto pr-2">${freqRows}</div>
                </div>
                <div>
                    <h3 class="font-label-md text-label-md text-on-surface mb-2">Recent searches <span class="text-on-surface-variant">(${recent.length})</span></h3>
                    <div class="max-h-[22rem] overflow-y-auto pr-2">${recentRows}</div>
                </div>
            </div>`;
    } catch (e) {
        // FULL mode returns 401 ("Unauthorized") until the user signs in.
        historyOut.innerHTML = e.status === 401
            ? `<p class="text-on-surface-variant text-body-sm">Sign in with Google (top right) to see your search history.</p>`
            : `<p class="text-on-surface-variant text-body-sm">Couldn't load history right now.</p>`;
    }
}

/* =========================================================================
 * Boot
 * ====================================================================== */
(async () => {
    await loadFacets();
    runTopPicks();   // show something before the first search
    loadHistory();
})();
