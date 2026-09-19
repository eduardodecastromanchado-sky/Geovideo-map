import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpRequest, HttpHandlerFn } from '@angular/common/http';

import { routes } from './app.routes';

/** Attach withCredentials to every request so session cookies are sent. */
function credentialsInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn) {
  return next(req.clone({ withCredentials: true }));
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([credentialsInterceptor]))
  ]
};
