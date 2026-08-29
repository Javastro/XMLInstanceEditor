import adapter from '@sveltejs/adapter-static';

const config = {
  kit: {
    adapter: adapter({
      fallback: 'index.html'
    }),
    files: {
      routes: 'src/routes'
    }
  }
};

export default config;
