# Morni Bollwerk

Der Verteidigungs-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): **Verteidigt das Dorf gegen Welle um
Welle – und rüstet zwischen den Wellen auf.** Zusammen mit 1 bis 6 Leuten (oder allein). Ein Paper-Plugin für
Paper 26.x (Java 25). Es läuft im Netzwerk und auch ganz allein.

Englischer Name: **Bulwark**. (Das Repo heißt `morni-bollwek` – der Name des Modus ist Bollwerk.)

## So wird gespielt

1. In der **Wachstube** (Holzpalisaden-Lager mit Lagerfeuer) stimmt man mit dem **Kompass** über die Karte ab und sagt
   mit dem **grünen Farbstoff** „bereit“. Sind genug Leute da oder jemand bereit, läuft ein Countdown; sind alle
   bereit, geht es nach 5 Sekunden los. Höchstens 6 Leute pro Runde, Partys bleiben zusammen.
2. Auf der Karte steht das **Herz** – der Dorfbrunnen mit Maibaum bzw. die Klosterglocke – mit einer Lebensleiste.
   Gegner kommen auf festen **Wegen** aus mehreren Richtungen und wollen es zerstören.
3. **Aufbauphase** (zu Beginn 30 s, danach 25 s): im **Krämerladen** (Smaragd oder die Krämerin anklicken) Waffen,
   Rüstung, Tränke und **Baupläne** kaufen, Bauten auf die **Bauplätze** setzen, verbessern und ausbessern. Mit dem
   Knopf **„Welle starten“** sagt man, dass man fertig ist – sind alle fertig, kommt die Welle sofort.
4. **Welle:** Titel, Hornsignal, Leiste oben mit den übrigen Gegnern. Ist jeder Gegner besiegt, gibt es Gulden für
   alle und die nächste Aufbauphase.
5. Wer zu viel abbekommt, **liegt am Boden** statt zu sterben: Mitspieler **ducken sich daneben** (Schleichen,
   3 Sekunden) und holen ihn zurück. Sonst scheidet man aus und schaut zu – in der nächsten Aufbauphase ist man wieder
   dabei.
6. **Fällt das Herz, ist die Runde vorbei.** Gewertet wird, wie viele Wellen die Truppe überstanden hat – die besten
   Truppen je Karte stehen auf der Bestenliste.
7. **Zurück ins Spiel:** Verbindung mitten in der Runde weg (Spiel abgestürzt, Internet weg)? Der Platz bleibt 120
   Sekunden frei (`rueckkehr-sekunden`), die anderen verteidigen weiter. Wer rechtzeitig zurückkommt, steht genau dort,
   wo er war, mit Ausrüstung und Leben (wer am Boden lag, ist raus wie bisher). Sind alle weg, hält die Runde an –
   Gegner eingefroren –, bis jemand zurückkommt.

## Karten (eigene Entwürfe, aus dem Code gebaut)

| Karte | Herz | Wege der Gegner |
|---|---|---|
| **Talsperre Morni-Dorf** – ummauertes Bergdorf auf der Alm, Ecktürme, Alpenhäuser mit Balkon und Blumenkästen, Berge im Süden | Dorfbrunnen mit weiß-blauem Maibaum | Nordtor mit Torhaus · Bresche in der Ostmauer · Almtor im Westen |
| **Klosterhof** – Kloster auf einer Felsnase: Kirche mit Zwiebelturm, Kreuzgang, Konventbau, Kräutergarten, ferne Gipfel | Klosterglocke im offenen Glockenhaus | Pilgerweg (Treppe von Süden herauf) · Hängebrücke über die Schlucht (Osten) · Gratweg (Westen) |

Jede Karte hat 14 Bauplätze: 6 **auf** den Wegen (Meißelsteine quer über den Weg) und 8 **daneben** (Steinplatten
3×3). Beide Karten und die Wachstube entstehen beim ersten Start in einer eigenen leeren Welt (`bollwerk`); auf jeder
Karte läuft höchstens eine Runde gleichzeitig.

## Gegner (eigene Namen)

| Gegner | ab Welle | Besonderes |
|---|---|---|
| Plünderer | 1 | Nahkämpfer mit Axt |
| Flinker | 2 | schnell, wenig Leben |
| Schildträger | 3 | viel Leben, Turmpfeile richten nur halben Schaden an |
| Armbrustschütze | 4 | bleibt auf Abstand und schießt aufs Herz |
| Brecher | 6 | geht auf Bauten los, vierfacher Schaden an Palisaden |
| **Der Lawinenriese** (Boss) | 5, 15, 25 … | riesig, walzt Bauten nieder |
| **Die Nebelhexe** (Boss) | 10, 20, 30 … | wirft Tränke, ruft alle 8 s Flinke herbei |

Die Wellen wachsen mit der Welle und der Spielerzahl: Budget `(4 + 2,5 × Welle) × (1 + 0,6 × (Spieler − 1))`, Leben
`× (1 + 0,10 × (Welle − 1) + 0,20 × (Spieler − 1))`, Schaden `× (1 + 0,06 × (Welle − 1))`, höchstens 60 Gegner. Eine
neue Gegnerart wird in ihrer ersten Welle angekündigt; Boss-Wellen haben weniger Begleitung.

## Bauten

| Bau | Platz | Wirkung (Stufe 1 → 3) | Preis | Ausbau |
|---|---|---|---|---|
| Palisade | auf dem Weg | sperrt den Weg, bis sie zerschlagen ist (120 → 360 Leben) | 30 | 40, 70 |
| Kesselfalle | auf dem Weg (Kessel am Rand) | bremst (Langsamkeit I → III) und verbrüht (1 → 3 Schaden/s), Radius 3 → 5 | 45 | 45, 80 |
| Pfeilturm | neben dem Weg | schießt auf den Gegner, der dem Herzen am nächsten ist: 4 → 9 Schaden, Reichweite 12 → 18, schneller | 60 | 55, 95 |
| Heilbanner | neben dem Weg | heilt Verteidiger in der Nähe, auf Stufe 3 auch das Herz | 40 | 40, 70 |

Bauplan in die Hand, Rechtsklick auf den Bauplatz = bauen. Rechtsklick auf einen Bau = Menü mit Werten, **Verbessern**
und **Ausbessern** (1 Gulden je 4 Lebenspunkte). Zerstörte Bauten verschwinden, der Platz wird wieder frei; am Ende
der Runde ist die Karte wieder wie vorher.

## Krämerladen und Gulden

Startgeld 50 Gulden. Gulden gibt es für jeden besiegten Gegner (wer ihn besiegt; Türme, Kessel usw. teilen ihre
Beute auf alle auf) und für jede geschaffte Welle (`15 + 5 × Welle` für jede Person).

Im Laden: Eisen- und Diamantschwert, Kraft- und Meisterbogen, Pfeile, Ketten-, Eisen- und Diamantrüstung (nur
aufwärts), Heil-, Stärke- und Tempotrank, Goldapfel, die vier Baupläne, „Herz flicken“ (+50) und „Alle Bauten
ausbessern“. Alle Menüs sind Truhen und gehen auch mit Bedrock (Geyser).

## Befehle

| Befehl | Was er tut |
|---|---|
| `/karte [talsperre\|klosterhof]` | über die Karte abstimmen (ohne Angabe: Menü) – auch `/map`, `/vote` |
| `/bereit` | in der Wachstube bereit melden, in der Runde „Welle starten“ – auch `/ready` |
| `/shop` | Krämerladen (in der Aufbauphase) – auch `/laden` |
| `/zuschauen [spieler\|karte]` | einer Runde zuschauen – auch `/spectate` |
| `/verlassen` | Runde oder Zuschauen verlassen (zählt als gespielt) – auch `/leave` |
| `/stats [spieler]` · `/top [karte]` | Statistik und beste Truppen |
| `/bollwerk info\|status\|start [karte]\|welle <n> [karte]\|herz <leben> [karte]\|stopp\|neubauen\|neuladen` | Verwaltung (`moinmorni.bollwerk.admin`) |
| `/bollwerk geld <spieler> <betrag>` | Gulden in der laufenden Runde dazugeben – zum Testen (`moinmorni.bollwerk.admin`, im Netzwerk Owner und Admin über „Spielmodi verwalten“) |

Einstellungen stehen in der `config.yml` (Rückkehr nach Verbindungsabbruch, Countdown, Aufbauzeiten, Startgeld, Leben des
Herzens, Zeit am Boden …).

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Bollwerk nutzt sie,
wenn sie da ist (`softdepend`): Sprache der Spieler, Party (zusammen in eine Runde; wer später kommt, steigt als
Verstärkung in die laufende Runde seiner Party ein), Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby.
Ohne Netzwerk: deutsche Texte, Statistik nur lokal, „zur Lobby“ trennt die Verbindung mit einem Hinweis.

Ab Schnittstelle 2 meldet Bollwerk außerdem, wer gerade spielt (`playing`: von der Runde bis zum Ende oder Verlassen –
auch, wer bis zur nächsten Aufbauphase zuschaut, gehört noch dazu), hält nach einem Verbindungsabbruch den Platz frei
(`holdSlot`: die Lobby bietet „Zurück ins Spiel“ an, `0` nimmt das Angebot zurück) und schickt mit dem Bett in der
Wachstube über `askToLobby`. Ein älteres Netzwerk-Plugin kennt das nicht; dann läuft alles wie bisher.

Statistik nach jeder Runde (`stats("bollwerk", …)`, höher ist besser):

- hochgezählt: `games`, `kills`, `waves` (überstandene Wellen), `revives` (gerettete Mitspieler)
- Bestwert: `best_wave`

Zustand: `waiting` (keine Runde) oder `running`, dazu die Spielerzahl. Lokal: `spieler/<uuid>.yml` und
`bestenliste.yml` (beste Truppen je Karte).

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Bollwerk.jar` (Version aus `pom.xml`); das Netzwerk
holt sich das neueste.

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Bollwerk.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: tut so, als wäre es das Netzwerk-Plugin –
Bert spricht Englisch, Anna und Carl sind eine Party, alle Meldungen landen in `calls.log`), richtet einen
Paper-26.1.2-Server ein (offline, Port 25790, RCON 25890, kurze Aufbauzeiten, Herz mit 300 Leben) und schickt vier
Test-Spieler durch Wachstube, Abstimmung, Countdown, Laden, Bauen und Verbessern, Wellen, Turm, Palisade, Kessel,
Heilbanner, Am-Boden und Retten, Boss, Verstärkung aus der Party, Zuschauen, das Ende der Runde, Statistik und
Bestenliste, Geld zum Testen, Verbindungsabbrüche (rechtzeitig zurück, alle weg = Pause, zu lange weg) und eine
zweite Runde auf dem Klosterhof (`test/bollwerk-test.cjs`). Mit
`EXTRA_PLUGINS="…/ViaVersion.jar …/ViaBackwards.jar"` kommen ViaVersion und ViaBackwards dazu; Pfade und Ports lassen
sich per Umgebung ändern (siehe Kopf von `test/run.sh`).

---

Verfasst mit Claude 🤖
