module.exports = {
  plugins: {
    'postcss-import': {path: ['tailwind', 'node_modules']},
    'tailwindcss/nesting': {},
    'tailwindcss': 'tailwind/app/config/tailwind.config.js',
    'postcss-nested': {},
    'postcss-reporter': {},
  }
};
