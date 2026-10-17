# Stand für die Weiterarbeit

Branch: `claude/busy-maxwell-frjsz4`. Plan und Meilensteine: [PLAN.md](PLAN.md).

## Fertig
- M0–M8e und der Absturzschutz (Lithium-Crash behoben, siehe `safety/Safety`, `safety/CrashGuard`,
  `mixin/CrashGuard*`, `client/ClientSafety`). 94/94 Gametests grün – auch zusammen mit Lithium,
  ModernFix, FerriteCore, Collective und Jade.

## In Arbeit: M8f – Boss „Wächter der letzten Stadt“ (Commit „M8f (unfertig)“)
Zwei Gametests in `LastCityTests` schlagen noch fehl:
- `phasesCallGuardiansAndShedPlates`: Bei 2/3 und 1/3 Leben erscheinen keine Ruinenwächter (0 gefunden),
  obwohl die Platten fallen – `CityColossus.callGuardians` prüfen.
- `onlyAcceleratedTimeWearsHimDown`: Außerhalb beschleunigter Zeit heilt er sich nicht (bleibt bei 540) –
  die Regeneration in `CityColossus` prüfen.
Danach: Vorführung (Glocke in den Ruinen von +1000, Erwachen, Phasen) und Bilder.

## Als Nächstes
1. Einsteiger-Hilfe: „Erste Schritte“-Seite mit Bildern ganz vorne in der Chronik, Shift-Tooltips
   „So benutzt du das“, später animierte 3D-Vorführungen; JEI-Kategorie für Uhrmacher-Rezepte.
2. Absturzschutz vervollständigen: Client-Ereignisse (HUD, Welt-Effekte, Client-Ticks, Netzwerk-Empfänger
   in `TimeModClient`, `ParadoxClient`, `ChronosClient`, `TimeStreamClient`, `ClientAging`) mit
   `ClientSafety.render` bzw. `Safety.run` einpacken; Logik in `ClientTimeEngine` absichern;
   Post-Effekte bei aktivem Iris-Shaderpack abschalten; `defaultRequire: 0` in den Mixin-Konfigurationen erwägen.
3. M9 (nahe Vergangenheit, Eiszeit), M10 (tiefe Vergangenheit, Dinosaurier), Zeitrisse,
   M11 (Konfiguration, Jade/EMI/REI/Mod Menu, API, Balancing).

## Testen
- Alle Gametests: `bash tools/run-gametests.sh`, einzelne Klassen: `bash tools/run-gametests-only.sh TownTests`.
- Mit echtem Modpack: `python3 -I tools/fetch-compat-mods.py`, dann `GRADLE_ARGS=-Pcompat bash tools/run-gametests.sh`.
- Jar bauen: `./gradlew remapJar` → `build/libs/timemod-0.1.0.jar`.
