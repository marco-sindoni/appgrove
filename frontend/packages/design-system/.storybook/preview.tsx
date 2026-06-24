import { useEffect } from 'react'
import type { Decorator, Preview } from '@storybook/react-vite'
import { applyTheme, type Accent, type Theme } from '../src/theme/theme'
import './storybook.css'

const withTheme: Decorator = (Story, context) => {
  const theme = context.globals.theme as Theme
  const accent = context.globals.accent as Accent
  useEffect(() => {
    applyTheme({ theme, accent })
  }, [theme, accent])
  return <Story />
}

const preview: Preview = {
  decorators: [withTheme],
  initialGlobals: { theme: 'light', accent: 'coral' },
  globalTypes: {
    theme: {
      description: 'Tema',
      toolbar: {
        title: 'Tema',
        icon: 'circlehollow',
        items: [
          { value: 'light', title: 'Light' },
          { value: 'dark', title: 'Dark' },
        ],
        dynamicTitle: true,
      },
    },
    accent: {
      description: 'Accent',
      toolbar: {
        title: 'Accent',
        icon: 'paintbrush',
        items: [
          { value: 'coral', title: 'Coral' },
          { value: 'violet', title: 'Violet' },
          { value: 'teal', title: 'Teal' },
          { value: 'blue', title: 'Blue' },
        ],
        dynamicTitle: true,
      },
    },
  },
  parameters: {
    layout: 'centered',
    controls: { matchers: { color: /(background|color)$/i, date: /Date$/i } },
  },
}

export default preview
