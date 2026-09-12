#!/usr/bin/env node
// Regenerates docs/ANIMATION_MANIFEST.json from the actual .animation.json assets.
// This is derived, reproducible data (same spirit as src/generated/resources): don't hand-edit the
// output, rerun this script instead (`node tools/gen_animation_manifest.js`) after adding/changing
// a species' animation file.
"use strict";

const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");
const animDir = path.join(root, "src/main/resources/assets/mhnw/animations/entity");
const outFile = path.join(root, "docs/ANIMATION_MANIFEST.json");

const manifest = {};

for (const file of fs.readdirSync(animDir).sort()) {
  if (!file.endsWith(".animation.json")) continue;
  const species = file.replace(".animation.json", "");
  const data = JSON.parse(fs.readFileSync(path.join(animDir, file), "utf8"));
  const clips = {};
  for (const [key, anim] of Object.entries(data.animations || {})) {
    const prefix = `animation.${species}.`;
    const name = key.startsWith(prefix) ? key.slice(prefix.length) : key;
    const lengthSeconds = anim.animation_length ?? null;
    clips[name] = {
      lengthSeconds,
      lengthTicks: lengthSeconds != null ? Math.round(lengthSeconds * 20) : null,
      loop: anim.loop ?? false,
    };
  }
  manifest[species] = clips;
}

fs.writeFileSync(outFile, JSON.stringify(manifest, null, 2) + "\n");
console.log(`Wrote ${outFile} (${Object.keys(manifest).length} species).`);
