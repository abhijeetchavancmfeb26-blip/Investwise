/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.jsx'],
  theme: {
    // Only one extension: the brand colour. Everything else uses Tailwind's
    // defaults, which removes a custom palette, custom shadows and keyframes.
    extend: {
      colors: { brand: '#0f766e' },
    },
  },
  plugins: [],
};
