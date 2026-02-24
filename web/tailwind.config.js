/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#FF9800",
        secondary: "#FFC107",
        background: "#0F0F0F",
        surface: "#1A1A1A",
        card: "#242424",
      },
    },
  },
  plugins: [],
}
