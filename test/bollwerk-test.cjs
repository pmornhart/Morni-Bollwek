/**
 * Echter Test von Morni Bollwerk mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node bollwerk-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Carl), Bert (englisch), Carl (kommt als Verstärkung dazu), Dora
 * (schaut zu). Sprache und Party kommen vom Test-Netzwerk (test/fake-network), das auch Statistik, Zustand und
 * „zur Lobby“ in calls.log mitschreibt – dazu „spielt gerade“ und freie Plätze nach einem Verbindungsabbruch (im Test
 * 20 Sekunden).
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25790', RCON = '127.0.0.1:25890', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const HALL = { x: 0.5, y: 101, z: 8.5 };
const NO_MOVE = process.env.NO_MOVE === '1'; // Paper 26.3 über ViaBackwards: Test-Spieler ohne Bewegung

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const playerFile = (name) =>
  path.join(SERVER, 'plugins/MorniBollwerk/spieler', `${offlineUuid(name)}.yml`);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));

async function waitFor(cond, ms = 5000, step = 100) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 1500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({
    host: HOST,
    port: Number(PORT),
    username: name,
    version: VERSION,
    auth: 'offline',
    physicsEnabled: !NO_MOVE,
  });
  if (NO_MOVE) {
    // Über ViaBackwards wirft Paper 26.3 Test-Spieler wegen ihrer Bewegungs-Pakete hinaus – also keine schicken.
    const write = bot._client.write.bind(bot._client);
    const MOVES = new Set(['position', 'position_look', 'look', 'flying', 'player_input']);
    bot._client.write = (name, params) => (MOVES.has(name) ? undefined : write(name, params));
  }
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.kickReason = null;
  bot.pushes = [];
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  // Aktionsleiste kommt als eigenes Paket; Geschwindigkeit (Sprung, Rückstoß) wendet mineflayer bei sich nicht an –
  // darum die rohen Pakete mitschreiben.
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('entity_velocity', (p) => {
    if (bot.entity && p.entityId === bot.entity.id) bot.pushes.push(p.velocity);
  });
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 20000);
  });
  await sleep(1500);
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHall = (bot) => near(bot.entity.position, HALL, 4) && bot.game.gameMode === 'adventure';
const onMap = (bot, originX) => Math.abs(bot.entity.position.x - originX) < 70;
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const clear = (...bots) =>
  bots.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
  });
const inWorld = (c) => `execute in minecraft:bollwerk run ${c}`;
const TAL = 1000;
const KLO = 2000;
const RETURN_S = 20; // rueckkehr-sekunden im Test
const holds = (mark, n) => calls().slice(mark).filter((c) => c.t === 'hold-slot' && c.name === n).map((c) => c.seconds).join(',');
const lastPlaying = (n) => calls().filter((c) => c.t === 'playing' && c.name === n).slice(-1)[0]?.playing;

/** /bollwerk status als Werte: {phase, welle, geschafft, herz, max, gegner, …, players: {Anna: {state, gold, kills, revives}}} */
async function status() {
  const lines = (await cmd('bollwerk status'))[0].split('\n');
  const games = {};
  for (const line of lines) {
    if (!line.startsWith('karte=')) continue;
    const s = { bau: {}, players: {} };
    for (const part of line.split(' ')) {
      const [k, v] = part.split('=');
      if (k === 'herz') {
        const [h, m] = v.split('/');
        s.herz = Number(h);
        s.max = Number(m);
      } else if (/^bau\d+$/.test(k)) {
        const [type, level, hp, uses] = v.split(':');
        s.bau[k.slice(3)] = { type, level: Number(level), hp: Number(hp), uses: Number(uses) };
      } else if (/^[A-Z]/.test(k)) {
        const [state, gold, kills, revives] = v.split(':');
        s.players[k] = { state, gold: Number(gold), kills: Number(kills), revives: Number(revives) };
      } else {
        s[k] = /^-?\d+$/.test(v) ? Number(v) : v;
      }
    }
    games[s.karte] = s;
  }
  return games;
}

async function openWithItem(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  bot.activateItem();
  return opened;
}

async function closeWindow(bot) {
  if (bot.currentWindow) bot.closeWindow(bot.currentWindow);
  await sleep(200);
}

/** Bauplan in die Hand und den Bauplatz-Block anklicken. */
async function place(bot, planName, x, y, z) {
  const i = bot.inventory.items().find((it) => plain(it.customName).toLowerCase().includes(planName.toLowerCase()));
  if (!i) return false;
  await bot.equip(i, 'hand');
  await sleep(250);
  const block = bot.blockAt(new (require('vec3'))(x, y, z));
  if (!block) return false;
  await bot.lookAt(block.position.offset(0.5, 1, 0.5), true);
  await bot.activateBlock(block);
  await sleep(600);
  return true;
}

async function killAll() {
  await cmd(inWorld('kill @e[tag=bollwerk_enemy]'));
}

/** Welle durch Abräumen (Konsole) beenden; liefert true, wenn „geschafft“. */
async function clearWave(n, karte = 'talsperre') {
  for (let i = 0; i < 80; i++) {
    const s = (await status())[karte];
    if (!s) return false;
    if (s.geschafft >= n && s.phase === 'PREPARE') return true;
    if (s.phase === 'WAVE') await killAll();
    await sleep(500);
  }
  return false;
}

async function main() {
  // ------------------------------------------------------------------ Wachstube
  // Die Tafeln sind nicht dauerhaft gespeichert: Ohne Ticket entlud der Server die Wachstube kurz nach dem Start,
  // solange niemand da war, und die Tafeln blieben weg. Erst warten, dann (noch ohne Spieler) zählen.
  await sleep(8000);
  const holos = String(await cmd(inWorld('execute positioned 0 101 0 if entity @e[type=text_display,distance=..30]')));
  check('Tafeln der Wachstube stehen, obwohl nach dem Start niemand da war', /: 6\b/.test(holos), holos.trim());
  let anna = await join('Anna');
  let bert = await join('Bert');
  check('Ankunft in der Wachstube', inHall(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Kompass, Farbstoff, Gold, Bett',
    slot(anna, 0)?.name === 'compass' && slot(anna, 1)?.name === 'gray_dye' && slot(anna, 4)?.name === 'gold_ingot' &&
      slot(anna, 8)?.name === 'red_bed', [0, 1, 4, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Karte wählen“', plain(slot(anna, 0)?.customName).includes('Karte wählen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Vote for a map“', plain(slot(bert, 0)?.customName).includes('Vote for a map'),
    plain(slot(bert, 0)?.customName));
  check('Begrüßung + Erklärung im Chat', sees(anna, /Willkommen in der Wachstube/) && sees(anna, /Herz/));
  check('Leiste rechts (Scoreboard) vorhanden', await waitFor(() => Object.keys(anna.scoreboards ?? {}).length > 0, 3000));
  check('Netzwerk: Zustand „waiting“', calls().some((c) => c.t === 'state' && c.state === 'waiting'));

  // ------------------------------------------------------------------ Abstimmung
  let win = await openWithItem(anna, 0);
  check('Kompass öffnet „Karte wählen“', win && plain(win.title).includes('Karte wählen'), win && plain(win.title));
  check('Beide Karten im Menü', plain(win?.slots[11]?.customName).includes('Talsperre') &&
    plain(win?.slots[15]?.customName).includes('Klosterhof'), `${plain(win?.slots[11]?.customName)} / ${plain(win?.slots[15]?.customName)}`);
  await anna.clickWindow(11, 0, 0);
  await sleep(600);
  check('Stimme für Talsperre', sees(anna, /Deine Stimme: Talsperre Morni-Dorf/));
  bert.chat('/karte talsperre');
  await sleep(600);
  check('Bert stimmt per Befehl (englisch)', sees(bert, /Your vote: Morni Village Barrier/));

  // ------------------------------------------------------------------ Bereit → Start
  clear(anna, bert);
  check('Kein Countdown, solange nur 2 von 3 nötigen da sind', !anna.bars.some((b) => /Nächste Runde/.test(b)));
  anna.setQuickBarSlot(1);
  await sleep(250);
  anna.activateItem();
  check('Farbstoff: Anna ist bereit → Countdown startet', await waitFor(() => sees(anna, /Du bist bereit/) &&
    anna.bars.some((b) => /Nächste Runde in \d+ s/.test(b)), 3000), anna.bars.slice(-1)[0]);
  bert.chat('/bereit');
  check('Bereit gemeldet', await waitFor(() => sees(anna, /Du bist bereit/) && sees(bert, /You're ready/), 3000),
    `${anna.chats.slice(-2).join(' | ')} / ${bert.chats.slice(-2).join(' | ')}`);
  check('Countdown in der Aktionsleiste', await waitFor(() => anna.bars.some((b) => /Nächste Runde in \d s · Karte: Talsperre/.test(b)), 4000),
    anna.bars.slice(-1)[0]);
  // Beim ersten Start lädt der Server die Chunks der Karte; auf einem ausgelasteten Rechner dauert das deutlich länger.
  const startedOk = await waitFor(() => onMap(anna, TAL) && onMap(bert, TAL), 45000);
  check('Runde startet: beide auf der Talsperre', startedOk, JSON.stringify(anna.entity.position));
  check('Start-Nachricht', sees(anna, /Auf geht's! Karte: Talsperre Morni-Dorf – 2 Verteidiger/));
  check('Erklärung zum Herz (englisch)', sees(bert, /Hold the heart: Village Well/));
  await sleep(800);
  check('Ausrüstung: Steinschwert, Bogen, Smaragd (Laden), Farbstoff (Welle starten)',
    slot(anna, 0)?.name === 'stone_sword' && slot(anna, 1)?.name === 'bow' && slot(anna, 8)?.name === 'emerald' &&
      slot(anna, 7)?.name === 'lime_dye', [0, 1, 7, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Netzwerk: Zustand „running“', await waitFor(() => calls().some((c) => c.t === 'state' && c.state === 'running'), 3000));
  check('Netzwerk: Anna und Bert „spielen gerade“', lastPlaying('Anna') === true && lastPlaying('Bert') === true);
  let s = (await status()).talsperre;
  check('Aufbauphase, Herz 300/300, Startgeld 120', s?.phase === 'PREPARE' && s.herz === 300 && s.players.Anna?.gold === 120,
    JSON.stringify(s));
  const shopkeeper = Object.values(anna.entities).some((e) => e.name === 'villager');
  check('Krämerin steht am Dorfplatz', shopkeeper);

  // ------------------------------------------------------------------ Laden + Bauen
  win = await openWithItem(anna, 8);
  check('Smaragd öffnet den Krämerladen mit Kontostand', win && /Krämerladen – 120 Gulden/.test(plain(win.title)), win && plain(win.title));
  const opened2 = new Promise((r) => { anna.once('windowOpen', r); setTimeout(() => r(null), 3000); });
  await anna.clickWindow(30, 0, 0); // Bauplan: Pfeilturm (60)
  win = await opened2;
  check('Bauplan Pfeilturm gekauft (−60)', sees(anna, /Gekauft: Bauplan: Pfeilturm/) && win && /60 Gulden/.test(plain(win.title)),
    win && plain(win.title));
  await anna.clickWindow(10, 0, 0); // Eisenschwert (35)
  await sleep(700);
  await closeWindow(anna);
  check('Eisenschwert ersetzt das Steinschwert', anna.inventory.items().some((i) => i.name === 'iron_sword') &&
    !anna.inventory.items().some((i) => i.name === 'stone_sword'));
  s = (await status()).talsperre;
  check('Kontostand 25 nach zwei Käufen', s.players.Anna.gold === 25, String(s.players.Anna.gold));
  // zu teuer
  win = await openWithItem(anna, 8);
  await anna.clickWindow(21, 0, 0); // Diamantrüstung (150)
  await sleep(600);
  await closeWindow(anna);
  check('Zu teuer: Hinweis, nichts abgezogen', sees(anna, /fehlen dir noch 125 Gulden/) && (await status()).talsperre.players.Anna.gold === 25);

  // Pfeilturm auf Platz 8 (4|-14) neben dem Nordweg
  await cmd(inWorld(`tp Anna ${TAL + 4.5} 101 -10.5 180 30`));
  await sleep(1200);
  const placedT = await place(anna, 'Pfeilturm', TAL + 4, 100, -14);
  await sleep(500);
  s = (await status()).talsperre;
  check('Pfeilturm gebaut (Bauplatz 8)', placedT && s.bau['8']?.type === 'pfeilturm', JSON.stringify(s.bau));
  const towerBlock = (await cmd(inWorld(`execute if block ${TAL + 3} 101 -15 spruce_log`)))[0];
  check('Turm steht als Blöcke (Holzpfosten)', /passed/i.test(towerBlock), towerBlock);
  check('Alle sehen die Bau-Meldung (englisch)', await waitFor(() => sees(bert, /Anna built: Arrow tower/), 2000),
    bert.chats.slice(-3).join(' | '));
  check('Bauplan verbraucht', !anna.inventory.items().some((i) => plain(i.customName).includes('Bauplan')),
    anna.inventory.items().map((i) => `${i.name}×${i.count}`).join(','));

  // Bert: Palisade auf dem Nordweg (Platz 1 bei 0|-16)
  win = await openWithItem(bert, 8);
  check('Englischer Laden: „Shop – 120 guilders“', win && /Shop – 120 guilders/.test(plain(win.title)), win && plain(win.title));
  await bert.clickWindow(28, 0, 0); // Palisade (30)
  await sleep(700);
  await closeWindow(bert);
  await cmd(inWorld(`tp Bert ${TAL + 0.5} 101 -12.5 180 40`));
  await sleep(1200);
  // erst falscher Platz: Seitenplatz 7 (−4|−14) → Hinweis
  clear(bert);
  const placedW = await place(bert, 'palisade', TAL - 4, 100, -14);
  check('Palisade passt nicht auf einen Seitenplatz (Hinweis)', await waitFor(() => sees(bert, /doesn't fit here/), 2000),
    `${placedW} ${bert.chats.slice(-2).join(' | ')} ${bert.inventory.items().map((i) => i.name).join(',')}`);
  await place(bert, 'palisade', TAL + 0, 100, -16);
  s = (await status()).talsperre;
  check('Palisade quer über den Nordweg gebaut', s.bau['1']?.type === 'palisade' && s.bau['1'].hp === 120, JSON.stringify(s.bau));
  const pal = (await cmd(inWorld(`execute if block ${TAL - 1} 101 -16 spruce_log`)))[0];
  check('Palisade aus Holzstämmen', /passed/i.test(pal), pal);
  // Bau verbessern: Rechtsklick auf den Turm
  await cmd(`bollwerk status`);

  // ------------------------------------------------------------------ Welle 1
  clear(anna, bert);
  await cmd(inWorld(`tp Anna ${TAL + 0.5} 101 6.5 180 0`));
  await cmd(inWorld(`tp Bert ${TAL + 2.5} 101 6.5 180 0`));
  await sleep(600);
  anna.setQuickBarSlot(7);
  await sleep(200);
  anna.activateItem();
  await sleep(300);
  check('„Welle starten“: Anna ist fertig (1/2)', await waitFor(() => sees(bert, /Anna is done \(1\/2\)/), 2000),
    bert.chats.slice(-3).join(' | '));
  bert.setQuickBarSlot(7);
  await sleep(200);
  bert.activateItem();
  check('Alle fertig → Welle 1 kommt', await waitFor(() => sawTitle(anna, /Welle 1/) && sawTitle(bert, /Wave 1/), 6000),
    anna.titles.join(' | '));
  s = (await status()).talsperre;
  check('Welle 1: 10 Gegner bei 2 Leuten (Budget 6,5 × 1,6)', s.phase === 'WAVE' && s.gegner + s.spawn === 10, JSON.stringify(s));
  check('„Welle starten“-Knopf weg, Laden zu', slot(anna, 7) == null);
  win = await openWithItem(anna, 8);
  check('Laden während der Welle geschlossen', !win && sees(anna, /nur zwischen den Wellen/));
  await sleep(2000);
  const tagged = (await cmd(inWorld('execute if entity @e[tag=bollwerk_pluenderer]')))[0];
  check('Plünderer erscheinen (markiert)', /passed|count/i.test(tagged) || /\d+/.test(tagged), tagged);

  // Wer direkt hinter der Palisade steht, wird nicht umlaufen: ein Plünderer vom Nordweg (vor der Palisade) schlägt
  // auf die Palisade ein, statt seitlich vorbei auf Bert loszugehen. Beide unverwundbar, damit nur der Weg zählt.
  const bertAt = bert.entity.position.clone();
  let probe = '';
  for (let i = 0; i < 20 && !/Applied|Tag/i.test(probe); i++) {
    probe = (await cmd(inWorld(`execute positioned ${TAL - 10.5} 101 -46.5 run tag @e[tag=bollwerk_pluenderer,sort=nearest,limit=1,distance=..25] add mm_probe`)))[0];
    if (!/Applied|Tag/i.test(probe)) await sleep(500);
  }
  await cmd(inWorld(`effect give @e[tag=mm_probe] resistance 30 4 true`), inWorld(`effect give Bert resistance 30 4 true`),
    inWorld(`tp Bert ${TAL + 0.5} 101 -13.5 180 0`), inWorld(`tp @e[tag=mm_probe] ${TAL + 0.5} 101 -18.5`));
  let passedWall = false;
  for (let i = 0; i < 16 && !passedWall; i++) {
    await sleep(500);
    passedWall = /passed/i.test((await cmd(inWorld(`execute if entity @e[tag=mm_probe,x=${TAL - 6},y=95,z=-15.5,dx=12,dy=15,dz=5]`)))[0]);
  }
  s = (await status()).talsperre;
  check('Gegner laufen nicht um die Palisade herum, wenn jemand dahinter steht', /Tag|Applied/i.test(probe) && !passedWall &&
    s.bau['1'] && s.bau['1'].hp < 120, `${probe.trim()} · vorbei ${passedWall} · ${JSON.stringify(s.bau['1'])}`);
  await cmd(inWorld('effect clear @e[tag=mm_probe]'), inWorld('tag @e[tag=mm_probe] remove mm_probe'), inWorld('effect clear Bert'),
    inWorld(`tp Bert ${bertAt.x} ${bertAt.y} ${bertAt.z}`));

  // Gegner laufen die Wege entlang; Turm schießt, Palisade wird beschädigt
  const towerShot = await waitFor(async () => (await status()).talsperre.turm_schuesse > 0, 45000, 1000);
  s = (await status()).talsperre;
  check('Pfeilturm schießt auf Gegner', towerShot, `Schüsse ${s.turm_schuesse}, Turm-Siege ${s.turm_kills}`);
  const palHit = await waitFor(async () => {
    const b = (await status()).talsperre.bau['1'];
    return !b || b.hp < 120;
  }, 30000, 1000);
  s = (await status()).talsperre;
  check('Gegner schlagen auf die Palisade ein', palHit, JSON.stringify(s.bau['1']));

  // Anna besiegt einen Gegner selbst
  const before = (await status()).talsperre.players.Anna;
  await cmd(inWorld(`tp @e[tag=bollwerk_enemy,limit=1,sort=random] ${TAL + 0.5} 101 4.5`));
  await sleep(500);
  anna.setQuickBarSlot(anna.inventory.items().find((i) => i.name === 'iron_sword').slot - 36);
  let won = false;
  for (let i = 0; i < 30 && !won; i++) {
    const target = anna.nearestEntity((e) => ['zombie', 'pillager', 'vindicator'].includes(e.name) &&
      e.position.distanceTo(anna.entity.position) < 5);
    if (target) {
      await anna.lookAt(target.position.offset(0, 1, 0), true);
      anna.attack(target);
    } else {
      await cmd(inWorld(`tp @e[tag=bollwerk_enemy,limit=1,sort=random] ${TAL + 0.5} 101 4.5`));
    }
    await sleep(600);
    won = (await status()).talsperre.players.Anna.kills > before.kills;
  }
  const after = (await status()).talsperre.players.Anna;
  check('Anna besiegt einen Gegner: Kill + Gulden', won && after.gold > before.gold, `${JSON.stringify(before)} → ${JSON.stringify(after)}`);
  check('Aktionsleiste „+n Gulden für Plünderer“', anna.bars.some((b) => /Gulden für Plünderer/.test(b)), anna.bars.slice(-1)[0]);

  // Rest abräumen → geschafft
  const bertGold = (await status()).talsperre.players.Bert.gold;
  check('Welle 1 geschafft (Rest per Konsole abgeräumt)', await clearWave(1));
  check('Titel „Welle 1 geschafft!“ / „Wave 1 survived!“', await waitFor(() => sawTitle(anna, /Welle 1 geschafft/) && sawTitle(bert, /Wave 1 survived/), 3000));
  s = (await status()).talsperre;
  check('Wellen-Bonus (+20) und Anteil an Turm-/Konsolen-Siegen', s.players.Bert.gold >= bertGold + 20, `${bertGold} → ${s.players.Bert.gold}`);
  check('„Welle starten“ wieder da', await waitFor(() => slot(anna, 7)?.name === 'lime_dye', 2000));

  // ------------------------------------------------------------------ Bau verbessern (Rechtsklick auf den Turm)
  await cmd(inWorld(`tp Anna ${TAL + 4.5} 101 -10.5 180 30`));
  await sleep(1000);
  anna.setQuickBarSlot(2);
  await sleep(200);
  const menuP = new Promise((r) => { anna.once('windowOpen', r); setTimeout(() => r(null), 3000); });
  await anna.activateBlock(anna.blockAt(new (require('vec3'))(TAL + 4, 104, -14)));
  win = await menuP;
  check('Rechtsklick auf den Turm öffnet sein Menü', win && /Pfeilturm – Stufe 1/.test(plain(win.title)), win && plain(win.title));
  const goldNow = (await status()).talsperre.players.Anna.gold;
  if (win && goldNow >= 55) {
    await anna.clickWindow(13, 0, 0);
    await sleep(700);
    s = (await status()).talsperre;
    check('Turm auf Stufe 2 verbessert (−55)', s.bau['8'].level === 2 && s.players.Anna.gold === goldNow - 55, JSON.stringify(s.bau['8']));
    const stone = (await cmd(inWorld(`execute if block ${TAL + 3} 101 -15 stone_bricks`)))[0];
    check('Stufe 2 aus Stein', /passed/i.test(stone), stone);
  } else {
    await closeWindow(anna);
    check('Turm verbessern (genug Gulden?)', false, `nur ${goldNow} Gulden`);
  }
  await closeWindow(anna);

  // ------------------------------------------------------------------ Am Boden + retten
  clear(anna, bert);
  await cmd(inWorld(`tp Bert ${TAL + 0.5} 101 6.5`));
  await cmd(inWorld(`tp Anna ${TAL + 1.5} 101 6.5`));
  await sleep(800);
  await cmd('damage Bert 100');
  await sleep(500);
  s = (await status()).talsperre;
  check('Tödlicher Treffer: Bert liegt am Boden (nicht tot)', s.players.Bert.state === 'DOWNED' && bert.health > 0, `${s.players.Bert.state}, ${bert.health}`);
  check('Alle erfahren es', sees(anna, /Bert ist am Boden/) && sawTitle(bert, /You're down/));
  anna.setControlState('sneak', true);
  const revived = await waitFor(async () => (await status()).talsperre.players.Bert.state === 'ALIVE', 6000, 300);
  anna.setControlState('sneak', false);
  s = (await status()).talsperre;
  check('Anna rettet Bert durch Ducken', revived && s.players.Anna.revives === 1, JSON.stringify(s.players));
  check('Meldung „wieder auf den Beinen“', await waitFor(() => sees(anna, /Bert ist wieder auf den Beinen/), 2000));

  // ------------------------------------------------------------------ Kesselfalle + Heilbanner (Bert)
  win = await openWithItem(bert, 8);
  let reopened = new Promise((r) => { bert.once('windowOpen', r); setTimeout(() => r(null), 3000); });
  await bert.clickWindow(29, 0, 0); // Kesselfalle (45)
  await reopened;
  await bert.clickWindow(31, 0, 0); // Heilbanner (40)
  await sleep(700);
  await closeWindow(bert);
  await cmd(inWorld(`tp Bert ${TAL + 13.5} 101 0.5 90 40`));
  await sleep(1200);
  await place(bert, 'cauldron', TAL + 16, 100, 0);
  await cmd(inWorld(`tp Bert ${TAL + 5.5} 101 -2.5 180 40`));
  await sleep(1200);
  await place(bert, 'healing banner', TAL + 5, 100, -6);
  s = (await status()).talsperre;
  check('Kesselfalle an der Bresche und Heilbanner am Dorfplatz gebaut',
    s.bau['3']?.type === 'kesselfalle' && s.bau['13']?.type === 'heilbanner', JSON.stringify(s.bau));
  const cauldron = (await cmd(inWorld(`execute if block ${TAL + 16} 102 2 water_cauldron`)))[0];
  check('Kessel steht neben dem Weg (Weg bleibt frei)', /passed/i.test(cauldron) &&
    /passed/i.test((await cmd(inWorld(`execute if block ${TAL + 16} 101 0 air`)))[0]), cauldron);
  await cmd('effect give Bert minecraft:instant_health 1 3');
  await sleep(500);
  await cmd('damage Bert 8');
  await sleep(300);
  const hurt = bert.health;
  const healed = await waitFor(() => bert.health > hurt, 6000, 200);
  check('Heilbanner heilt Bert', healed, `${hurt} → ${bert.health}`);

  // ------------------------------------------------------------------ Wellen 2 und 3
  for (const n of [2, 3]) {
    await cmd('bollwerk welle ' + n + ' talsperre');
    await sleep(1500);
    const sn = (await status()).talsperre;
    check(`Welle ${n} läuft (${sn.gegner + sn.spawn} Gegner)`, sn.phase === 'WAVE' && sn.welle === n, JSON.stringify(sn));
    if (n === 2) {
      const scald = await waitFor(async () => (await status()).talsperre.bau['3']?.uses > 0, 40000, 1000);
      check('Kesselfalle verbrüht Gegner an der Bresche', scald, JSON.stringify((await status()).talsperre.bau['3']));
    }
    if (n === 3) {
      let shields = '';
      await waitFor(async () => /passed/i.test(shields = (await cmd(inWorld('execute if entity @e[tag=bollwerk_schildtraeger]')))[0]), 25000, 1000);
      check('Welle 3 bringt Schildträger', /passed/i.test(shields), shields);
      check('Neue Gegnerart wird vorgestellt', sees(anna, /Neu dabei: Schildträger/));
    }
    check(`Welle ${n} geschafft`, await clearWave(n));
  }

  // ------------------------------------------------------------------ Verstärkung aus der Party + Zuschauen
  const carl = await join('Carl');
  check('Carl (Party mit Anna) kommt als Verstärkung in die Runde', await waitFor(() => onMap(carl, TAL), 6000),
    JSON.stringify(carl.entity.position));
  check('Hinweis an Carl und alle', sees(carl, /Deine Party spielt gerade/) && await waitFor(() => sees(anna, /Carl kommt zur Verstärkung/), 2000));
  s = (await status()).talsperre;
  check('Carl hat Startgeld + Bonus', s.players.Carl && s.players.Carl.gold > 120, JSON.stringify(s.players.Carl));
  const dora = await join('Dora');
  check('Dora landet in der Wachstube', inHall(dora));
  dora.chat('/zuschauen Anna');
  check('Dora schaut zu (Zuschauermodus auf der Karte)', await waitFor(() => dora.game.gameMode === 'spectator' && onMap(dora, TAL), 5000),
    `${dora.game.gameMode} ${JSON.stringify(dora.entity.position)}`);
  check('Hinweis „Du schaust … zu“', sees(dora, /Du schaust Talsperre Morni-Dorf zu/));

  // ------------------------------------------------------------------ Geld zum Testen (Owner/Admin): /bollwerk geld
  s = (await status()).talsperre;
  const goldAnna = s.players.Anna.gold;
  const goldBert = s.players.Bert.gold;
  const [money] = await cmd('bollwerk geld Anna 30');
  s = (await status()).talsperre;
  check('/bollwerk geld Anna 30: +30 Gulden in der laufenden Runde, mit Bestätigung',
    s.players.Anna.gold >= goldAnna + 30 && /Anna bekommt 30 Gulden – jetzt \d+/.test(money.replace(/§./g, '')),
    `${goldAnna} → ${s.players.Anna.gold}: ${money.replace(/§./g, '')}`);
  check('Anna liest „Vom Team zum Testen: +30 Gulden“', await waitFor(() => sees(anna, /Vom Team zum Testen: \+30 Gulden/), 3000),
    anna.chats.slice(-2).join(' | '));
  bert.chat('/bollwerk geld Bert 100');
  await sleep(800);
  s = (await status()).talsperre;
  check('Ohne Verwaltungsrecht (Bert) gibt es kein Geld', s.players.Bert.gold - goldBert < 100, `${goldBert} → ${s.players.Bert.gold}`);

  // ------------------------------------------------------------------ Verbindung weg, rechtzeitig zurück
  let mark = calls().length;
  await cmd('damage Bert 4 minecraft:out_of_world');
  await waitFor(() => bert.health <= 16.5, 3000);
  const bertBefore = { pos: bert.entity.position.clone(), hp: bert.health, gear: [0, 1, 7, 8].map((i) => slot(bert, i)?.name).join(',') };
  clear(anna);
  bert.quit();
  check(`Bert weg: Anna erfährt es (Platz ${RETURN_S} s frei, weiter verteidigen)`, await waitFor(() =>
    sees(anna, new RegExp(`Bert hat die Verbindung verloren – der Platz bleibt ${RETURN_S} Sekunden frei, ihr verteidigt weiter`)), 4000),
  anna.chats.join(' | '));
  s = (await status()).talsperre;
  check('Bert bleibt in der Runde (Platz frei), die Runde läuft weiter', s.players.Bert?.state === 'ALIVE' && s.weg === 1 &&
    s.angehalten === 'false', JSON.stringify({ bert: s.players.Bert, weg: s.weg, angehalten: s.angehalten }));
  check(`Netzwerk: Platz für Bert frei (holdSlot ${RETURN_S})`, holds(mark, 'Bert') === String(RETURN_S), holds(mark, 'Bert'));
  bert = await join('Bert');
  check('Bert ist rechtzeitig zurück: auf der Karte, genau dort, wo er war', onMap(bert, TAL) &&
    bert.entity.position.distanceTo(bertBefore.pos) < 1.5 && bert.game.gameMode === 'adventure',
  `${bertBefore.pos} → ${bert.entity.position} ${bert.game.gameMode}`);
  check('Bert hat seine Ausrüstung und sein Leben', [0, 1, 7, 8].map((i) => slot(bert, i)?.name).join(',') === bertBefore.gear &&
    Math.abs(bert.health - bertBefore.hp) < 1, `${bertBefore.gear} → ${[0, 1, 7, 8].map((i) => slot(bert, i)?.name).join(',')}, ${bertBefore.hp} → ${bert.health}`);
  check('Bert (englisch): „Welcome back“, Anna: „Bert ist wieder da“', await waitFor(() =>
    sees(bert, /Welcome back! You're right where you were/) && sees(anna, /Bert ist wieder da/), 3000), bert.chats.join(' | '));
  check('Netzwerk: Platz wieder zu (0), Bert „spielt“ wieder', holds(mark, 'Bert') === `${RETURN_S},0` && lastPlaying('Bert') === true,
    holds(mark, 'Bert'));

  // ------------------------------------------------------------------ Boss-Welle, Herz nimmt Schaden, Ende
  clear(anna, bert, carl, dora);
  await cmd('bollwerk welle 5 talsperre');
  await sleep(1500);
  check('Boss-Welle 5: „Der Lawinenriese kommt!“', sees(anna, /Der Lawinenriese kommt/) && sees(bert, /The Avalanche Giant is coming/));
  const boss = await waitFor(async () => !/failed|0/i.test((await cmd(inWorld('execute if entity @e[type=ravager,tag=bollwerk_lawinenriese]')))[0]), 40000, 1000);
  check('Der Lawinenriese erscheint (zuletzt)', boss);
  // Spieler aus dem Weg, damit die Gegner zum Herzen laufen
  for (const n of ['Anna', 'Bert', 'Carl']) await cmd(`effect give ${n} minecraft:resistance 600 4 true`);
  for (const n of ['Anna', 'Bert', 'Carl']) await cmd(inWorld(`tp ${n} ${TAL + 14.5} 101 12.5`));
  const t0 = Date.now();
  const hit = await waitFor(async () => (await status()).talsperre.herz < 300, 60000, 1000);
  s = (await status()).talsperre;
  check('Gegner erreichen das Herz und schlagen zu', hit, `Herz ${s.herz}/300 nach ${((Date.now() - t0) / 1000).toFixed(0)} s`);
  check('Warnung „Das Herz wird angegriffen!“', sees(anna, /Das Herz wird angegriffen/));
  const fell = await waitFor(async () => !(await status()).talsperre, 90000, 1000) || sawTitle(anna, /gefallen/);
  if (!sawTitle(anna, /gefallen/)) {
    await cmd('bollwerk herz 0 talsperre');
  }
  check('Herz fällt → „Das Bollwerk ist gefallen“', await waitFor(() => sawTitle(anna, /Das Bollwerk ist gefallen/) && sawTitle(bert, /The bulwark has fallen/), 10000),
    `natürlich gefallen: ${fell}`);
  check('Ergebnis im Chat: 4 Wellen, Platz 1', await waitFor(() => sees(anna, /4 Wellen überstanden – Platz 1/), 3000), anna.chats.slice(-3).join(' | '));
  const stats = calls().filter((c) => c.t === 'stats');
  const annaStats = stats.find((c) => c.name === 'Anna');
  check('Netzwerk-Statistik: bollwerk, games/kills/waves, best_wave (höher ist besser)',
    annaStats && annaStats.mode === 'bollwerk' && annaStats.add.games === 1 && annaStats.add.waves === 4 &&
      annaStats.add.kills >= 1 && annaStats.best.best_wave === 4 && annaStats.lower === false, JSON.stringify(annaStats));
  check('Statistik für alle drei Verteidiger', ['Anna', 'Bert', 'Carl'].every((n) => stats.some((c) => c.name === n)),
    stats.map((c) => c.name).join(','));
  check('Zurück in die Wachstube (alle, auch die Zuschauerin)', await waitFor(() => [anna, bert, carl, dora].every(inHall), 12000),
    [anna, bert, carl, dora].map((b) => `${b.username}:${b.game.gameMode}`).join(' '));
  check('Ausrüstung weg, Wachstuben-Knöpfe da', slot(anna, 0)?.name === 'compass');
  const palGone = (await cmd(inWorld(`execute if block ${TAL - 1} 101 -16 air`)))[0];
  const towerGone = (await cmd(inWorld(`execute if block ${TAL + 3} 101 -15 air`)))[0];
  const pathBack = (await cmd(inWorld(`execute if block ${TAL} 100 -16 chiseled_stone_bricks`)))[0];
  check('Karte aufgeräumt: Palisade und Turm weg, Bauplatz wie vorher', /passed/i.test(palGone) && /passed/i.test(towerGone) &&
    /passed/i.test(pathBack), `${palGone} / ${towerGone} / ${pathBack}`);
  const leftovers = (await cmd(inWorld('execute if entity @e[tag=bollwerk_enemy]')))[0];
  check('Keine Gegner mehr übrig', /failed/i.test(leftovers), leftovers);
  const board = readFile(path.join(SERVER, 'plugins/MorniBollwerk/bestenliste.yml'));
  check('Bestenliste: Talsperre 4 Wellen mit Anna, Bert, Carl', /talsperre:[\s\S]*wellen: 4/.test(board) && /Carl/.test(board), board.trim().slice(0, 200));
  check('Spielerdatei: beste Welle 4', /talsperre: 4/.test(readFile(playerFile('Anna'))) && /rettungen: 1/.test(readFile(playerFile('Anna'))),
    readFile(playerFile('Anna')).trim().replace(/\n/g, ' '));
  const top = (await cmd('top talsperre'))[0];
  check('/top zeigt die Truppe', /4 Wellen/.test(top), top.replace(/\n/g, ' | ').slice(0, 160));
  check('Netzwerk: Zustand wieder „waiting“', await waitFor(() => calls().filter((c) => c.t === 'state').slice(-1)[0]?.state === 'waiting', 4000));
  check('Netzwerk: nach der Runde „spielt“ niemand mehr', ['Anna', 'Bert', 'Carl'].every((n) => lastPlaying(n) === false),
    ['Anna', 'Bert', 'Carl'].map((n) => `${n}:${lastPlaying(n)}`).join(' '));
  check('Ergebnis „Neue Bestleistung“', sees(anna, /Neue Bestleistung für dich: 4 Wellen/));

  // ------------------------------------------------------------------ Klosterhof
  clear(anna, bert);
  carl.quit();
  dora.quit();
  await sleep(1000);
  anna.chat('/karte klosterhof');
  bert.chat('/karte klosterhof');
  anna.chat('/bereit');
  bert.chat('/bereit');
  check('Zweite Runde auf dem Klosterhof', await waitFor(() => onMap(anna, KLO) && onMap(bert, KLO), 15000), JSON.stringify(anna.entity.position));
  check('Englisch: „Hold the heart: Cloister Bell“', sees(bert, /Hold the heart: Cloister Bell/));
  anna.chat('/bereit');
  bert.chat('/bereit');
  check('Welle 1 auf dem Klosterhof', await waitFor(() => sawTitle(anna, /Welle 1/), 8000));
  await sleep(4000);
  const pos1 = (await cmd(inWorld('execute as @e[tag=bollwerk_enemy,limit=1,sort=furthest] at @s run tp @s ~ ~ ~')))[0];
  check('Gegner unterwegs auf dem Klosterhof', /Teleported/i.test(pos1), pos1);

  // ------------------------------------------------------------------ Alle weg: die Runde hält an
  mark = calls().length;
  anna.quit();
  bert.quit();
  await sleep(2500);
  s = (await status()).klosterhof;
  check('Alle weg: die Runde hält an (statt vorbei zu sein)', s && s.angehalten === 'true' && s.weg === 2,
    JSON.stringify(s && { angehalten: s.angehalten, weg: s.weg, phase: s.phase }));
  const frozenAt = (await cmd(inWorld('execute as @e[tag=bollwerk_enemy,limit=1,sort=furthest] at @s run tp @s ~ ~ ~')))[0];
  await sleep(1500);
  const frozenLater = (await cmd(inWorld('execute as @e[tag=bollwerk_enemy,limit=1,sort=furthest] at @s run tp @s ~ ~ ~')))[0];
  check('Angehalten: Gegner eingefroren', frozenAt === frozenLater, `${frozenAt} / ${frozenLater}`);
  anna = await join('Anna');
  bert = await join('Bert');
  s = (await status()).klosterhof;
  check('Beide rechtzeitig zurück: die Runde läuft weiter, beide auf dem Klosterhof', s && s.angehalten === 'false' && s.weg === 0 &&
    onMap(anna, KLO) && onMap(bert, KLO), JSON.stringify(s && { angehalten: s.angehalten, weg: s.weg }));
  // Zu lange weg: wie bisher – in der Welle als Zuschauer zurück
  clear(anna);
  bert.quit();
  check(`Zu lange weg (über ${RETURN_S} s): „Bert ist nicht rechtzeitig zurückgekommen“`, await waitFor(() =>
    sees(anna, /Bert ist nicht rechtzeitig zurückgekommen/), RETURN_S * 1000 + 6000), anna.chats.join(' | '));
  check(`Netzwerk: holdSlot für Bert ${RETURN_S}, 0, ${RETURN_S}, 0`, holds(mark, 'Bert') === `${RETURN_S},0,${RETURN_S},0`,
    holds(mark, 'Bert'));
  bert = await join('Bert');
  await sleep(500);
  s = (await status()).klosterhof;
  check('Später zurück: wie bisher – in der Welle als Zuschauer (in der Aufbauphase auf der Karte), gehört weiter zur Runde',
    s && s.players.Bert && (s.phase === 'WAVE' ? bert.game.gameMode === 'spectator' : onMap(bert, KLO)),
    `${s?.phase} ${bert.game.gameMode} ${JSON.stringify(s?.players?.Bert)}`);
  for (const n of ['Anna', 'Bert']) await cmd(`effect give ${n} minecraft:resistance 600 4 true`);
  for (const n of ['Anna', 'Bert']) await cmd(inWorld(`tp ${n} ${KLO + 10.5} 101 -12.5`));
  const t1 = Date.now();
  const hitK = await waitFor(async () => (await status()).klosterhof?.herz < 300, 70000, 1000);
  s = (await status()).klosterhof;
  check('Gegner finden über Pilgerweg/Brücke/Grat zur Klosterglocke', hitK, `Herz ${s?.herz} nach ${((Date.now() - t1) / 1000).toFixed(0)} s`);
  await cmd('bollwerk herz 0 klosterhof');
  check('Klosterhof-Runde vorbei, 0 Wellen → kein Eintrag in der Bestenliste',
    await waitFor(() => sawTitle(anna, /gefallen/), 5000) && !/klosterhof/.test(readFile(path.join(SERVER, 'plugins/MorniBollwerk/bestenliste.yml'))));
  check('Wieder in der Wachstube', await waitFor(() => inHall(anna) && inHall(bert), 10000));

  // ------------------------------------------------------------------ Lobby-Knopf
  clear(anna);
  mark = calls().length;
  anna.setQuickBarSlot(8);
  await sleep(200);
  anna.activateItem();
  check('Bett: zur Lobby über askToLobby (Anna spielt nicht → sofort)', await waitFor(() =>
    calls().slice(mark).some((c) => c.t === 'ask-to-lobby' && c.name === 'Anna' && c.playing === false), 4000) &&
    !calls().slice(mark).some((c) => c.t === 'to-lobby' && c.name === 'Anna'));
  // Älteres Netzwerk-Plugin (ohne Methoden ab Version 2): wie bisher sofort über toLobby
  fs.appendFileSync(path.join(SERVER, 'plugins/MoinMorni/fake.properties'), 'api=1\n');
  bert.setQuickBarSlot(8);
  await sleep(200);
  bert.activateItem();
  check('Älteres Netzwerk-Plugin: Bett schickt wie bisher sofort in die Lobby (toLobby)', await waitFor(() =>
    calls().slice(mark).some((c) => c.t === 'old-api' && c.method === 'askToLobby') &&
      calls().slice(mark).some((c) => c.t === 'to-lobby' && c.name === 'Bert'), 4000));

  bert.quit();
  await sleep(500);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\n${ok}/${results.length} Prüfungen bestanden`);
  process.exit(ok === results.length ? 0 : 1);
}

main().catch((e) => {
  console.error(e);
  const ok = results.filter((r) => r.ok).length;
  console.log(`\nAbgebrochen – ${ok}/${results.length} Prüfungen bestanden`);
  process.exit(1);
});
