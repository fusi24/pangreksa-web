import path from 'path';
import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
    resolve: {
        alias: {
            // The @tailwindcss/vite plugin can't resolve bare relative @import inside
            // generated jar-resource CSS. Provide an explicit alias so Vite resolves it.
            'full-calendar-theme-vaadin.css': path.resolve(
                __dirname,
                'src/main/frontend/generated/jar-resources/vaadin-full-calendar/full-calendar-theme-vaadin.css'
            ),
        },
    },
});

export default overrideVaadinConfig(customConfig);
