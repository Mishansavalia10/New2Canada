// Builds the same design tokens the old CDN script used, reading the project's own config file.
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const STATIC = path.join(__dirname, '..', 'src', 'main', 'resources', 'static');
const source = fs.readFileSync(path.join(STATIC, 'js', 'tailwind-config.js'), 'utf8');
const sandbox = { tailwind: {} };
vm.runInNewContext(source, sandbox);

module.exports = {
  ...sandbox.tailwind.config,
  content: [path.join(STATIC, '**', '*.html'), path.join(STATIC, 'js', '**', '*.js')],
  plugins: [require('@tailwindcss/forms'), require('@tailwindcss/container-queries')],
};
