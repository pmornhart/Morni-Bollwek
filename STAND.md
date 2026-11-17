# Stand – Morni Bollwerk

**Was es ist:** Der Verteidigungs-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniBollwerk`, Java 25, Paper 26.x):
Verteidigt das Dorf gegen Welle um Welle – zwischen den Wellen aufrüsten und bauen. 1–6 Leute zusammen (auch allein).
Repo `pmornhart/morni-bollwek` (Name so gewollt), Jar `Morni-Bollwerk.jar`, englisch „Bulwark“.

## Aufbau

- `src/main/java/de/moinmorni/bollwerk/`: `BollwerkPlugin` (Start, Welt, Takt), `Settings` (config.yml), `Texts` (de/en),
  `Ui`, `Items`, `Holo` (schwebende Schrift je Sprache), `Sidebar`, `Boards` (Leiste + Tafeln der Wachstube), `Commands`.
- `logic/` (ohne Server, mit Tests): `EnemyType`, `WavePlan` (Zusammensetzung und Skalierung), `Economy` (Gulden,
  Geldbeutel), `Offer` (Laden, Stufen), `DefenceType` (Bauten, Stufen, Preise), `Targeting` (Zielwahl der Türme),
  `Heart`, `Groups` (Partys in eine Runde), `Votes` (Kartenwahl).
- `data/`: `PlayerData`, `Store` (YAML, Hintergrund), `Leaderboard` (beste Welle je Karte).
- `world/`: `Plan`, `Decor`, `MapKit` (Wege, Bauplätze, Alpenhaus, Lager), `MapLayout` (Wege/Bauplätze/Herz, rein),
  `MapType` (Talsperre, Klosterhof), `TalsperrePlan`, `KlosterhofPlan`, `HallPlan` (Wachstube), `Construction`
  (baut nur bei neuer Bau-Version), `VoidGenerator`.
- `game/`: `Game` (Runde: Aufbau, Welle, Gegner-Verhalten, Bauten, Am-Boden/Retten, Ende), `Games`, `Fighter`, `Enemy`,
  `Defence` (Blöcke merken und zurücksetzen), `Kit`, `Shop`, `DefenceMenu`, `LangBar`.
- `hall/`: `Hall` (Wachstube, Countdown, Abstimmung, Partys), `HallMenus`. `menu/`: `Menu`, `MenuListener`.
- `listen/`: `PlayerListener`, `GameListener`, `GuardListener`. `net/`: `Network`, `Connected`, `Standalone`.
- `src/api/java/de/moinmorni/api/MoinMorni.java`: Kopie der Netzwerk-Schnittstelle (nicht in der Jar).
- `test/`: `run.sh` (echter Server), `bollwerk-test.cjs` (mineflayer), `rcon.cjs`, `fake-network/` (Test-Netzwerk).

## Stand (16.11.2026)

- Version 0.1.4 (Branch `claude/zurueck-ins-spiel`, wartet auf Philips Go): **Zurück ins Spiel** (Philip: „Wenn das Game
  crasht, soll man zurück zum Spiel können“ und vor der Lobby nachfragen, solange man spielt). Netzwerk-Schnittstelle 2
  (Kopie byte-gleich): `playing` vom Start der Runde bis zum Ende bzw. Verlassen (wer bis zur nächsten Aufbauphase
  zuschaut, spielt weiter), das Bett „Zurück zur Lobby“ ruft `askToLobby`, Verbindung mitten in der Runde weg → Platz
  `rueckkehr-sekunden` frei (oben in der config.yml, Vorgabe 120, 0 = aus) und `holdSlot(uuid, s)`; die anderen
  verteidigen weiter, Gulden, Ausrüstung und Kills bleiben. Rechtzeitig zurück → `PlayerState` stellt Ort, Inventar,
  Leben, Hunger und Effekte wieder her, „Willkommen zurück!“, `holdSlot(uuid, 0)`; wer beim Gehen am Boden lag, ist raus
  wie bisher und in der nächsten Aufbauphase wieder dabei. Sind alle weg (z. B. allein), hält die Runde an (Gegner
  eingefroren, keine Aufbau-Zeit läuft); erst wenn niemand mehr zurückkommen kann, ist sie vorbei. Zu spät → wie bisher
  (Aufbauphase: auf die Karte, Welle: zuschauen); Rundenende → `holdSlot(uuid, 0)`. Älteres Netzwerk-Plugin
  (`LinkageError`) → einmal Hinweis im Log, alles wie bisher. `/bollwerk geld <spieler> <betrag>` (Philip: Owner und
  Admins sollen sich zum Testen Geld geben können): Gulden in der laufenden Runde, Recht `moinmorni.bollwerk.admin`,
  Bestätigung de/en. `/bollwerk status` zeigt `angehalten` und `weg`. Test-Netzwerk schreibt `playing`, `ask-to-lobby`
  (fragt nur, solange „spielt“), `hold-slot` mit und spielt mit `api=1` ein älteres Netzwerk-Plugin. Geprüft: 48
  Unit-Tests (neu `SettingsTest`; JUnit 6.1.3, Issue #3), Paper 26.1 übersetzt, echt mit Paper 26.1.2 (`test/run.sh`,
  `rueckkehr-sekunden: 20`, Server mit 1,5 GB): **118/118**, Log ohne Fehler – neu: „spielt“ in der Runde und danach nicht mehr; Geld per Befehl (+30,
  Bestätigung, Anna liest es, ohne Recht nichts); Bert weg → Anna erfährt es, Runde läuft weiter, holdSlot 20; Bert
  zurück → genau an derselben Stelle, gleiche Ausrüstung und gleiches Leben, „Welcome back“, holdSlot 0; beide weg →
  Runde angehalten, Gegner eingefroren, beide zurück → läuft weiter; zu lange weg → „nicht rechtzeitig zurückgekommen“,
  holdSlot 20, 0, 20, 0, später zurück wie bisher als Zuschauer in der Welle; Bett → `askToLobby`, älteres
  Netzwerk-Plugin → wie bisher `toLobby`. Test wartet beim ersten Rundenstart bis 45 s (auf dem ausgelasteten Rechner
  lädt der Server die Chunks der Karte langsam – vorher 12 s).
- Version 0.1.3 (16.11. veröffentlicht, Release v0.1.3): **Name wieder MoinMorni** – 0.1.2 (16.11.
  veröffentlicht) hatte Bollwerk kurz an einen neuen Netzwerk-Namen angepasst; der war nicht verfügbar, darum ist der
  Code wieder wie in 0.1.1 (Umbenennung per `git revert` zurückgenommen): Paket `de.moinmorni.bollwerk`,
  Schnittstelle `de.moinmorni.api.MoinMorni`, Netzwerk-Plugin „MoinMorni“, Recht `moinmorni.bollwerk.admin`, Leiste
  unten wieder moinmorni.net. Plugin-Name, Datenordner `plugins/MorniBollwerk`, Wachstube und Karten waren nie anders
  (kein Umzug, kein Neubau). `test/run.sh` löscht alle alten Jars im Test-Server. Geprüft: 46 Unit-Tests, Paper 26.1
  übersetzt, echt mit Paper 26.1.2: **99/99**, mit ViaVersion/ViaBackwards 5.12.2 ebenfalls **99/99**, Bollwerk
  meldet sich beim Test-Netzwerk „MoinMorni“, Server-Log ohne Fehler.
- Version 0.1.1 (16.11. veröffentlicht, Release v0.1.1): Funde aus dem Trailer-Dreh. Wachstube
  bekommt ein Plugin-Chunk-Ticket (`keepHallLoaded`) – vorher entlud der Server sie nach dem Start ohne Spieler und die
  nicht dauerhaften Tafeln waren weg; `refreshHall` stellt fehlende Tafeln neu auf (`Holo.valid`). Gegner gehen nicht
  mehr um eine stehende Palisade herum auf Spieler los, die dahinter stehen (`Game.behind`), sondern schlagen auf sie
  ein. Echter Test: zwei neue Prüfungen (vorher rot: 97/99), mit Korrektur 99/99.
- Version 0.1.0 (15.11. veröffentlicht, Release v0.1.0): alles aus dem README.
- Geprüft: 46 Unit-Tests (Wellen nach Welle und Spielerzahl, Boss-Wellen, Gulden, Laden-Stufen, Bauten, Zielwahl,
  Herz, Bestenliste, Spielerdaten, Partys, Abstimmung, Wege/Bauplätze der Karten, Texte).
- Echter Test mit Paper 26.1.2 + ViaVersion/ViaBackwards 5.12.2 und vier Test-Spielern (mineflayer): 97/97 –
  Wachstube, Abstimmung (Menü und Befehl), bereit/Countdown, Start auf der Talsperre, Laden (Kauf, zu teuer, Stufen),
  Pfeilturm und Palisade bauen (falscher Platz abgelehnt), Kesselfalle und Heilbanner, Welle 1 (10 Gegner bei 2 Leuten),
  Turm schießt, Palisade wird beschädigt, Kill mit Belohnung, Wellen-Bonus, Turm per Rechtsklick auf Stufe 2, Am Boden +
  Retten durch Ducken, Wellen 2/3 (Schildträger), Verstärkung aus der Party, Zuschauen, Boss-Welle 5 (Lawinenriese),
  Herz fällt von selbst, Netzwerk-Statistik, Bestenliste, Spielerdatei, Karte wieder aufgeräumt, zweite Runde auf dem
  Klosterhof, Lobby-Knopf.
- Wege geprüft (Welle 6, niemand verteidigt): auf beiden Karten erreichen alle Gegner aller drei Wege das Herz,
  0× festgesteckt (Notfall-Teleport nie nötig).
- Paper 26.3 (Test-Spieler über ViaBackwards ohne Bewegung): Bau der Welt, Runde, Boss-Welle 5, Herz fällt, Statistik –
  ohne Fehler im Log.
- Nicht echt geprüft: Aussehen von Wachstube, Karten, Bauten und Gegnern im echten Client (keine Bilder gemacht),
  Bedrock-Spieler, Nebelhexe (Welle 10) im Spiel, Bauten der Stufe 3, Brecher gegen Palisaden mit echtem Kampf.

## Offen

- Philip: Go für `main` mit 0.1.4 (Zurück ins Spiel). Netzwerk-Repo: Einstellungs-Katalog um `rueckkehr-sekunden`
  ergänzen (0–600, Vorgabe 120); Nachfrage vor der Lobby und „Zurück ins Spiel“ baut das Netzwerk-Plugin.
- Nicht echt geprüft: Rückkehr mit echtem Client über das echte Netzwerk (Lobby-Angebot); Bilder im echten Client.
- Einstellungen (config.yml) im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
- Ideen: weitere Karten, Sitzplätze für Zuschauer, Belohnungen für Bestleistungen im Netzwerk.
