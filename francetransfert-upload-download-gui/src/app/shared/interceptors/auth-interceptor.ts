import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from './../../../environments/environment';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { LoginService } from 'src/app/services/login/login.service';
import { OAuthService } from 'angular-oauth2-oidc';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    constructor(private oauthService: OAuthService) { }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {

        if (this.oauthService.hasValidAccessToken() && this.oauthService.getAccessToken()) {

            const token = this.oauthService.getAccessToken();

            const modifiedReq = req.clone({
                headers: req.headers.set('Authorization', `Bearer ${token}`),
            });
            return next.handle(modifiedReq).pipe(
                catchError(
                    (err, caught) => {
                        if (err.status === 401) {
                            this.handleAuthError();
                            return of(err);
                        }
                        throw err;
                    }
                )
            );
        } else if (this.oauthService.hasValidAccessToken() && !this.oauthService.getAccessToken()) {
            this.oauthService.logOut(false);
        }
        return next.handle(req);
    }

    private handleAuthError() {
        this.oauthService.logOut(false);
        //localStorage.removeItem('auth');
        //window.location.reload();
    }
}

