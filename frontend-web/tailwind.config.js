/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: {
          DEFAULT: '#0B0F17',
          surface: '#0F1420',
          elevated: '#151B2B',
          card: '#161F33',
          hover: '#1D2740',
        },
        border: {
          subtle: '#1C2538',
          DEFAULT: '#222B40',
          strong: '#2E3A54',
          active: '#3F4E70',
        },
        risk: {
          low: '#10B981',        // Emerald
          'low-bg': 'rgba(16, 185, 129, 0.12)',
          'low-border': 'rgba(16, 185, 129, 0.25)',
          medium: '#F59E0B',     // Amber
          'medium-bg': 'rgba(245, 158, 11, 0.12)',
          'medium-border': 'rgba(245, 158, 11, 0.25)',
          high: '#EF4444',       // Rose / Red
          'high-bg': 'rgba(239, 68, 68, 0.12)',
          'high-border': 'rgba(239, 68, 68, 0.25)',
          unknown: '#94A3B8',
          'unknown-bg': 'rgba(148, 163, 184, 0.12)',
        },
        brand: {
          50: '#EEF2FF',
          100: '#E0E7FF',
          400: '#818CF8',
          500: '#6366F1',
          600: '#4F46E5',
          700: '#4338CA',
        },
        accent: {
          cyan: '#06B6D4',
          teal: '#14B8A6',
          purple: '#A855F7',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
    },
  },
  plugins: [],
}
