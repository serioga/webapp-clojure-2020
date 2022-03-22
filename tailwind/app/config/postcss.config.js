module.exports = {
  plugins: {
    'postcss-import': {path: ['tailwind', 'node_modules']},
    'tailwindcss/nesting': {},
    'tailwindcss': 'tailwind/app/config/tailwind.config.js',
    'autoprefixer': {},
    'cssnano': {preset: 'default'},
    'postcss-reporter': {},
  }
};
