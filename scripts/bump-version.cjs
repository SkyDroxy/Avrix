#!/usr/bin/env node
const fs = require('fs');
const path = require('path');

function log(...args) { console.log('[avrix-bump-version]', ...args); }

const rawArg = (process.argv[2] || process.env.VERSION || '').trim();
if (!rawArg) {
  console.error('Usage: node scripts/bump-version.cjs <version>');
  process.exit(1);
}
const version = rawArg.replace(/^v/i, '');
const semverRe = /^(\d+)\.(\d+)\.(\d+)(?:[-+][0-9A-Za-z-.]+)?$/;
if (!semverRe.test(version)) {
  console.error(`Invalid version: ${rawArg}`);
  process.exit(1);
}

const root = process.cwd();
const gradlePath = path.join(root, 'build.gradle');
let gradle = fs.readFileSync(gradlePath, 'utf8');

const before = gradle;
gradle = gradle.replace(/(else\s*\{\s*\n\s*version\s*=\s*')[^']+('\s*\n\s*\})/, `$1${version}$2`);

if (gradle === before) {
  console.error('Did not find fallback version assignment to update.');
  process.exit(1);
}

fs.writeFileSync(gradlePath, gradle);
log('Updated build.gradle fallback version ->', version);
log('Done.');
