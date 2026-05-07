const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Parameter einlesen
const benchmarkClassName = process.argv[2]; // Erster Parameter: benchmark
const targetDir = process.argv[3]; // Zweiter Parameter: targetDir

// Überprüfen, ob beide Parameter übergeben wurden
if (!benchmarkClassName || !targetDir) {
    console.error('Bitte geben Sie zwei Parameter an: benchmark und targetDir.');
    process.exit(1);
}

// Variablen
const BASE_DIR = process.cwd();
const LIBRARY = 'benchmark';
const BUILD_VARIANT_NAME = 'Benchmark';
const BENCHMARK_PACKAGE_NAME = 'com.example.benchmark';
//const DEVICE = 'Pixel_3a_API_33(AVD) - 13';
const DEVICE = 'Pixel_7a_API_33(AVD) - 13';

// Convert benchmarkClassName to lowerCamelCase for main_test_method
const mainTestMethod = benchmarkClassName.charAt(0).toLowerCase() + benchmarkClassName.slice(1);

// Befehl zusammensetzen
const command = `"${path.join(BASE_DIR, 'gradlew')}" "${LIBRARY}:connected${BUILD_VARIANT_NAME}AndroidTest" \
-P android.testInstrumentationRunnerArguments.class="${BENCHMARK_PACKAGE_NAME}.${benchmarkClassName}#${mainTestMethod}"`;

console.log('Führe folgenden Befehl aus:');
console.log(command);

try {
    // Befehl ausführen und auf Abschluss warten
    execSync(command, { stdio: 'inherit' });

    // Wenn der Befehl erfolgreich war, Datei verschieben
    console.log('Befehl erfolgreich ausgeführt. Verschiebe die Datei...');

    // Pfad zur Datei, die verschoben werden soll
    const sourceFile = path.join(BASE_DIR, 'benchmark', 'build', 'outputs', 'connected_android_test_additional_output', 'benchmark', 'connected', DEVICE, 'com.example.benchmark-benchmarkData.json');

    // Zielpfad für die verschobene Datei
    const targetFile = path.join(targetDir, 'com.example.benchmark-benchmarkData.json');

    // Überprüfen, ob die Quelldatei existiert
    if (fs.existsSync(sourceFile)) {
        // Datei verschieben
        fs.renameSync(sourceFile, targetFile);
        console.log(`Datei erfolgreich verschoben nach: ${targetFile}`);
    } else {
        console.error('Quelldatei existiert nicht:', sourceFile);
        process.exit(1);
    }
} catch (error) {
    console.error('Fehler beim Ausführen des Befehls oder beim Verschieben der Datei:', error.message);
    process.exit(1);
}
