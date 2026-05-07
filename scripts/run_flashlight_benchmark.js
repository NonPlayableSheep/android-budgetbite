const { execSync } = require('child_process');
require('dotenv').config();
const fs = require('fs');
const path = require('path');

// Überprüfen, ob beide Parameter übergeben wurden
const flashlightScript = process.argv[2];
const targetDir = process.argv[3];

if (!flashlightScript || !targetDir) {
    console.error('Bitte geben Sie zwei Parameter an: flashlightScript und targetDir.');
    process.exit(1);
}

// Parameter
const bundleId = 'de.fhe.budget_bite';
const hostIp = process.env.HOST_IP;
const testScript = `./scripts/helper_scripts/${flashlightScript}.yaml`;
const projectDir = process.cwd();
const iterations = 30;

// Pfad zur results.json im targetDir
const resultsFilePath = path.join(targetDir, 'results.json');

// Nur erstellen, wenn die Datei nicht existiert
if (!fs.existsSync(resultsFilePath)) {
    fs.writeFileSync(resultsFilePath, '{}');
}

// Ausf�hren des flashlight-Tests
console.log('F�hre flashlight Test aus...');
try {
    execSync(`flashlight test --bundleId ${bundleId} --testCommand "maestro --host ${hostIp} test ${testScript}" --resultsFilePath ${path.join(projectDir, resultsFilePath)} --iterationCount ${iterations}`, { stdio: 'inherit' });

    // �berpr�fen, ob der flashlight-Test erfolgreich war und Node-Skript ausf�hren
    console.log('Flashlight Test abgeschlossen. Starte das Node.js-Skript...');
    execSync(`node ./scripts/helper_scripts/process_flashlight_results.js ${targetDir}`, { stdio: 'inherit' });
} catch (error) {
    console.error('Fehler beim Ausf�hren des flashlight-Tests:', error.message);
    process.exit(1);
}
