import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Único lugar donde se define la paleta de la aplicación.
 * Primario: azul profundo; superficies: grises neutros (slate).
 */
export const TemaSeleccion = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eef4fb',
      100: '#d5e3f5',
      200: '#abc7eb',
      300: '#7ea6de',
      400: '#4f80cc',
      500: '#2c62b4',
      600: '#1f4d96',
      700: '#1a3f7a',
      800: '#163363',
      900: '#12284d',
      950: '#0b1a33',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
          950: '#020617',
        },
      },
    },
  },
});
