module.exports = {
  plugins: [
    require('postcss-import')({path: ['tailwind', 'node_modules']}),
    require('tailwindcss')('tailwind/app/config/tailwind.config.js'),
    require('postcss-nested'),
    require('postcss-reporter'),
  ]
};
