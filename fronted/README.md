# Online Judge Frontend

This is the frontend for the Online Judge system, built with Astro, Vue 3, Tailwind CSS, and Element Plus.

## Project Structure

- `src/pages`: Astro routes (SSR enabled).
- `src/components`: Vue 3 components.
- `src/stores`: Pinia state management.
- `src/utils`: Utilities like Axios instance.
- `src/layouts`: Main layout.

## Setup

1.  Install dependencies:
    ```bash
    npm install
    ```

2.  Run development server:
    ```bash
    npm run dev
    ```
    The server will start at `http://localhost:4321`.
    API requests are proxied to `http://localhost:8080`.

3.  Build for production:
    ```bash
    npm run build
    ```
    The output will be in `dist/`. You can run it with `node dist/server/entry.mjs`.

## Features

-   **Authentication**: Login and Register with automatic redirection on 401 errors.
-   **Problems**: List and Detail views with Markdown rendering and code submission.
-   **Submissions**: List and Detail views with status tags and code display.
-   **UI**: Modern design using Tailwind CSS and Element Plus.

Astro looks for `.astro` or `.md` files in the `src/pages/` directory. Each page is exposed as a route based on its file name.

There's nothing special about `src/components/`, but that's where we like to put any Astro/React/Vue/Svelte/Preact components.

Any static assets, like images, can be placed in the `public/` directory.

## 🧞 Commands

All commands are run from the root of the project, from a terminal:

| Command                   | Action                                           |
| :------------------------ | :----------------------------------------------- |
| `npm install`             | Installs dependencies                            |
| `npm run dev`             | Starts local dev server at `localhost:4321`      |
| `npm run build`           | Build your production site to `./dist/`          |
| `npm run preview`         | Preview your build locally, before deploying     |
| `npm run astro ...`       | Run CLI commands like `astro add`, `astro check` |
| `npm run astro -- --help` | Get help using the Astro CLI                     |

## 👀 Want to learn more?

Feel free to check [our documentation](https://docs.astro.build) or jump into our [Discord server](https://astro.build/chat).
