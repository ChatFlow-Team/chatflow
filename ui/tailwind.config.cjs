module.exports = {
  content: ['./src/main/resources/web/*.{html,js}'],
  theme: {
    extend: {
      colors: {
        ink: '#191b32',
        muted: '#535c73',
        canvas: '#f6f7fb',
        line: '#d5d9e4',
        action: '#6042ce',
        waiting: '#e9e3f7',
        lavender: '#eee9ff',
        danger: '#b42318',
        success: '#16724a',
      },
      fontFamily: { sans: ['Inter', 'Arial', 'sans-serif'] },
    },
  },
};
