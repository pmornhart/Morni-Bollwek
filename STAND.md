# Stand – Morni Bollwerk

**Was es ist:** Der Verteidigungs-Modus des Netzwerks morni.net (Paper-Plugin `MorniBollwerk`, Java 25, Paper 26.x):
Verteidigt das Dorf gegen Welle um Welle – zwischen den Wellen aufrüsten und bauen. 1–6 Leute zusammen (auch allein).
Repo `pmornhart/morni-bollwek` (Name so gewollt), Jar `Morni-Bollwerk.jar`, englisch „Bulwark“.

## Aufbau

- `src/main/java/net/morni/bollwerk/`: `BollwerkPlugin` (Start, Welt, Takt), `Settings` (config.yml), `Texts` (de/en),
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
- `src/api/java/net/morni/api/MorniNet.java`: Kopie der Netzwerk-Schnittstelle (nicht in der Jar).
- `test/`: `run.sh` (echter Server), `bollwerk-test.cjs` (mineflayer), `rcon.cjs`, `fake-network/` (Test-Netzwerk).

## Stand (16.11.2026)

- Version 0.1.2 (Branch `claude/morni-net`, wartet auf Philips Go): **Umbenannt in morni.net** (vorher MoinMorni,
  16.11.2026) – Paket `net.morni.bollwerk`, Schnittstelle `net.morni.api.MorniNet` vom Netzwerk-Plugin „MorniNet“
  (`softdepend: [MorniNet]`), Recht `morninet.bollwerk.admin`, Leiste unten „morni.net“, Texte und Plugin-Angaben mit
  morni.net. Plugin-Name, Datenordner `plugins/MorniBollwerk`, Wachstube und Karten bleiben (kein Neubau).
  `test/run.sh` löscht alte Jars im Test-Server (auch umbenannte). Geprüft: 46 Unit-Tests, Paper 26.1 übersetzt; echt
  mit Paper 26.1.2 **99/99**, mit ViaVersion/ViaBackwards 5.12.2 ebenfalls **99/99**, Server-Log ohne Fehler.
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

- Philip: Bilder im echten Client ansehen; Go für `main` (dann Release v0.1.2).
- Einstellungen (config.yml) im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
- Ideen: weitere Karten, Sitzplätze für Zuschauer, Belohnungen für Bestleistungen im Netzwerk.
