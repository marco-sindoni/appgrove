import preset from '@appgrove/design-system/preset'

/** @type {import('tailwindcss').Config} */
export default {
  presets: [preset],
  content: [
    './index.html',
    './src/**/*.{ts,tsx}',
    // includi i primitivi del design-system così le loro classi non vengono purgate
    './node_modules/@appgrove/design-system/dist/**/*.js',
  ],
}
