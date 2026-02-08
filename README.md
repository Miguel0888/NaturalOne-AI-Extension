# Natural Code Insight Bridge – Maven/Tycho Build

Dieses Repository enthält das Eclipse-Plugin **de.bund.zrb.natural.codeinsightbridge** inkl. Feature und p2-Update-Site – gebaut mit **Maven/Tycho**.

## Projektstruktur

* `releng/target-platform` – Target-Definition (Eclipse Plattform)
* `de.bund.zrb.natural.codeinsightbridge` – Eclipse-Plugin (OSGi Bundle)
* `de.bund.zrb.natural.codeinsightbridge.feature` – Feature zum Installieren
* `de.bund.zrb.natural.codeinsightbridge.repository` – p2 Update Site

## Voraussetzungen

* **JDK 8**
* **Maven 3.x**
* (Optional) Eclipse IDE / NaturalONE zum Installieren/Testen

## 1) Target Platform

Die Eclipse-Basis kommt standardmäßig aus `https://download.eclipse.org/releases/latest`.

Wenn du eine feste Eclipse-Version willst, passe diese Datei an:

`releng/target-platform/natural-code-insight-bridge.target`

## 2) Build

Im Repo-Root:

```bash
mvn clean verify
```

Ergebnis (p2 Repository):

* `de.bund.zrb.natural.codeinsightbridge.repository/target/repository`

## 3) Installation in Eclipse (Erstinstallation)

In Eclipse:

* **Help → Install New Software...**
* **Add... → Local...**
* Ordner auswählen:
  `de.bund.zrb.natural.codeinsightbridge.repository/target/repository`

## 4) Updates in Eclipse ohne Deinstallieren (p2 “Check for Updates”)

Damit Eclipse Updates findet, muss sich die **Bundle-/Feature-Version** erhöhen. Praktisch ist es, nur den **Qualifier** automatisiert zu ändern (Timestamp), z. B. `1.0.0.v20260208153045`.

### Version automatisch erhöhen (Qualifier bump)

Im Repo-Root liegen Skripte, die in allen `MANIFEST.MF` (`Bundle-Version`) und `feature.xml` (Feature-Version) den Qualifier auf einen Timestamp setzen:

* `bump_versions.sh` (Linux/macOS/Git Bash)
* `bump_versions.cmd` (Windows CMD, nutzt intern PowerShell)
* `bump_versions.ps1` (Windows PowerShell)
* `bump_versions.py` (cross-platform)

Beispiele:

**PowerShell**

```powershell
powershell -ExecutionPolicy Bypass -File .\bump_versions.ps1
```

**CMD**

```bat
bump_versions.cmd
```

**Bash**

```bash
./bump_versions.sh
```

**Python**

```bash
python bump_versions.py
```

### Update-Workflow (empfohlen)

1. **Qualifier bump** (eins der Skripte ausführen)
2. **Build**:

   ```bash
   mvn clean verify
   ```
3. In Eclipse:

    * **Help → Check for Updates**

> Hinweis: p2 cached teilweise aggressiv. Wenn Eclipse das Update nicht sieht, hilft oft ein Neustart der IDE.

## 5) Entwicklung in IntelliJ

1. Repo als Maven-Projekt öffnen (Root `pom.xml`).
2. **JDK 8** konfigurieren.
3. Änderungen machen, dann:

   ```bash
   mvn clean verify
   ```
