# TIME – Gesamtplan

> Fabric · Minecraft 1.20.1 · Java 17 · Anzeigename **TIME** · Mod-ID `timemod`

Dieses Dokument ist der vollständige Bauplan: was die Mod kann, wie es technisch funktioniert, in welcher Reihenfolge gebaut wird und wie jede Stufe getestet wird.

---

## Inhalt

1. [Grundprinzip: wie „echte“ Zeitmanipulation funktioniert](#1-grundprinzip)
2. [Die Zeit-Engine (Vorspulen, Zeitlupe, Stopp)](#2-die-zeit-engine)
3. [Rückspulen (bis 60 Sekunden)](#3-rückspulen)
4. [Filter: was beeinflusst wird](#4-filter)
5. [Chronon – die Energie der Zeit](#5-chronon)
6. [Fortschritt: Stufe I bis V](#6-fortschritt)
7. [Bosse, Strukturen, Zeitrisse](#7-bosse-strukturen-zeitrisse)
8. [Die Zeitmaschine](#8-die-zeitmaschine)
9. [Zukunft: +1 bis +1000 Jahre](#9-zukunft)
10. [Vergangenheit: −1 bis −245 Mio. Jahre](#10-vergangenheit)
11. [Mehrspieler](#11-mehrspieler)
12. [Optik und Sound](#12-optik-und-sound)
13. [Kompatibilität mit anderen Mods](#13-kompatibilität)
14. [Konfiguration, Befehle, Barrierefreiheit](#14-konfiguration-befehle-barrierefreiheit)
15. [Technische Architektur](#15-technische-architektur)
16. [Tests: wie „akkurat“ bewiesen wird](#16-tests)
17. [Meilensteine](#17-meilensteine)
18. [Ehrliche Grenzen](#18-ehrliche-grenzen)
19. [Offene Fragen](#19-offene-fragen)

---

## 1. Grundprinzip

**Kein Speed-Effekt, kein `/gamerule randomTickSpeed`.**
Minecraft läuft in *Ticks*: 20 Mal pro Sekunde wird jede Kuh, jede Pflanze, jeder Ofen und jede Wasserströmung einen Schritt weitergerechnet.

- `randomTickSpeed` beschleunigt nur das zufällige Wachstum. Kühe, Öfen, Wasser, Redstone und Tag/Nacht bleiben gleich schnell. Deshalb wirkt das komisch.
- Ein Speed-Effekt lässt Mobs weiter pro Schritt laufen. Ihre KI entscheidet aber genauso selten wie vorher. Das ist kein schnelleres Video, sondern ein anderes Video.

**TIME rechnet stattdessen die Spiellogik selbst öfter (oder seltener) pro Sekunde.**
Bei 2× wird jedes betroffene Objekt pro echtem Tick **zweimal** komplett weitergerechnet: KI, Wegfindung, Physik, Wachstum, Animation, Maschinen. Die Kuh geht also exakt den Weg, den sie in 2 Sekunden normaler Zeit gegangen wäre, nur in 1 Sekunde. Das ist genau wie ein Video auf 2-facher Geschwindigkeit. Langsam schlendern sieht dann aus wie Sprinten, ist aber Schlendern.

- **Zeitlupe** funktioniert umgekehrt: Bei 0,5× wird nur jeden zweiten Tick gerechnet. Der Client interpoliert dazwischen weich, damit es nicht ruckelt.
- **Stopp** ist 0×: Es wird gar nicht gerechnet, und auch der Client friert ein.
- **Rückspulen** spielt aufgezeichnete Zustände rückwärts ab.

Das ist auch der Grund, warum es automatisch mit **allen anderen Mods** funktioniert: Wir beschleunigen nicht „Kühe“, sondern den Tick an sich. Jede Mod-Kreatur und jede Mod-Maschine tickt über dieselben Stellen im Spiel.

---

## 2. Die Zeit-Engine

### 2.1 Zeitzonen
Jede Manipulation ist eine **Zeitzone**:

| Eigenschaft | Werte |
|---|---|
| Form | Kugel, Quader, ein einzelner Block, eine einzelne Entity, ganze Dimension (global) |
| Faktor | 0× (Stopp), 0,05× – 10× lokal, bis 5× global (an die Server-Leistung gekoppelt) |
| Filter | siehe [Kapitel 4](#4-filter) |
| Besitzer | Spieler oder Block (Zeitfeld-Projektor) |
| Dauer | Ticks oder dauerhaft (solange Chronon fließt) |

Bruchfaktoren wie 1,5× laufen über einen Akkumulator, also abwechselnd 1 und 2 Ticks. Der Client glättet das, siehe 2.4.

**Überlappende Zonen haben eine feste Priorität:** Stopp > Rückspulen > Zeitlupe/Vorspulen. Bei gleichem Typ gewinnt das stärkere Werkzeug. Faktoren multiplizieren sich nicht, das verhindert Exploits mit 10× · 10×.

### 2.2 Was wird alles beschleunigt? Wirklich ALLES

| System | Lokal | Global |
|---|:-:|:-:|
| Entities: KI, Wegfindung, Bewegung, Physik, Animation, Alter (Babys wachsen), Zucht-Cooldowns | ✅ | ✅ |
| Projektile, Pfeile, Feuerbälle, TNT-Zündschnur, fallender Sand/Kies | ✅ | ✅ |
| Items am Boden: Despawn-Timer, Aufheben-Verzögerung, XP-Kugeln | ✅ | ✅ |
| Block-Entities: Ofen, Brauen, Trichter, Spawner, Lagerfeuer, Leuchtfeuer, **Mod-Maschinen** | ✅ | ✅ |
| Zufalls-Ticks: Pflanzen, Setzlinge→Bäume, Gras, Eis, Laub, Kupfer-Oxidation, Amethyst, Zuckerrohr, Kelp, Eier | ✅ | ✅ |
| Geplante Ticks: Redstone, Beobachter, Kolben, Wasser- und Lavafluss, Feuer | ✅ (Zonen-Eigenzeit) | ✅ |
| Statuseffekte und deren Dauer | ✅ | ✅ |
| Dorfbewohner-Tagesablauf, Restock, Überfälle, Händler-Timer | – | ✅ |
| Tag/Nacht, Mondphase, Wetter (Regen- und Gewitterdauer) | – | ✅ |
| Client: animierte Texturen (Wasser, Lava, Feuer, Portale), Partikel, Wolken, Himmel, Regen | ✅ (in der Zone) | ✅ |
| Sounds: Tonhöhe = Faktor, bei 2× also hoch und schnell wie im Video | ✅ | ✅ |

Tag, Wetter und Mondphase gibt es in Minecraft nur einmal pro Dimension. Deshalb lassen sie sich ehrlicherweise **nur global** verändern.

### 2.3 Der Nutzer selbst
- Standardmäßig steht der Benutzer **außerhalb** der Zeit. Er bewegt sich normal, während die Welt rast oder stillsteht.
- Optional per Einstellung: **„Ich bin Teil der Zeit“**. Dann läuft man mit, alles inklusive Hunger und Regeneration. Das ist echtes Video-Gefühl.

### 2.4 Weiche Darstellung (Client)
- Der Server schickt jedem Client die Zonen samt Faktor.
- Der Client skaliert die Interpolation pro Entity mit dem Faktor. Dadurch bleiben 0,1×-Zeitlupen butterweich und Vorspulen ruckelt nicht.
- Im Stopp werden Animationen, Partikel und animierte Texturen in der Zone eingefroren. Flammen stehen still, Wasser wird zu Glas.

### 2.5 Leistung
- Globales 5× kostet den Server bis zu 5× Rechenleistung.
- Ein **Zeitmotor-Budget** misst die Tick-Dauer (MSPT) und drosselt automatisch, bevor der Server laggt. Im HUD steht dann „Zeitmotor überlastet“.
- Lokale Zonen sind günstig, weil nur der Inhalt der Zone mehrfach tickt.

---

## 3. Rückspulen

### 3.1 Prinzip: das Zeitjournal
Ein Ringpuffer pro Dimension speichert die **letzten 60 Sekunden** (1200 Ticks, einstellbar bis 60 s):

| Aufgezeichnet | Wie |
|---|---|
| **Jede Blockänderung**: alter Zustand und alte Block-Entity-Daten | Ein Hook in `LevelChunk.setBlockState`. Dadurch werden automatisch **alle** Änderungen erfasst: Abbauen, Bauen, Wachstum, Baum-Wachstum, Wasser- und Lavafluss, Feuer, Explosionen, Endermen, Kolben, auch von anderen Mods |
| **Bewegung jeder Entity pro Tick**: Position, Rotation, Kopfdrehung, Geschwindigkeit, Pose, Laufanimation | Kompaktes Binärformat |
| **Entity-Zustand** (Leben, Inventar, KI-Erinnerung, Mod-Daten) | Vollständige NBT-Keyframes alle 10 Ticks und bei Änderung |
| **Spawn und Despawn**: was erscheint, verschwindet oder stirbt | Beim Entfernen wird die komplette NBT mit derselben UUID gespeichert, damit es wieder auftaucht |
| **Spieler**: Position, Inventar, Leben, Hunger, XP, Effekte | Differenzen pro Tick |
| **Welt**: Tageszeit, Wetter | pro Tick |
| **Maschinen**: Ofen-Fortschritt, Mod-Maschinen, Energie | NBT-Schnappschuss bei `setChanged()`, gedrosselt |

### 3.2 Abspielen
- Die Aufzeichnung wird **rückwärts Tick für Tick** angewendet.
- Die Kuh läuft ihren Weg rückwärts, gewachsenes Getreide schrumpft und ein Baum „entwächst“ zurück zum Setzling.
- Abgebaute Blöcke kommen wieder, gestorbene Mobs stehen wieder auf. Ihre Drops verschwinden, weil sie zu dem Zeitpunkt noch nicht existierten. Despawnte Items erscheinen wieder.
- Auch **du** läufst deinen Weg rückwärts, und dein Inventar wird zurückgesetzt.
- Die KI ist während des Rückspulens aus. Danach geht die Zeit vom gewählten Punkt **neu** weiter, als neuer Zweig.
- Rückspul-Tempo: 1× (dauert so lange wie das Original), 2×, 4× oder „Sprung“ (sofort).
- **Zeitleisten-Scrubbing** mit der Chronos-Uhr: Taste halten und mit dem Mausrad den Zielpunkt wählen. Ein Geisterpfad zeigt, wo du warst.

### 3.3 Speicher
- Aufgezeichnet wird nur, wenn jemand in der Dimension ein Rückspul-Werkzeug oder eine entsprechende Maschine besitzt.
- Erfasst werden nur Bereiche rund um diese Spieler, Standardradius 96 Blöcke.
- Grobe Rechnung: 300 Entities × 1200 Ticks × ca. 40 Byte ≈ **15 MB** plus Blockänderungen. Das ist unproblematisch.

### 3.4 Anti-Dupe-Regeln
Rückspulen ist die gefährlichste Mechanik für Item-Duplikation. Deshalb gilt:
- Alles, was in der Zeitspanne die Zeitblase verlassen hat, gilt als **zeitlich verankert**: Items in Endertruhen, in anderen Dimensionen, in entladenen Chunks oder bei Spielern außerhalb der Blase. Diese Items werden beim Rückspulen **nicht** zurückgegeben.
- Gametests prüfen alle bekannten Dupe-Wege automatisch.

### 3.5 Bonus: „Schleife“ (Endgame-Talisman)
Stirbst du, spult die Welt um dich automatisch 10 Sekunden zurück, ähnlich wie ein Totem, aber mit Zeit. Das verbraucht eine riesige Menge Chronon und hat 30 Minuten Abklingzeit.

---

## 4. Filter

Filtermodule werden in Werkzeuge eingesetzt (ab Stufe III). In der Chronos-Uhr sind alle Filter frei kombinierbar:

- **Entities**: Tiere · Monster · Spieler · Dorfbewohner · Projektile · Items/XP · Fahrzeuge · Bosse (standardmäßig immun)
- **Natur**: Pflanzenwachstum · Bäume · Gras/Moos-Ausbreitung · Eis/Schnee · Flüssigkeiten · Feuer
- **Technik**: Block-Entities/Maschinen · Redstone · Trichter/Transport
- **Welt** (nur global): Tag/Nacht · Wetter · Mondphase
- **Eigene Listen**: Freund/Feind, Whitelist/Blacklist per Entity-Typ oder Block-Tag

Beispiele: „nur Pflanzen 10× vorspulen“, „nur Monster stoppen“, „nur Tag/Nacht zurückdrehen“.

Mods und Packs können mit dem Tag `#timemod:time_immune` Dinge immun machen. Standardmäßig gilt das für Wither, Enderdrache und Warden, umschaltbar.

---

## 5. Chronon

Zeitmanipulation ist **teuer**. Die Währung dafür heißt **Chronon (Ch)**.

### Quellen
| Quelle | Stufe |
|---|---|
| **Aufziehen**: Rechtsklick halten mit Tick-Sound, nur für Stufe-I-Werkzeuge, kein Chronon nötig | I |
| **Chronit-Kristalle** in einem Zeitkondensator „ausbrennen“ | II |
| **Lebenszeit-Extraktor**: entzieht Mobs Lebenszeit. Sie altern sichtbar, werden grau und sterben an Altersschwäche. Das ergibt eine Mob-Farm für Zeit | III |
| **Zeitrisse** schließen (siehe 7.3) | II–IV |
| **XP opfern**: Notfallquelle, schlechter Kurs | I |

### Speicher
Taschen-Zeitzelle → Chronit-Zelle → Paradox-Zelle → Unendlichkeits-Zelle (Endgame, riesig, aber nicht unendlich).

### Kosten (Richtwerte, werden ausbalanciert)
- Stopp: `Radius² × Sekunden × k`
- Vorspulen: `Volumen × (Faktor − 1)² × Sekunden`. Das wächst quadratisch, also wird 10× sehr teuer.
- Rückspulen: `Fläche × Sekunden³ / c`. 60 Sekunden für das ganze Gebiet ist das teuerste im ganzen Mod.

### Paradox-Druck
Wer die Zeit übermäßig nutzt, lässt den **Paradox-Druck** in der Region steigen. Dann entstehen Zeitrisse mit Gefahren und seltenen Belohnungen. Das ist Risiko gegen Nutzen.

---

## 6. Fortschritt

### Stufe I – Der Uhrmacher (frühes Spiel)
**Materialien:** Kupfer, Gold, Redstone, Quarz und **Messing** (Kupfer + Goldnuggets in der Uhrmacher-Werkbank).
**Station:** *Uhrmacher-Werkbank*, ein Präzisionstisch. Einzelteile wie Zahnrad, Feder, Unruh und Ziffernblatt entstehen dort.

| Item | Wirkung | Antrieb |
|---|---|---|
| **Taschenuhr** | zeigt Zeit, Mondphase, Chronon und Zeitzonen in der Nähe | – |
| **Wachstums-Sanduhr** | ein Block (Pflanze, Setzling, Ofen, Mod-Maschine) 4× für 30 s | aufziehen |
| **Bremsfeder** | ein Mob 0,5× für 10 s | aufziehen |
| **Moment-Linse** | einen Mob oder ein Projektil 3 s einfrieren | aufziehen |
| **Rückblick-Uhr** | dich selbst 5 s zurück (Position und Leben) | aufziehen, 60 s Abklingzeit |

### Stufe II – Chronit (mittleres Spiel)
- **Chronit-Erz** in der Tiefenschiefer-Schicht, häufiger nahe Antiken Städten. Es **flackert durch die Zeit**: Es ist nur zeitweise da und lässt sich nur dann abbauen. Mit der Moment-Linse kann man es festhalten.
- **Zeitsand** in Wüsten-Tiefen. Daraus werden Sanduhren und Glas gemacht.
- **Echoscherben** (Vanilla) als Zutat.
- Rohes Chronit muss in einer beschleunigten Zone **reifen**. Die Mod benutzt also ihre eigene Mechanik.

| Item / Block | Wirkung |
|---|---|
| **Stillstand-Uhr** | Zeitstopp, Radius 12, 2 s (durch Meisterschaft bis 9 s). Du bewegst dich frei |
| **Zeitraffer** | Zone 2–4× für 30 s |
| **Zeitlupen-Pendel** | Zone 0,25–0,5× |
| **Rückspul-Spule** | Zone, Radius 16, bis 10 s zurück |
| **Chrono-Wurfmesser** | Im Stopp geworfen, schweben sie in der Luft und fliegen los, sobald die Zeit weiterläuft |
| **Zeitfeld-Projektor** (Block) | stationäre Zone für Farmen, verbraucht dauerhaft Chronon |
| **Zeitkondensator** (Block) | Chronit → Chronon |

### Stufe III – Temporaltechnik (spätes Spiel, Netherite und End)
- **Temporal-Werkbank**: Multiblock mit drehenden Ringen. Hier werden Module eingebaut.
- **Filtermodule** (Kapitel 4) und **Verstärkungsmodule** für Radius, Dauer, Stärke und Effizienz.
- **Chrono-Rüstung**: Mit dem vollen Set kannst du dich **in der gestoppten Zeit anderer bewegen**, ganz nach JoJo-Regel. Außerdem bekommst du Teilimmunität gegen Zeitlupe.
- **Lebenszeit-Extraktor** und **Zeitzellen** der Stufe III.
- Zum Fortschritt geht es nur über **Boss 1 und Boss 2**.

### Stufe IV – Herr der Zeit
**Die Chronos-Uhr**, das Werkzeug mit voller Kontrolle. Sie wird aus allen Stufe-II-Werkzeugen, Modulen, Bosskernen, einem Netherstern und dem **Herz der Zeit** (Boss 3) gecraftet.
- Stopp bis 10 s, Radius bis 64 oder global
- 0,05×–10× lokal und bis 5× global
- Rückspulen bis 60 s mit Zeitleisten-Scrubbing
- alle Filter
- **Radialmenü** (Taste `R`), Modus auf Mausrad, HUD mit Zeitleiste
- **Meisterschaft**: Nutzung schaltet längere Stopps, größere Radien und bessere Effizienz frei

### Stufe V – Die Zeitmaschine
Siehe [Kapitel 8](#8-die-zeitmaschine).

**Komplette Fortschrittsanzeige** über Advancements, ein eigenes Handbuch *„Die Chronik“* im Spiel und Rezepte in EMI/REI.

---

## 7. Bosse, Strukturen, Zeitrisse

### 7.1 Strukturen
| Struktur | Ort | Inhalt |
|---|---|---|
| **Verfallener Uhrturm** | Ebenen, selten | erste Baupläne, Messing-Loot |
| **Vergessenes Observatorium** | Berggipfel | Hinweise auf Epochen, Teleskop zeigt Zeitrisse |
| **Chrono-Ruinen** | Deep Dark | Chronit-Adern, Zeitkern-Fragmente |
| **Zeitlose Werkstatt** | Untergrund, versiegelt | **Boss 1** |
| **Zeitlose Zitadelle** | Äußeres End | **Boss 3** (Zugang zum Zeitstrom) |

### 7.2 Bosse
1. **Der Uhrmacher**, ein Uhrwerk-Titan. Er springt in der Zeit zurück, wenn er getroffen wird, und schießt Zahnrad-Sägen. Seine Schwachstelle zeigt sich nur in Zeitlupe. Drop: *Uhrwerkskern*.
2. **Der Chronophage**, ein Zeitfresser. Er macht dich langsam und heilt sich, indem er seinen Schaden zurückspult. Töten kann man ihn nur, wenn man im richtigen Moment die Zeit stoppt. Drop: *Phagenherz*.
3. **Paradox**, dein eigenes Echo im Zeitstrom. Es benutzt Kopien deiner Ausrüstung und deiner Zeitfähigkeiten gegen dich. Drop: **Herz der Zeit**.

Dazu kommen **Epochen-Bosse** in der Zeitmaschinen-Welt (Kapitel 9 und 10).

### 7.3 Zeitrisse (dynamische Ereignisse)
Steigt der Paradox-Druck oder geht ein Gewitter nieder, öffnen sich Risse. Daraus kommen **Anomalien**:
- Mobs aus anderen Epochen, etwa ein kleiner Raptor in deiner Basis
- Blöcke, die altern oder sich verjüngen
- Zonen mit zufälliger Zeitgeschwindigkeit

Wer einen Riss schließt, bekommt Chronon, *Zukunftssplitter* und *Zeitstaub*.

---

## 8. Die Zeitmaschine

### 8.1 Das Chronos-Tor
- **Multiblock**: ein Ring aus 7×7 Chronit-Rahmen mit konzentrischen Zahnrad-Ringen. Sie drehen sich beim Aufladen schneller und gegenläufig.
- **Konsole** mit GUI. Mit Pfeiltasten ◀◀ ◀ ▶ ▶▶ wählt man die Zielzeit aus fester Stufenliste:

```
ZUKUNFT:      +1 +2 +3 +4 +5 +10 +25 +50 +100 +250 +500 +1000 Jahre
VERGANGENHEIT: −1 −2 −3 −4 −5 −10 −50 −100 −500 −1000
               −5.000 −20.000 (Eiszeit-Maximum) −100.000
               −1 Mio −10 Mio −66 Mio (Asteroid!) −70 Mio
               −100 Mio −150 Mio −200 Mio −245 Mio
```

- **Kosten** steigen logarithmisch mit dem Abstand zur Gegenwart, dazu kommt ein **Epochen-Fokus** pro Zeitalter (siehe 10.4).
- **Aufladen** dauert 10 Sekunden mit Sound-Crescendo. Danach geht es durch das Portal in den **Zeitstrom**: ein Tunnel-Flug von 5 bis 8 Sekunden, ein riesiger Jahreszähler rast runter oder hoch („Jahr −66.000.000“). Die Ladezeit des Ziels wird so versteckt.
- **Rückkehr**: Am Ziel steht automatisch ein *Heimkehr-Anker*. Wird er zerstört, musst du vor Ort ein neues Tor bauen, mit lokalen Materialien. Das ergibt ein Survival-Abenteuer.

### 8.2 Technische Umsetzung
- **Jede Zielzeit ist eine eigene Dimension**, ungefähr 32 Stück. Sie sind fest definiert und werden erst beim ersten Besuch erzeugt.
- **Zwei Spieler, die beide −5 Jahre wählen, landen in derselben Welt und treffen sich.**
- Jede Epochen-Dimension hat einen eigenen Chunk-Generator. Er benutzt den **Seed deiner Welt** und Parameter, die vom Jahr abhängen.
- Alle Zufallsentscheidungen sind **deterministisch** aus Seed, Position und Jahr. Wer zweimal ins Jahr +100 reist, sieht dieselbe Zukunft.

### 8.3 Zeitlinien-Regeln
- **Die Zukunft ist nicht festgeschrieben.** Änderst du die Gegenwart, wird die Zukunft beim nächsten Besuch neu berechnet. Ausgenommen sind Chunks, in denen du **in der Zukunft selbst gebaut hast**, die bleiben erhalten.
- **Vergangenheit → Gegenwart**: Normale Änderungen in der Vergangenheit wirken nicht auf die Gegenwart, sonst gäbe es endlose Paradoxa. Für gewollte Verbindungen gibt es zwei Ausnahmen:
  - **Zeitkapsel** (Block): Was du in der Vergangenheit einlagerst, findest du in der Gegenwart an derselben Stelle, entsprechend gealtert. Essen ist verrottet, Eisen verrostet, Diamanten bleiben Diamanten.
  - **Zeitsaat**: ein Setzling, der in der Vergangenheit gepflanzt wird, steht in der Gegenwart als uralter Riesenbaum.
- **Zeitkäfig**: Damit kann man Tiere aus anderen Epochen mitbringen, zum Beispiel einen Dino als Haustier. Per Server-Einstellung abschaltbar.

---

## 9. Zukunft

Realismus-Grundlage: In 1000 Jahren verändern sich Berge und Kontinente **kaum**. Was sich stark ändert, sind **Vegetation, Bauwerke und Zivilisation**. Genau darauf konzentriert sich die Zukunft.

### 9.1 Deine Bauwerke altern (Verwitterungs-Pipeline)
Chunks, die Spieler verändert haben, werden aus der Gegenwart kopiert und durchlaufen eine **Alterungs-Simulation**. Sie skaliert mit den Jahren:

| Material | +5 J | +50 J | +500 J | +1000 J |
|---|---|---|---|---|
| Holz (Bretter, Türen, Zäune) | leicht verblichen | Löcher | größtenteils verrottet | weg, nur Steinreste |
| Glas | ganz | einzelne kaputt | fast alle kaputt | Scherben |
| Stein/Ziegel | ganz | bemoost, rissig | stark bemoost, Teileinsturz | Ruine |
| Kupfer | angelaufen | oxidiert | voll oxidiert | voll oxidiert |
| Eisen | – | **rostig** (neue Blöcke) | stark rostig | zerfallen |
| Wolle, Teppich, Betten | staubig | zerfressen | weg | weg |
| Fackeln, Laternen | aus | aus | weg | weg |
| Felder | verwildert | Gras | Wiese | Wald |
| Truhen | – | Essen verrottet | Werkzeuge beschädigt | teilweise Staub |
| Redstone | – | defekt | weg | weg |

Dazu kommen:
- **Statik**: Blöcke ohne Halt stürzen nach einer gewissen Zeit ein. Dächer brechen ein, Brücken fallen.
- **Natur holt sich alles zurück**: Ranken, Moos, Spinnweben in Innenräumen, Laub und Wurzeln. **Bäume wachsen durch Häuser.** Sand weht in Wüsten herein, Schnee häuft sich an.
- **Tiere**: Deine eingezäunten Kühe sind tot, übrig sind Knochenhaufen. Ihre Nachkommen leben verwildert in der Nähe.
- **Schilder**: Die Schrift verblasst mit der Zeit bis zur Unlesbarkeit.
- Die Tabellen sind **datengesteuert (JSON)**. Blöcke anderer Mods bekommen automatisch passende Regeln über ihr Material und ihren Sound-Typ, Mod-Packs können sie überschreiben.

### 9.2 Die Welt um dich herum
- **Wald-Sukzession**: Wälder breiten sich in Ebenen aus, alte Bäume werden riesig, und es entstehen neue Wälder, wo vorher keine waren.
- **Flüsse**: leichte Erosion an Ufern, neue Sandbänke.
- **Klima**: Je weiter in der Zukunft, desto mehr verschiebt es sich leicht, mit Variationen bei Schnee, Wüste und Sumpf.

### 9.3 Dörfer wachsen
Ein **Dorf-Wachstums-Modell** bestimmt aus Seed, Lage und Jahr, ob ein Dorf gewachsen, gleich geblieben oder **untergegangen** ist (Ruine, Plünderer-Lager).

| Zeit | Dorf |
|---|---|
| +10 J | ein paar neue Häuser, mehr Felder |
| +100 J | Steinstraßen, Mauer, Marktplatz, Kirche |
| +500 J | Kleinstadt: Türme, Brücken, Mühlen, Hafen |
| +1000 J | **Stadt**: Glas, Kupfer, Laternen-Alleen, Uhrturm, Bibliothek, Stadtmauer, eigene Architektur |

Neue Gebäude entstehen über das Jigsaw-System von Minecraft mit eigenen Bauteil-Vorlagen pro Ära.

### 9.4 Neue Wesen der Zukunft
- **Zukunfts-Dorfbewohner** mit neuen Berufen, zum Beispiel der *Chrono-Händler*, der Zeit-Items handelt.
- **Ruinenwächter**: Golems, die verlassene Städte bewachen.
- Angepasste Tiere, zum Beispiel ein *Moosbär* in überwucherten Ruinen.
- **Zeitwächter**: Sie jagen Spieler mit hohem Paradox-Druck, eine Art Zeitpolizei.
- **Epochen-Boss +1000**: der *Wächter der letzten Stadt*.

---

## 10. Vergangenheit

Je weiter zurück, desto **wilder, gewaltiger und gefährlicher**. Ab etwa 1 Mio. Jahren hat die Welt mit der heutigen Landschaft nichts mehr zu tun, wie in echt.

### 10.1 Nahe Vergangenheit (−1 bis −1000 Jahre)
- **Ab −1 Jahr** sieht die Welt so aus, wie sie bei der Erstellung war: dein Seed, **ohne** deine Bauwerke.
- Mit jedem Schritt wird es **ursprünglicher**:
  - Urwälder mit dichterer Vegetation
  - mehr Wildtiere, dazu Wölfe und Bären in Rudeln
  - kleinere, primitivere Dörfer (Holz, Stroh, Palisaden)
  - bei −1000 Jahren Stimmung wie im Frühmittelalter: Burgruinen werden zu Burgen
- Neue Wesen: **Urzeit-Dorfbewohner** und **Wildschwein**.

### 10.2 Steinzeit und Eiszeit (−5.000 bis −100.000)
**−20.000 Jahre: Höhepunkt der letzten Eiszeit.**
- **Gletscher** bedecken den Norden.
- Der **Meeresspiegel ist niedriger** (in echt etwa 120 m). Dadurch werden Inseln zu Halbinseln, und es gibt **Landbrücken**.
- Schneestürme, Tundra und Steppe.
- Tiere: **Mammut, Säbelzahntiger, Wollnashorn, Riesenhirsch, Höhlenbär**, dazu **Höhlenmenschen-Stämme** mit Feuerstellen und Höhlenmalereien.
- Epochen-Boss: **Mammut-Patriarch**.

### 10.3 Tiefe Vergangenheit (−1 Mio bis −245 Mio)
Ab hier gilt ein eigener **Paläo-Generator**. Er wird aus dem Seed abgeleitet, erzeugt aber komplett andere Kontinente, und die Parameter folgen der Erdgeschichte:

| Epoche | Landschaft | Klima | Wesen |
|---|---|---|---|
| **−1 Mio** (Pleistozän) | Steppen, große Flüsse | kühl | Riesenfaultier, Glyptodon, **Bernstein mit Insekten** |
| **−10 Mio** (Miozän) | Savannen, Urmeere | warm | **Megalodon** im Meer, Terrorvogel, Paraceratherium |
| **−66 Mio** ☄️ (Kreidezeit-Ende) | wie −70 Mio | warm | **Asteroiden-Event**: Der Asteroid wird über mehrere Spieltage am Himmel größer. Bleibst du, kommt der **Einschlag**: Druckwelle, Feuerregen, verbrannte Welt |
| **−70 Mio** (Späte Kreide) | **Meeresspiegel viel höher → Binnenmeere**, Vulkane | heiß, feucht | **T-Rex, Triceratops, Velociraptor, Ankylosaurus, Pteranodon, Mosasaurus** |
| **−100 Mio** (Kreide) | Sümpfe, erste Blütenpflanzen | heiß | **Spinosaurus**, Iguanodon |
| **−150 Mio** (Jura) | Farnwälder, Riesen-Nadelbäume, Lagunen | warm, feucht | **Brachiosaurus, Stegosaurus, Allosaurus, Archaeopteryx, Plesiosaurus** |
| **−200 Mio** (Trias/Jura) | Riftgräben, Vulkanismus | heiß | Coelophysis, Dilophosaurus |
| **−245 Mio** (Trias) | **Pangäa**: Superkontinent, riesige Wüste im Inneren, Monsun an den Küsten, **Vulkanfelder** | sehr heiß, trocken | **Erste Dinosaurier**, Postosuchus, Ichthyosaurier, Plateosaurus-Vorfahren |

### Kleinste Details mit echtem Hintergrund
- **Kürzere Tage** in der Tiefen Vergangenheit, weil die Erde sich schneller drehte. Der Tag-Nacht-Zyklus wird kürzer.
- **Der Mond ist größer**, weil er näher war, und die **Sterne stehen anders**.
- **Pflanzen**: kein Gras vor etwa 66 Mio. Jahren, keine Blumen vor etwa 130 Mio. Jahren. Stattdessen gibt es Farne, Palmfarne, Schachtelhalme und Ginkgos.
- **Kein Vanilla-Mob** passt in die Tiefe Vergangenheit. Kühe, Zombies und Dorfbewohner werden dort nicht gespawnt.
- Eigene **Farb- und Nebelstimmung** pro Epoche: Bernstein-Ton für die Kreide, dunstig-grün für den Jura, rötlich-heiß für die Trias, blau-weiß für die Eiszeit.

### 10.4 Epochen-Freischaltung (die Entdeckungs-Schleife)
- In der Gegenwart findest du **Fossilien**: Mit dem Pinsel (Vanilla-Archäologie) kommen tiefe Gesteinsschichten zum Vorschein. Daraus wird der **Epochen-Fokus** gecraftet.
- Für die Dino-Zeitalter brauchst du **Bernstein mit Insekt** aus dem Zeitalter −1 Mio. Daraus gewinnst du **DNS** → *Fokus: Mesozoikum*.
- So wird jede neue Epoche ein Abenteuer, das auf der vorherigen aufbaut.
- Epochen-Boss in der Kreide: **Tyrannus Rex Alpha**.

---

## 11. Mehrspieler

- **Im Zeitstopp** sind andere Spieler eingefroren. Sie sehen ein graues, eingefrorenes Bild und können sich nicht bewegen. Wer die volle Chrono-Rüstung trägt, kann sich trotzdem bewegen, genau wie in JoJo. Das ergibt **Zeitstopp-Duelle**.
- **Schaden im Zeitstopp** wird gesammelt und **auf einmal** ausgeteilt, sobald die Zeit weiterläuft.
- **Rückspulen** betrifft andere Spieler nur, wenn die Server-Einstellung das erlaubt. Mit dem *Zeitanker-Amulett* ist man geschützt.
- **Zeitreisen**: Spieler im selben Zieljahr landen in derselben Welt (siehe 8.2).
- Für Server mit vielen Spielern gibt es Begrenzungen für Radius, Faktor und Häufigkeit, dazu Schutzzonen ohne Zeitmanipulation, etwa am Spawn.

---

## 12. Optik und Sound

### 12.1 Effekte (Post-Processing-Shader, Vanilla-Technik, ohne Zusatz-Mod)
| Effekt | Aussehen |
|---|---|
| **Zeitstopp** | Eine Schockwelle breitet sich kugelförmig aus und **invertiert die Farben**. Danach ist alles **graustufig**, mit leichtem Farbstich und tickendem Ziffernblatt am Bildschirmrand |
| **Zeit läuft weiter** | Die Welle fällt rückwärts zusammen, und die Farben kommen zurück |
| **Vorspulen** | Bewegungsschlieren und Nachbilder an Entities, leichte chromatische Verschiebung am Rand |
| **Zeitlupe** | kühler Blauton, Tiefenunschärfe, Partikel in Zeitlupe |
| **Rückspulen** | VHS- oder Sepia-Look mit Scanlines und Bildrauschen. Partikel fliegen rückwärts, denn der Client zeichnet sie auf und spielt sie rückwärts ab |
| **Zeitmaschine** | drehende Ringe, Spiralpartikel, Lichtbögen, Tunnelflug mit Jahreszähler |

### 12.2 Items
- **Animierte Texturen**: drehende Zeiger und Zahnräder, rieselnder Sand.
- **Leuchtende Teile** (Emissive-Texturen).
- **Eigene 3D-Modelle**: Die Taschenuhr öffnet ihren Deckel, die Chronos-Uhr hat schwebende Ringe.
- Die **Zeiger zeigen echte Werte**: aktuellen Modus, Faktor und Restladung.

### 12.3 Sounds
- Im Stil von JoJo, aber **komplett selbst erzeugt** (Synthese mit Python und ffmpeg). JoJo-Audio darf ich aus Urheberrechtsgründen nicht verwenden.
- **Zeitstopp**: tiefer Bass-Schlag, rückwärts laufender Hall, Glocke, dann Stille mit Herzschlag und Uhr-Ticken.
- **Weiterlaufen**: Uhrwerk-Klick, dann kommt die Welt mit einem Rauschen zurück.
- **Rückspulen**: Bandsalat-Scratchen und rückwärts gespielte Effekte.
- **Vorspulen**: Rauschen, hochgepitchte Weltgeräusche.
- Außerdem: Aufziehen, Ticken, Portal-Aufladen, Zeitstrom-Ambiente, Dino-Brüllen, Mammut-Trompeten.
- **Weltgeräusche reagieren auf die Zeit**: Im Stopp ist alles stumm, bei Vorspulen höher und schneller, in Zeitlupe tief und langsam. Die eingebaute Grenze von 0,5 bis 2,0 für die Tonhöhe wird dafür aufgehoben.
- **Musik** pro Epoche ist optional.
- Optional können CC0-lizenzierte Sounds (z. B. von freesound.org) eingebunden werden, mit Lizenznachweis.

---

## 13. Kompatibilität

### 13.1 Alle Mods (automatisch)
- Die Zeit-Engine greift auf **Tick-Ebene** ein, nicht pro Mob- oder Maschinentyp. Mod-Entities, Mod-Maschinen und Mod-Pflanzen werden automatisch beschleunigt, verlangsamt, gestoppt und zurückgespult.
- Rückspulen speichert **NBT**. Auch Energie, Fortschritt und Inventare von Mod-Maschinen kommen zurück.
- **Öffentliche API**: Andere Mods können eigene Zustände registrieren, etwa globale Netzwerke, oder Zeitmanipulation erkennen und abfangen.

### 13.2 Weitere (optional, nur aktiv wenn installiert)
- **Jade / WTHIT**: zeigt beim Anschauen den Zeitzustand eines Blocks oder Mobs („2,0× · noch 12 s“).
- **EMI / REI**: Rezepte, auch für die Uhrmacher- und die Temporal-Werkbank.
- **Mod Menu und Cloth Config**: Einstellungen im Spiel.
- **Sodium / Iris**: Kompatibilität der Shader-Effekte wird geprüft.
- **GeckoLib**: animierte Dino- und Boss-Modelle.

---

## 14. Konfiguration, Befehle, Barrierefreiheit

### Server-Einstellungen
- Maximalwerte für Faktoren, Radien und die Rückspul-Länge (Standard 60 s)
- Zeitmotor-Budget (MSPT)
- ob andere Spieler betroffen sein dürfen
- Schutzzonen ohne Zeitmanipulation
- ob Zeitkäfige erlaubt sind
- Immunitätslisten
- Chronon-Kosten-Multiplikator

### Client-Einstellungen
- Intensität der Effekte
- **Blitze und Farb-Invertierung abschwächen** (Fotosensibilität)
- Lautstärke der Zeit-Sounds
- HUD-Position

### Admin-Befehle
```
/chrono zone <pos> <radius> <faktor> [filter]
/chrono stop <sekunden>
/chrono rewind <sekunden>
/chrono travel <spieler> <jahr>
/chrono chronon <spieler> <add|set> <wert>
/chrono debug
```
`/chrono debug` blendet Zonen, Journal-Größe und Kosten ein.

---

## 15. Technische Architektur

```
timemod/
├─ api/            öffentliche Schnittstelle für andere Mods
├─ core/           TimeController, TimeZone, Akkumulator, Prioritäten, Budget
│  └─ mixin/       ServerLevel-Tick, Entity-Tick, BlockEntity-Ticker,
│                  Random-/Scheduled-Ticks, Fluids, Weather, DayTime
├─ rewind/         ChronoJournal (Ringpuffer), Recorder, Playback, Anti-Dupe
├─ energy/         Chronon, Zellen, Kostenformeln
├─ content/        Items, Blöcke, Block-Entities, Menüs, Rezepte
├─ entity/         Bosse, Epochen-Wesen, Zeitriss-Anomalien
├─ world/          Erze, Strukturen, Zeitrisse
├─ travel/
│  ├─ era/         Epochen-Definitionen (Jahr → Parameter)
│  ├─ gen/         Zukunfts- und Paläo-Chunk-Generatoren
│  ├─ aging/       Verwitterungs-Pipeline (datengesteuert)
│  ├─ village/     Dorf-Wachstum
│  └─ portal/      Multiblock, Konsole, Zeitstrom, Heimkehr-Anker
├─ client/         Interpolations-Skalierung, Shader, HUD, Radialmenü,
│                  Sound-Pitch, Partikel-Rückwärts, Item-Renderer
└─ compat/         projecte, jade, emi, rei, modmenu
```

- **Mappings**: offizielle Mojang-Mappings über Loom.
- **Datengesteuert**: Epochen, Verwitterung, Dorfbau-Teile, Kosten und Immunitäten liegen als JSON vor und sind per Datapack anpassbar.
- **Netzwerk**: Zonen-Sync, Stopp-/Rückspul-Status und HUD-Daten werden nur an Spieler in Reichweite geschickt.
- **Assets** entstehen über eigene Python-Skripte im Repo (`tools/`): Pixel-Art, Animationen, Sounds und Struktur-NBT. Sie sind reproduzierbar und lassen sich leicht nachbessern.

---

## 16. Tests

**„Akkurat“ wird nicht behauptet, sondern bewiesen.** Dafür gibt es automatische Tests:

| Test | Was er beweist |
|---|---|
| **Video-Äquivalenz** | Szene A läuft 200 Ticks bei 1×, Szene B 100 Ticks bei 2×. Danach sind alle Zustände **identisch**: Lore auf Schiene, fallender Sand, Wasserfluss, Redstone-Uhr, Ofen-Fortschritt, Pflanzenwachstum mit festem Zufall, Mob-Bewegung mit festem Zufall |
| **Zeitlupe** | 0,5× für 200 Ticks entspricht 1× für 100 Ticks |
| **Stopp** | Nach N Ticks Stopp hat sich **nichts** verändert, der Hash der Welt bleibt gleich |
| **Rückspulen** | Welt speichern, 60 s mit Bauen, Abbauen, Töten, Wachstum, Explosionen, Wasser und Feuer. Danach zurückspulen: Der Hash der Welt ist **identisch** mit dem Ausgangszustand |
| **Anti-Dupe** | Endertruhe, Dimensionswechsel, Trichter, Item-Rahmen, Spieler-Handel: kein Item mehr als vorher |
| **Grenzfälle** | Entity verlässt oder betritt die Zone, Chunk entlädt sich, Zonen überlappen |
| **Zeitreise** | Gleiches Jahr mit gleichem Seed ergibt dieselben Chunks. Verwitterung ist deterministisch. Zwei Spieler landen in derselben Dimension |
| **Leistung** | MSPT-Messung bei globalem 5× mit 500 Entities |
| **Server-Start** | Ein headless Dedicated Server startet mit der Mod, das Log wird auf Fehler und Warnungen geprüft |
| **Client-Screenshots** | Versuch mit virtuellem Bildschirm (Xvfb) und Software-Rendering. Damit bekomme ich hoffentlich echte Screenshots von Items, Effekten und Epochen |

Dazu kommen **JUnit-Tests** für die reine Logik: Ringpuffer, Kostenformeln, Jahres-Stufen und Hash-Determinismus.

---

## 17. Meilensteine

Jeder Meilenstein wird kompiliert, getestet und als `.jar` lauffähig gepusht.

| # | Meilenstein | Inhalt | Ergebnis zum Anspielen |
|---|---|---|---|
| **M0** | Fundament | Gradle/Loom, Mod-Grundgerüst, Test-Infrastruktur (Gametests, Server-Boot), Asset-Skripte | Mod lädt |
| **M1** | Zeit-Engine | Zonen, 0,05–10×, Akkumulator, alle Tick-Systeme, Client-Glättung, Sound-Pitch, Video-Äquivalenz-Tests | Debug-Item: Zone vorspulen und verlangsamen |
| **M2** | Zeitstopp deluxe | Stopp, Shader-Welle, Graustufen, Sounds, Wurfmesser, Schaden sammeln, Immunität | „ZA WARUDO“-Moment 🔥 |
| **M3** | Rückspulen | Zeitjournal, Playback, Anti-Dupe, Welt-Hash-Tests, VHS-Effekt, Rückwärts-Partikel | 60 s rückspulen |
| **M4** | Stufe I + II | Messing, Uhrmacher-Werkbank, Chronit (flackerndes Erz), Zeitsand, alle Stufe-I/II-Items, Chronon, Zellen, Kondensator, Projektor | spielbarer Fortschritt im frühen und mittleren Spiel |
| **M5** | Stufe III + Bosse 1–2 | Temporal-Werkbank, Module, Filter, Chrono-Rüstung, Extraktor, Strukturen, Uhrmacher, Chronophage, Handbuch, Advancements | spätes Spiel |
| **M6** | Stufe IV | Boss 3, Herz der Zeit, Chronos-Uhr, Radialmenü, HUD-Zeitleiste, Scrubbing, globale Kontrolle, Meisterschaft, Schleife | volle Zeitkontrolle |
| **M7** | Zeitmaschine | Chronos-Tor, Konsole mit Jahresauswahl, Zeitstrom-Flug, ca. 32 Epochen-Dimensionen, Heimkehr-Anker, Zeitkapsel, Zeitsaat | Zeitreisen funktionieren |
| **M8** | Zukunft | Verwitterungs-Pipeline, Statik, Natur-Rückeroberung, Wald-Sukzession, Dorf-Wachstum, Zukunfts-Wesen, Epochen-Boss | +1 bis +1000 Jahre |
| **M9** | Nahe Vergangenheit + Eiszeit | ursprüngliche Welt, Gletscher, Meeresspiegel, Megafauna, Höhlenmenschen | bis −100.000 Jahre |
| **M10** | Tiefe Vergangenheit | Paläo-Generator, Pangäa, Binnenmeere, Urpflanzen, alle Dinos, Asteroiden-Event, Fossilien, Bernstein/DNS, Epochen-Bosse | bis −245 Mio. Jahre 🦖 |
| **M11** | Kompatibilität + Feinschliff | Jade, EMI/REI, Mod Menu, API, Balancing, Leistung, Barrierefreiheit, Übersetzungen (de/en) | Release 1.0 |

---

## 18. Ehrliche Grenzen

- **„Absolut perfekt bis ins kleinste Detail“** geht für eine Simulation von 245 Mio. Jahren nicht wörtlich. Die Epochen werden **prozedural und stilisiert**, mit echtem wissenschaftlichem Hintergrund (Meeresspiegel, Pangäa, Tageslänge, Pflanzenwelt). Es ist aber keine Geologie-Simulation.
- **Andere Mods** werden fast immer automatisch erfasst. Ausnahmen sind Mods, die eigene Uhren auf Basis der echten Zeit benutzen oder Zustände außerhalb der Welt speichern, etwa globale Netzwerke. Für solche Fälle gibt es die API.
- **Globales Vorspulen** ist durch die Server-Leistung begrenzt, das lässt sich physikalisch nicht umgehen.
- **Grafik und Sound** erzeuge ich selbst per Code. Das wird stimmig und gut, aber nicht so gut wie von professionellen Pixel-Artists und Sound-Designern. Später austauschen ist jederzeit möglich, weil alle Assets an einer Stelle liegen.
- **Das Spielgefühl im Client** kann ich nur begrenzt testen. Server-Logik und Determinismus prüfe ich automatisch, Screenshots versuche ich über Xvfb. Wie es sich *anfühlt*, musst du am Ende anspielen.
- Das ist eine **große Mod mit zehntausenden Zeilen Code**. Sie entsteht über viele Sitzungen hinweg, Meilenstein für Meilenstein.

---

## 19. Offene Fragen

1. **GeckoLib** als Abhängigkeit für animierte Dinos und Bosse: okay?
2. Spielst du hauptsächlich **allein oder auf einem Server**? Das bestimmt die Standard-Grenzwerte.

### Benötigte Domains (Netzwerk-Freigabe)
```
maven.fabricmc.net        meta.fabricmc.net
piston-meta.mojang.com    piston-data.mojang.com
launchermeta.mojang.com   libraries.minecraft.net
resources.download.minecraft.net
api.modrinth.com          (Modrinth-Maven: Jade, EMI, Mod Menu …)
maven.terraformersmc.com  (Mod Menu, EMI)
maven.shedaniel.me        (Cloth Config, REI)
dl.cloudsmith.io          (GeckoLib)
```
