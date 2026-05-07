const {
    getAverageCpuUsage,
    getAverageFPSUsage,
    getAverageRAMUsage,
} = require("@perf-profiler/reporter");
const fs = require("fs");
const path = require("path");

// Parameter einlesen
const targetDir = process.argv[2];
if (!targetDir) {
    console.error("Bitte das targetDir als Parameter übergeben.");
    process.exit(1);
}

const currentDir = process.cwd();
console.log("Aktuelles Arbeitsverzeichnis:", currentDir);

// Pfad zur results.json
const resultsPath = path.join(currentDir, targetDir, "results.json");

if (!fs.existsSync(resultsPath)) {
    console.error(`Datei ${resultsPath} nicht gefunden.`);
    process.exit(1);
}

// Ergebnisse laden
const results = require(resultsPath);

// CPU, FPS und RAM für jede Iteration berechnen
const cpuPerTestIteration = results.iterations.map((iteration) =>
    getAverageCpuUsage(iteration.measures)
);

const fpsPerTestIteration = results.iterations.map((iteration) =>
    getAverageFPSUsage(iteration.measures)
);

const ramPerTestIteration = results.iterations.map((iteration) =>
    getAverageRAMUsage(iteration.measures)
);

// Durchschnittswerte berechnen
const totalAvg = (values) => {
    const sum = values.reduce((acc, val) => acc + val, 0);
    return sum / values.length;
};

const totalAvgCpuInPercent = totalAvg(cpuPerTestIteration);
const totalAvgFps = totalAvg(fpsPerTestIteration);
const totalAvgRamInMb = totalAvg(ramPerTestIteration);

// Ergebnisse speichern
const output = {
    totalAvgCpuInPercent,
    totalAvgFps,
    totalAvgRamInMb,
};

const outputPath = path.join(targetDir, "processed_results.json");
fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));

console.log(`Ergebnisse wurden in ${outputPath} gespeichert.`);
