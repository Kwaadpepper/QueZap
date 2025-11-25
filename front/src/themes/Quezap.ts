import { updatePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

const Quezap = updatePreset(
  Aura,
  {
    primitive: {
      deepblue: {
        50: '#e6f0ff',
        100: '#ccdfff',
        200: '#99c0ff',
        300: '#66a0ff',
        400: '#3380ff',
        500: '#0060ff',
        600: '#004ecc',
        700: '#003c99',
        800: '#031646', // Pour les inputs/surfaces plus claires
        900: '#021033', // Pour les cartes/contenu
        950: '#010A20', // Fond de page
      },

      sapphire: {
        50: '#eef1ff',
        100: '#e0e7ff',
        200: '#c7d2fe',
        300: '#a5b4fc',
        400: '#818cf8', // Éclairci (utilisé en mode sombre)
        500: '#6366f1',
        600: '#4f46e5',
        700: '#3730a3',
        800: '#2A3278', // Notre Bleu Principal (mode clair)
        900: '#1e3a8a',
        950: '#172554',
      },
      // Vert du logo (brand-green / Success)
      green: {
        50: '#f0fdf4',
        100: '#dcfce7',
        200: '#bbf7d0',
        300: '#86efac',
        400: '#4ade80', // Éclairci (utilisé en mode sombre)
        500: '#22c55e',
        600: '#16a34a',
        700: '#15803d',
        800: '#36712D', // AJUSTÉ (plus foncé) pour WCAG Mode Clair
        900: '#0f3f21',
        950: '#052e16',
      },
      // Jaune-Orange du logo (brand-yellow / Warn)
      amber: {
        50: '#fffbeb',
        100: '#fef3c7',
        200: '#fde68a',
        300: '#fcd34d',
        400: '#fbbf24', // Éclairci (utilisé en mode sombre)
        500: '#F9B43A', // Notre Jaune-Orange Principal (mode clair)
        600: '#d97706',
        700: '#b45309',
        800: '#92400e',
        900: '#78350f',
        950: '#451a03',
      },
      // Rouge (Danger)
      red: {
        50: '#fef2f2',
        100: '#fee2e2',
        200: '#fecaca',
        300: '#fca5a5', // Éclairci (utilisé en mode sombre)
        400: '#f87171',
        500: '#ef4444',
        600: '#c02a3a', // AJUSTÉ (plus foncé) pour WCAG Mode Clair
        700: '#b91c1c',
        800: '#991b1b',
        900: '#7f1d1d',
        950: '#450a0a',
      },
    },
    semantic: {
      primary: {
        50: '#f0fdf4',
        100: '#dcfce7',
        200: '#bbf7d0',
        300: '#86efac',
        400: '#22c55e',
        500: '#15803d',
        600: '#14782f',
        700: '#166534',
        800: '#14532d',
        900: '#0f3f21',
        950: '#052e16',
      },
      colorScheme: {
        light: {
          surface: {
            0: '#ffffff',
            50: '#fafaf9',
            100: '#f5f5f4',
            200: '#e7e5e4',
            300: '#d6d3d1',
            400: '#a8a29e',
            500: '#78716c',
            600: '#57534e',
            700: '#44403c',
            800: '#292524',
            900: '#1c1917',
            950: '#0c0a09',
          },
        },
        dark: {
          surface: {
            0: '#ffffff',
            50: '#fafaf9',
            100: '#f5f5f4',
            200: '#e7e5e4',
            300: '#d6d3d1',
            400: '#a8a29e',
            500: '#78716c',
            600: '#57534e',
            700: '#44403c',
            800: '#292524',
            900: '#1c1917',
            950: '#0c0a09',
          },
          primary: {
            color: '{primary.500}',
            contrastColor: '{surface.100}',
            hoverColor: '{primary.600}',
            activeColor: '{primary.700}',
          },
          content: {
            background: '{surface.800}',
            hoverBackground: '{surface.700}',
            borderColor: '{surface.700}',
            color: '{text.color}',
            hoverColor: '{text.hover.color}',
          },
          overlay: {
            select: {
              background: '{surface.800}',
              borderColor: '{surface.700}',
              color: '{text.color}',
            },
            popover: {
              background: '{surface.800}',
              borderColor: '{surface.700}',
              color: '{text.color}',
            },
            modal: {
              background: '{surface.900}',
              borderColor: '{surface.700}',
              color: '{text.color}',
            },
          },
        },
      },
    },
  })

console.log(Quezap)

export default Quezap
