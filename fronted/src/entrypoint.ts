import type { App } from 'vue';
import { ID_INJECTION_KEY } from 'element-plus';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

export default (app: App) => {
  app.use(createPinia());
  app.use(ElementPlus);
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }
  app.provide(ID_INJECTION_KEY, {
    prefix: 1024,
    current: 0,
  });
};
