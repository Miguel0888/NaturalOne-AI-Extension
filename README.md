# Natural Code Insight Bridge – Maven/Tycho Build

Dieses Repo ist die Tycho-Version des Eclipse-Plugins **de.bund.zrb.natural.codeinsightbridge**.

## Projektstruktur

- `releng/target-platform` – Target-Definition (Eclipse + NaturalONE Abhängigkeiten)
- `de.bund.zrb.natural.codeinsightbridge` – Eclipse-Plugin (OSGi Bundle)
- `de.bund.zrb.natural.codeinsightbridge.feature` – Feature zum Installieren
- `de.bund.zrb.natural.codeinsightbridge.repository` – p2 Update Site

## 1) Target Platform anpassen (wichtig)

In `releng/target-platform/natural-code-insight-bridge.target` musst du **dein** p2-Repo für NaturalONE eintragen:

```xml
<repository location="file:/CHANGE/ME/TO/NATURALONE/P2/REPO"/>
```

Dieses Repo muss die Bundles enthalten:
- `com.softwareag.naturalone.natural.natstyle` (>= 8.4.1)
- `com.softwareag.naturalone.natural.parser` (>= 8.4.1)

Die Eclipse-Basis kommt standardmäßig aus `https://download.eclipse.org/releases/latest`.
Wenn du eine feste Eclipse-Version willst, ändere das entsprechend.

## 2) Build

Im Repo-Root:

```bash
mvn clean verify
```

Ergebnis: `de.bund.zrb.natural.codeinsightbridge.repository/target/repository`

## 3) Installation in Eclipse / NaturalONE

In Eclipse/NaturalONE:
- **Help → Install New Software...**
- **Add... → Local...**
- Ordner auswählen: `de.bund.zrb.natural.codeinsightbridge.repository/target/repository`

## 4) Entwicklung in IntelliJ

1. Repo als Maven-Projekt öffnen (Root `pom.xml`).
2. JDK 8 konfigurieren.
3. Code ändern und per `mvn clean verify` bauen.

Hinweis: Das Starten einer Eclipse-Runtime direkt aus IntelliJ ist möglich, aber am stabilsten ist:
- Build über Maven/Tycho
- Installieren/Debuggen im Eclipse/NaturalONE IDE-Produkt

