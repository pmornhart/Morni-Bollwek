# Morni Bollwerk

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo).
  Auch keine Test-Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter.
- Das Repo heißt `pmornhart/morni-bollwek` (Tippfehler im Namen ist gewollt), der Modus heißt **Bollwerk**
  (englisch „Bulwark“), die Jar `Morni-Bollwerk.jar`, das Plugin `MorniBollwerk`.
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen; `TextsTest` prüft Platzhalter, Zeichen und dass jede Gegnerart, jeder
  Bau, jedes Angebot und jede Karte einen Namen hat).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (keine Mojang-, Nintendo-, Hypixel- oder Inhalte anderer
  Server): nur die Spielidee „Wellen abwehren, zwischendurch aufrüsten“, eigene Namen, eigene Karten, eigene Gegner.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar. Nur `net/Connected` darf sie benutzen.
- Bollwerk muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Rechenregeln (Wellen, Gulden, Bauten, Herz, Bestenliste, Abstimmung) gehören in `logic/` bzw. `data/` – ohne Server,
  mit Tests. Werte dort nur zusammen mit den Tests ändern.
- Ändern sich Wachstube oder Karten: `Construction.VERSION` erhöhen – dann baut der Server beim nächsten Start neu.
  Neue Wege/Bauplätze in `MapType` müssen `MapLayoutTest` bestehen.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Bollwerk.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echte Client-Texturen nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Bollwerk nur die
  `config.yml` und Verwaltungs-Befehle zum Testen; die Anbindung an den Einstellungs-Katalog gehört ins Netzwerk-Repo.
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README).
- `STAND.md` nach jedem größeren Schritt aktualisieren.
